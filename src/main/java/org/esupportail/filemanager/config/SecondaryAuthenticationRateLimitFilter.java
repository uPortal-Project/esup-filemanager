package org.esupportail.filemanager.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class SecondaryAuthenticationRateLimitFilter extends OncePerRequestFilter {

    private final AuthenticationRateLimitProperties properties;

    private final MessageSource messageSource;

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    public SecondaryAuthenticationRateLimitFilter(AuthenticationRateLimitProperties properties,
                                                  MessageSource messageSource,
                                                  ObjectMapper objectMapper) {
        this.properties = properties;
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled()
                || !"POST".equalsIgnoreCase(request.getMethod())
                || !"/authenticate".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = rateLimitKey(request);
        BucketEntry entry = bucketFor(key);
        entry.touch();

        ConsumptionProbe probe = entry.bucket().tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> jsonMsg = new HashMap<>();
        jsonMsg.put("status", 0);
        jsonMsg.put("msg", messageSource.getMessage("auth.tooManyRequests", null, LocaleContextHolder.getLocale()));
        objectMapper.writeValue(response.getWriter(), jsonMsg);

    }

    private BucketEntry bucketFor(String key) {
        BucketEntry entry = buckets.computeIfAbsent(key, ignored -> new BucketEntry(createBucket()));
        trimBucketsIfNeeded();
        return entry;
    }

    private Bucket createBucket() {
        Duration refillPeriod = properties.getRefillPeriod();
        Bandwidth limit = Bandwidth.builder()
                .capacity(properties.getCapacity())
                .refillGreedy(properties.getRefillTokens(), refillPeriod)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String rateLimitKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && StringUtils.hasText(authentication.getName())) {
            return "user:" + authentication.getName();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void trimBucketsIfNeeded() {
        int maxBuckets = properties.getMaxBuckets();
        if (maxBuckets <= 0 || buckets.size() <= maxBuckets) {
            return;
        }

        buckets.entrySet().stream()
                .min(Comparator.comparingLong(entry -> entry.getValue().lastAccessNanos()))
                .ifPresent(entry -> buckets.remove(entry.getKey(), entry.getValue()));
    }

    private static class BucketEntry {

        private static final AtomicLongFieldUpdater<BucketEntry> LAST_ACCESS_UPDATER =
                AtomicLongFieldUpdater.newUpdater(BucketEntry.class, "lastAccessNanos");

        private final Bucket bucket;

        private volatile long lastAccessNanos;

        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessNanos = System.nanoTime();
        }

        Bucket bucket() {
            return bucket;
        }

        long lastAccessNanos() {
            return lastAccessNanos;
        }

        private void touch() {
            LAST_ACCESS_UPDATER.set(this, System.nanoTime());
        }
    }
}
