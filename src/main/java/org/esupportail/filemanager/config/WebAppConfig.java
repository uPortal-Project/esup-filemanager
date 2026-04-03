package org.esupportail.filemanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;

@Configuration
@EnableMethodSecurity
public class WebAppConfig implements WebMvcConfigurer {

    /**
     * Resolves the locale from a cookie (persisted for 1 year).
     * When no cookie is present, uses the browser locale (Accept-Language).
     * The Java ResourceBundle fallback hierarchy ensures that fr_CA → messages_fr.properties,
     * en_US → messages.properties (English default), de_AT → messages_de.properties, etc.
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("esup-fm-lang");
        resolver.setCookieMaxAge(Duration.ofDays(365));
        // No default locale → automatically uses the browser's Accept-Language header
        // when the cookie is absent
        return resolver;
    }

    /**
     * Interceptor that changes the locale via the URL parameter ?lang=fr|en|de|es
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
