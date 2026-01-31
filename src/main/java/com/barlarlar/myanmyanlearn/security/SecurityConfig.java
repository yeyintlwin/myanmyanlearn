package com.barlarlar.myanmyanlearn.security;

import com.barlarlar.myanmyanlearn.entity.Role;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import com.barlarlar.myanmyanlearn.repository.RoleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.http.HttpStatus;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

        // add support for JDBC ... no more hardcoded users :-)

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public UserDetailsService userDetailsService(MemberRepository memberRepository, RoleRepository roleRepository) {
                return username -> {
                        var member = memberRepository.findById(username)
                                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                        boolean emailVerified = member.getEmailVerified() != null && member.getEmailVerified();
                        if (!emailVerified) {
                                throw new UsernameNotFoundException("User not found");
                        }

                        boolean enabled = member.getActive() != null && member.getActive();

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

                http.authorizeHttpRequests(configurer -> configurer
                                .requestMatchers("/", "/login", "/register", "/register-test",
                                                "/email-verification",
                                                "/resend-otp",
                                                "/forget-password", "/reset-password", "/reset-link-sent",
                                                "/reset-link-expired",
                                                "/reset-success",
                                                "/verification-success",
                                                "/splash", "/language", "/intro",
                                                "/error",
                                                "/images/**", "/css/**", "/js/**",
                                                "/webjars/**")
                                .permitAll()
                                .requestMatchers("/admin", "/admin/**").hasAnyRole("ADMIN", "TEACHER")
                                .requestMatchers("/markdown-editor", "/markdown-editor/**")
                                .hasAnyRole("ADMIN", "TEACHER")
                                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "TEACHER")
                                .requestMatchers("/assessment", "/assessment/**").authenticated()
                                .anyRequest().authenticated())
                                .formLogin(form -> form.loginPage("/login")
                                                .loginProcessingUrl("/authenticateTheUser")
                                                .successHandler(customAuthenticationSuccessHandler)
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
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout=true")
                                                .deleteCookies("remember-me", "JSESSIONID")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .permitAll())
                                .rememberMe(remember -> remember
                                                .key("myanmyanlearn-remember-me-key")
                                                .tokenValiditySeconds(86400 * 30) // 30 days
                                                .userDetailsService(userDetailsService)
                                                .rememberMeParameter("remember-me")
                                                .alwaysRemember(false))
                                .sessionManagement(session -> session
                                                .maximumSessions(1)
                                                .maxSessionsPreventsLogin(false))
                                .exceptionHandling(ex -> ex
                                                .defaultAuthenticationEntryPointFor(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                                                new AntPathRequestMatcher("/api/**"))
                                                .defaultAccessDeniedHandlerFor(
                                                                (request, response, accessDeniedException) -> response
                                                                                .sendError(
                                                                                                HttpStatus.FORBIDDEN
                                                                                                                .value()),
                                                                new AntPathRequestMatcher("/api/**")))
                                .headers(headers -> headers
                                                .cacheControl(Customizer.withDefaults()));

                return http.build();
        }
}
