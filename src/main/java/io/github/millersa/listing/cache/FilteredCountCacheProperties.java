package io.github.millersa.listing.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Конфигурация {@link FilteredCountCache}, привязана к префиксу {@code listing.count-cache} в application.yml.
 *
 * <pre>{@code
 * listing:
 *   count-cache:
 *     enabled: true
 *     ttl: 60s
 *     max-size: 2000
 * }</pre>
 */
@ConfigurationProperties(prefix = "listing.count-cache")
public record FilteredCountCacheProperties(
        boolean enabled,
        Duration ttl,
        long maxSize
) {
    public FilteredCountCacheProperties {
        if (ttl == null) ttl = Duration.ofSeconds(60);
        if (maxSize <= 0) maxSize = 2000;
    }
}
