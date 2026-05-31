package io.github.millersa.listing.autoconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.millersa.listing.cache.FilteredCountCache;
import io.github.millersa.listing.cache.FilteredCountCacheProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot Auto-Configuration для listing-toolkit.
 *
 * <p>Включает {@link FilteredCountCache} как Spring-бин при выставленном
 * {@code listing.count-cache.enabled=true} (по умолчанию выключено).</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(FilteredCountCacheProperties.class)
@ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Caffeine")
public class ListingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "listing.count-cache", name = "enabled", havingValue = "true")
    public FilteredCountCache filteredCountCache(FilteredCountCacheProperties props,
                                                 ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new FilteredCountCache(
                props.ttl(),
                props.maxSize(),
                objectMapperProvider.getIfAvailable()
        );
    }
}
