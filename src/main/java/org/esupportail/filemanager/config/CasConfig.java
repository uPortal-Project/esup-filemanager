/**
 * Licensed to EsupPortail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * EsupPortail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.filemanager.config;

import jakarta.servlet.http.HttpSessionBindingEvent;
import org.apereo.cas.client.session.SingleSignOutFilter;
import org.apereo.cas.client.session.SingleSignOutHttpSessionListener;
import org.apereo.cas.client.validation.Cas20ServiceTicketValidator;
import org.apereo.cas.client.validation.TicketValidator;

import org.esupportail.filemanager.services.auth.CasUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix="cas")
@EnableConfigurationProperties(AuthenticationRateLimitProperties.class)
@EnableWebSecurity
public class CasConfig {

    private final String cspExtraConnectSrc;

    private final String cspExtraFrameSrc;

    private final String cspReportUri;

    private final boolean cspReportOnly;

    public CasConfig(@Value("${security.csp.extra-connect-src:}") String cspExtraConnectSrc,
                     @Value("${security.csp.extra-frame-src:}") String cspExtraFrameSrc,
                     @Value("${security.csp.report-uri:}") String cspReportUri,
                     @Value("${security.csp.report-only:true}") boolean cspReportOnly) {
        this.cspExtraConnectSrc = cspExtraConnectSrc;
        this.cspExtraFrameSrc = cspExtraFrameSrc;
        this.cspReportUri = cspReportUri;
        this.cspReportOnly = cspReportOnly;
    }

    String url;

    String service;

    String key;

    public String getUrl() {
        return url;
    }

    public String getService() {
        return service;
    }

    public String getKey() {
        return key;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Bean
    public ServiceProperties serviceProperties() {
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setService(service + "/login/cas");
        serviceProperties.setSendRenew(false);
        return serviceProperties;
    }

    @Bean
    @Primary
    public AuthenticationEntryPoint authenticationEntryPoint(ServiceProperties sP) {
        CasAuthenticationEntryPoint entryPoint = new CasAuthenticationEntryPoint();
        entryPoint.setLoginUrl(url + "/login");
        entryPoint.setServiceProperties(sP);
        return entryPoint;
    }

    @Bean
    public TicketValidator ticketValidator() {
        ;return new Cas20ServiceTicketValidator(url);
    }

    @Bean
    public CasAuthenticationProvider casAuthenticationProvider(ServiceProperties serviceProperties, TicketValidator ticketValidator) {
        CasAuthenticationProvider provider = new CasAuthenticationProvider();
        provider.setServiceProperties(serviceProperties);
        provider.setTicketValidator(ticketValidator);
        provider.setAuthenticationUserDetailsService(new CasUserDetailsService());
        provider.setKey(key);
        return provider;
    }

    @Bean
    public CasAuthenticationEntryPoint casAuthenticationEntryPoint() {
        CasAuthenticationEntryPoint ep = new CasAuthenticationEntryPoint();
        ep.setLoginUrl(url + "/login");
        ep.setServiceProperties(serviceProperties());
        return ep;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CasAuthenticationFilter casAuthenticationFilter,
                                           SecondaryAuthenticationRateLimitFilter secondaryAuthenticationRateLimitFilter) throws Exception {

        CookieCsrfTokenRepository cookieCsrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        cookieCsrfTokenRepository.setCookieName("XSRF-TOKEN");

        http
                .exceptionHandling()
                .authenticationEntryPoint(casAuthenticationEntryPoint())
                .and()
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl(url + "/logout?service=" + service)
                )
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> {
                            csp.policyDirectives(buildContentSecurityPolicy());
                            if (cspReportOnly) {
                                csp.reportOnly();
                            }
                        })
                )
                .addFilter(casAuthenticationFilter)
                .addFilterAfter(secondaryAuthenticationRateLimitFilter, CasAuthenticationFilter.class)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(cookieCsrfTokenRepository)
                        .ignoringRequestMatchers("/csp-report")
                );
        return http.build();
    }

    private String buildContentSecurityPolicy() {
        String connectSrc = sources("'self'", cspExtraConnectSrc);
        String frameSrc = sources("'self'", cspExtraFrameSrc);

        List<String> directives = new ArrayList<>(List.of(
                "default-src 'self'",
                "base-uri 'self'",
                "object-src 'none'",
                "script-src 'self'",
                "style-src 'self'",
                "img-src 'self' data: blob:",
                "font-src 'self'",
                "media-src 'self' blob:",
                "connect-src " + connectSrc,
                "frame-src " + frameSrc,
                "form-action 'self'",
                "frame-ancestors 'self'"
        ));

        String reportUri = sanitizeCspSource(cspReportUri);
        if (!reportUri.isBlank()) {
            directives.add("report-uri " + reportUri);
        }

        return String.join("; ", directives);
    }

    private String sources(String defaultSource, String extraSources) {
        Set<String> sources = new LinkedHashSet<>();
        sources.add(defaultSource);
        sources.addAll(Arrays.stream(extraSources.split("[,\\s]+"))
                .map(this::sanitizeCspSource)
                .filter(source -> !source.isBlank())
                .collect(Collectors.toList()));
        return String.join(" ", sources);
    }

    private String sanitizeCspSource(String source) {
        if (source == null) {
            return "";
        }

        String trimmed = source.trim();
        if (trimmed.isEmpty() || trimmed.contains(";") || trimmed.contains("\r") || trimmed.contains("\n")) {
            return "";
        }

        return trimmed;
    }

    @Bean
    public SecondaryAuthenticationRateLimitFilter secondaryAuthenticationRateLimitFilter(
            AuthenticationRateLimitProperties properties,
            MessageSource messageSource,
            ObjectMapper objectMapper) {
        return new SecondaryAuthenticationRateLimitFilter(properties, messageSource, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<SecondaryAuthenticationRateLimitFilter> secondaryAuthenticationRateLimitFilterRegistration(
            SecondaryAuthenticationRateLimitFilter filter) {
        FilterRegistrationBean<SecondaryAuthenticationRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CasAuthenticationFilter casAuthenticationFilter(AuthenticationManager authenticationManager) throws Exception {
        CasAuthenticationFilter filter = new CasAuthenticationFilter();
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }

    @Bean
    public AuthenticationManager authenticationManager(CasAuthenticationProvider casAuthenticationProvider) {
        return new ProviderManager(List.of(casAuthenticationProvider));
    }


    @Bean
    public SecurityContextLogoutHandler securityContextLogoutHandler() {
        return new SecurityContextLogoutHandler();
    }

    @Bean
    public LogoutFilter logoutFilter(SecurityContextLogoutHandler securityContextLogoutHandler) {
        LogoutFilter logoutFilter = new LogoutFilter(
                url + "/logout?service=" + service, securityContextLogoutHandler);
        logoutFilter.setFilterProcessesUrl("/logout");
        return logoutFilter;
    }

    @Bean
    public SingleSignOutFilter singleSignOutFilter() {
        SingleSignOutFilter singleSignOutFilter = new SingleSignOutFilter();
        singleSignOutFilter.setIgnoreInitConfiguration(true);
        return singleSignOutFilter;
    }

    @EventListener
    public SingleSignOutHttpSessionListener singleSignOutHttpSessionListener(HttpSessionBindingEvent event) {
        return new SingleSignOutHttpSessionListener();
    }
}
