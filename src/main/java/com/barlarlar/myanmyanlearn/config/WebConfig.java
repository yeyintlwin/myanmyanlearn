package com.barlarlar.myanmyanlearn.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Serve course assets directly from classpath:/courses/**
        registry.addResourceHandler("/courses/**")
                .addResourceLocations("classpath:/courses/");
        String home = System.getProperty("user.home");
        String base = (home != null ? home : "") + "/.myanmyanlearn/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + base);
        // Also ensure standard static resources continue to work (usually auto-configured)
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        return ms;
    }

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver clr = new CookieLocaleResolver("selectedLanguage");
        clr.setDefaultLocale(Locale.ENGLISH);
        // 365 days
        clr.setCookieMaxAge(Objects.requireNonNull(Duration.ofSeconds(31536000)));
        return clr;
    }
}
