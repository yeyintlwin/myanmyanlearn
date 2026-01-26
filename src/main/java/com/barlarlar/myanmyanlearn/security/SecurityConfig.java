package com.barlarlar.myanmyanlearn.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

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
        public UserDetailsManager userDetailsManager(DataSource dataSource) {

                JdbcUserDetailsManager jdbcUserDetailsManager = new JdbcUserDetailsManager(dataSource);

                // define query to retrieve a user by username
                jdbcUserDetailsManager.setUsersByUsernameQuery(
                                "select user_id, pw, active from members where user_id=? and email_verified=1");

                // define query to retrieve the authorities/roles by username
                jdbcUserDetailsManager.setAuthoritiesByUsernameQuery(
                                "select user_id, role from roles where user_id=?");

                return jdbcUserDetailsManager;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, DataSource dataSource) throws Exception {

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
                                .requestMatchers("/assessment", "/assessment/**").authenticated()
                                .anyRequest().authenticated())
                                .formLogin(form -> form.loginPage("/login")
                                                .loginProcessingUrl("/authenticateTheUser")
                                                .successHandler(customAuthenticationSuccessHandler)
                                                .failureUrl("/login?error=true")
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
                                                .userDetailsService(userDetailsManager(dataSource))
                                                .rememberMeParameter("remember-me")
                                                .alwaysRemember(false))
                                .sessionManagement(session -> session
                                                .maximumSessions(1)
                                                .maxSessionsPreventsLogin(false))
                                .headers(headers -> headers
                                                .cacheControl(Customizer.withDefaults()));

                return http.build();
        }
}
