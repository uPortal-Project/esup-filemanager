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
     * Résout la locale depuis un cookie (persistant 1 an).
     * Quand aucun cookie n'est présent, utilise la locale du navigateur (Accept-Language).
     * La hiérarchie de fallback Java ResourceBundle assure que fr_CA → messages_fr.properties,
     * en_US → messages.properties (défaut anglais), de_AT → messages_de.properties, etc.
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("esup-fm-lang");
        resolver.setCookieMaxAge(Duration.ofDays(365));
        // Pas de locale par défaut → utilise automatiquement l'en-tête Accept-Language du navigateur
        // quand le cookie est absent
        return resolver;
    }

    /**
     * Intercepteur qui change la locale via le paramètre URL ?lang=fr|en|de|es
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
