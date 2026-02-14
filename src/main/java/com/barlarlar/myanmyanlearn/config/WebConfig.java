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
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String storageType;
    private final String localBaseDir;

    public WebConfig(
            @Value("${app.storage.type:local}") String storageType,
            @Value("${app.storage.local.base-dir:${user.dir}/.myanmyanlearn/uploads}") String localBaseDir) {
        this.storageType = storageType != null ? storageType.trim() : "local";
        this.localBaseDir = localBaseDir != null ? localBaseDir.trim() : "";
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        if (!"local".equalsIgnoreCase(storageType)) {
            return;
        }
        String base = localBaseDir.isBlank() ? (System.getProperty("user.dir", ".") + "/.myanmyanlearn/uploads")
                : localBaseDir;
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + base);
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
        CookieLocaleResolver clr = new CookieLocaleResolver("selectedLanguage") {
            @Override
            protected Locale parseLocaleValue(@NonNull String localeValue) {
                if (localeValue != null && localeValue.trim().equalsIgnoreCase("bd")) {
                    localeValue = "bn";
                }
                return super.parseLocaleValue(localeValue);
            }
        };
        clr.setDefaultLocale(Locale.ENGLISH);
        // 365 days
        clr.setCookieMaxAge(Objects.requireNonNull(Duration.ofSeconds(31536000)));
        return clr;
    }

}
