package com.barlarlar.myanmyanlearn.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Autowired;

import com.barlarlar.myanmyanlearn.security.CustomAuthenticationSuccessHandler;

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
                                "select user_id, pw, active from members where user_id=?");

                // define query to retrieve the authorities/roles by username
                jdbcUserDetailsManager.setAuthoritiesByUsernameQuery(
                                "select user_id, role from roles where user_id=?");

                return jdbcUserDetailsManager;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, DataSource dataSource) throws Exception {

                http.authorizeHttpRequests(configurer -> configurer
                                .requestMatchers("/showMyLoginPage", "/login", "/register", "/register-test",
                                                "/email-verification",
                                                "/resend-otp",
                                                "/forget-password", "/reset-password", "/reset-success",
                                                "/verification-success",
                                                "/test-email", "/test-otp", "/images/**", "/css/**", "/js/**",
                                                "/webjars/**")
                                .permitAll()
                                .anyRequest().authenticated())
                                .formLogin(form -> form.loginPage("/showMyLoginPage")
                                                .loginProcessingUrl("/authenticateTheUser")
                                                .successHandler(customAuthenticationSuccessHandler)
                                                .failureUrl("/showMyLoginPage?error=true")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/showMyLoginPage?logout=true")
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
                                                .maxSessionsPreventsLogin(false));

                return http.build();
        }
}
