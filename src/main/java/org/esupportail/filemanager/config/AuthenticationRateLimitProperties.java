package org.esupportail.filemanager.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.secondary-auth.rate-limit")
public class AuthenticationRateLimitProperties {

    private boolean enabled = true;

    private long capacity = 5;

    private long refillTokens = 5;

    private Duration refillPeriod = Duration.ofMinutes(1);

    private int maxBuckets = 10000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(long refillTokens) {
        this.refillTokens = refillTokens;
    }

    public Duration getRefillPeriod() {
        return refillPeriod;
    }

    public void setRefillPeriod(Duration refillPeriod) {
        this.refillPeriod = refillPeriod;
    }

    public int getMaxBuckets() {
        return maxBuckets;
    }

    public void setMaxBuckets(int maxBuckets) {
        this.maxBuckets = maxBuckets;
    }
}
