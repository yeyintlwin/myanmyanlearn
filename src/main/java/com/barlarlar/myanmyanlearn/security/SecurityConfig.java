package com.barlarlar.myanmyanlearn.security;

import com.barlarlar.myanmyanlearn.entity.Role;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import com.barlarlar.myanmyanlearn.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.http.HttpStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

        // DB ထဲက members/roles table ကိုသုံးပြီး login + role-based access control
        // (RBAC) ကို ချိတ်ဆက်ထားတဲ့ config ဖြစ်ပါတယ်။

        @Bean
        public PasswordEncoder passwordEncoder() {
                // Password hash အတွက် BCrypt ကို သုံးထားပါတယ် (DB ထဲမှာ hash သိုလှောင်ရန်)
                return new BCryptPasswordEncoder();
        }

        @Bean
        public UserDetailsService userDetailsService(MemberRepository memberRepository, RoleRepository roleRepository) {
                return username -> {
                        // Login အတွက် user record ကို DB ထဲကနေ ဆွဲပြီး စစ်ဆေးပါတယ်
                        var member = memberRepository.findById(username)
                                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                        // Email verify မပြီးရင် login မခွင့် (user မရှိသလိုပဲ ပြန်ပို့)
                        boolean emailVerified = member.getEmailVerified() != null && member.getEmailVerified();
                        if (!emailVerified) {
                                throw new UsernameNotFoundException("User not found");
                        }

                        // active=false ဖြစ်ရင် account disabled လုပ်ပါတယ်
                        boolean enabled = member.getActive() != null && member.getActive();

                        // roles table က role strings တွေကို Spring Security GrantedAuthority အဖြစ်
                        // ပြောင်းထည့်ပါတယ်
                        // ဥပမာ: ROLE_ADMIN, ROLE_TEACHER, ROLE_STUDENT
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        List<Role> roles = roleRepository.findByUserId(username);
                        if (roles != null) {
                                for (Role r : roles) {
                                        if (r != null && r.getRole() != null && !r.getRole().isBlank()) {
                                                authorities.add(new SimpleGrantedAuthority(r.getRole().trim()));
                                        }
                                }
                        }

                        return User.builder()
                                        .username(member.getUserId())
                                        .password(member.getPassword())
                                        .disabled(!enabled)
                                        .authorities(authorities)
                                        .build();
                };
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService)
                        throws Exception {

                // API route (/api/**) ကို web page route တွေနဲ့ ခွဲပြီး
                // unauthenticated/access denied ဖြစ်ရင် HTML redirect မလုပ်ဘဲ status code ဖြင့်
                // ပြန်ပေးရန် သတ်မှတ်ပါတယ်
                RequestMatcher apiRequestMatcher = request -> {
                        String uri = request.getRequestURI();
                        if (uri == null) {
                                return false;
                        }
                        String contextPath = request.getContextPath();
                        String prefix = (contextPath == null ? "" : contextPath) + "/api/";
                        return uri.startsWith(prefix);
                };

                http.cors(Customizer.withDefaults())
                                .authorizeHttpRequests(configurer -> configurer
                                                // Public routes (login/register/static assets) တွေကို login မလိုဘဲ
                                                // ဝင်ခွင့်ပေး
                                                .requestMatchers("/", "/login", "/register", "/register-test",
                                                                "/email-verification",
                                                                "/resend-otp",
                                                                "/forget-password", "/reset-password",
                                                                "/reset-link-sent",
                                                                "/reset-link-expired",
                                                                "/reset-success",
                                                                "/verification-success",
                                                                "/splash", "/language", "/intro",
                                                                "/error",
                                                                "/images/**", "/css/**", "/js/**",
                                                                "/webjars/**")
                                                .permitAll()
                                                // Browser preflight (CORS OPTIONS) ကို allow မလုပ်ရင် fetch က client
                                                // ဘက်မှာ abort ဖြစ်တတ်ပါတယ်
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                // Admin panel ကို ADMIN/TEACHER ပဲ ဝင်ခွင့်ပေး
                                                .requestMatchers("/admin", "/admin/**").hasAnyRole("ADMIN", "TEACHER")
                                                // Markdown editor ကို ADMIN/TEACHER ပဲ ဝင်ခွင့်ပေး
                                                .requestMatchers("/markdown-editor", "/markdown-editor/**")
                                                .hasAnyRole("ADMIN", "TEACHER")
                                                // Admin API (/api/admin/**) ကို ADMIN/TEACHER ပဲ ခွင့်ပေး
                                                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "TEACHER")
                                                // Assessment ကို login ဝင်ထားရုံနဲ့ သုံးခွင့်ပေး (role မကန့်သတ်)
                                                .requestMatchers("/assessment", "/assessment/**").authenticated()
                                                // အခြား route အားလုံးက login လိုအပ်
                                                .anyRequest().authenticated())
                                .formLogin(form -> form.loginPage("/login")
                                                .loginProcessingUrl("/authenticateTheUser")
                                                .successHandler(customAuthenticationSuccessHandler)
                                                // Login failure အမျိုးအစားအလိုက် query param ထည့်ပြီး login page ကို
                                                // redirect ပြန်ပို့
                                                .failureHandler((request, response, exception) -> {
                                                        String target = "/login?error=true";
                                                        if (exception instanceof DisabledException) {
                                                                target = "/login?disabled=true";
                                                        } else if (exception instanceof LockedException) {
                                                                target = "/login?locked=true";
                                                        } else if (exception instanceof AccountExpiredException
                                                                        || exception instanceof CredentialsExpiredException) {
                                                                target = "/login?expired=true";
                                                        }
                                                        response.sendRedirect(target);
                                                })
                                                .permitAll())
                                .logout(logout -> logout
                                                // Logout ပြီးရင် session/cookies ကိုရှင်းပြီး login page ကို ပြန်ပို့
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout=true")
                                                .deleteCookies("remember-me", "JSESSIONID")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .permitAll())
                                .rememberMe(remember -> remember
                                                // Remember-me cookie ကို အသုံးပြုချင်ရင် ဒီ config ကနေ control
                                                // လုပ်ပါတယ်
                                                .key("myanmyanlearn-remember-me-key")
                                                .tokenValiditySeconds(86400 * 30) // 30 days
                                                .userDetailsService(userDetailsService)
                                                .rememberMeParameter("remember-me")
                                                .alwaysRemember(false))
                                .sessionManagement(session -> session
                                                // user တစ်ယောက်ကို session တစ်ခုသာ ခွင့် (multi-login control)
                                                .maximumSessions(1)
                                                .maxSessionsPreventsLogin(false))
                                .exceptionHandling(ex -> ex
                                                // API request ဆိုရင် unauthenticated အတွက် 401 ပြန်
                                                .defaultAuthenticationEntryPointFor(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                                                apiRequestMatcher)
                                                // API request ဆိုရင် access denied အတွက် 403 ပြန်
                                                .defaultAccessDeniedHandlerFor(
                                                                (request, response, accessDeniedException) -> response
                                                                                .sendError(
                                                                                                HttpStatus.FORBIDDEN
                                                                                                                .value()),
                                                                apiRequestMatcher))
                                .headers(headers -> headers
                                                // Sensitive page များကို cache မဖြစ်အောင် default cache-control headers
                                                // ထည့်
                                                .cacheControl(Customizer.withDefaults()));

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                // Trae preview / iframe / localhost port မတူတဲ့ origin တွေကနေ
                // admin JS fetch (/api/**) လုပ်တဲ့အခါ browser က abort မဖြစ်အောင် CORS allow
                // လုပ်ထားပါတယ်
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowCredentials(true);
                config.setAllowedOriginPatterns(Arrays.asList("*"));
                config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(Arrays.asList("*"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", config);
                return source;
        }
}
