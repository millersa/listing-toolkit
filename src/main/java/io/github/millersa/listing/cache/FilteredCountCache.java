package io.github.millersa.listing.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Кеш для значений {@code totalCount} по комбинациям фильтров.
 * <p>
 * Использовать там, где count-запрос Spring Data делает с теми же joins, что и data-запрос,
 * и при этом одинаковые фильтры приходят повторно (типично — пользователь листает страницы).
 * </p>
 *
 * <p><b>Согласованность:</b> данные могут быть устаревшими на TTL. Подходит для UI-пагинации,
 * не подходит для бизнес-логики, где точный count критичен.</p>
 *
 * <p><b>Ключ:</b> {@link #key(String, Object)} использует Jackson-сериализацию фильтра.
 * Если ObjectMapper не передан в конструктор, метод бросит {@link IllegalStateException}.
 * Это сделано умышленно: дефолтный {@code Object.hashCode()} основан на identity и каждый новый DTO
 * с одинаковыми полями давал бы разный ключ, делая кеш бесполезным.</p>
 */
public class FilteredCountCache {

    private static final String NAMESPACE_SEPARATOR = ":";

    private final Cache<String, Long> cache;
    private final ObjectMapper objectMapper;

    public FilteredCountCache(Duration ttl, long maxSize, ObjectMapper objectMapper) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .recordStats()
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Возвращает значение из кеша либо считает через supplier и кладёт в кеш.
     */
    public long getOrCompute(String key, LongSupplier compute) {
        Long cached = cache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        long count = compute.getAsLong();
        cache.put(key, count);
        return count;
    }

    /**
     * Строит стабильный ключ кеша из произвольного фильтра через JSON-сериализацию.
     *
     * @throws IllegalStateException если {@link ObjectMapper} не был передан в конструктор
     * @throws IllegalArgumentException если фильтр не сериализуется в JSON
     */
    public String key(String namespace, Object filter) {
        if (filter == null) {
            return namespace + NAMESPACE_SEPARATOR + "null";
        }
        if (objectMapper == null) {
            throw new IllegalStateException(
                    "FilteredCountCache requires Jackson ObjectMapper for stable key building. " +
                            "Add com.fasterxml.jackson.core:jackson-databind to your classpath, " +
                            "or build the key string yourself before calling getOrCompute(...).");
        }
        try {
            return namespace + NAMESPACE_SEPARATOR + objectMapper.writeValueAsString(filter);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize filter to cache key", e);
        }
    }

    public void invalidate(String key) {
        cache.invalidate(key);
    }

    /**
     * Инвалидирует все ключи, начинающиеся с {@code namespace + ':'}.
     * Дороже {@link #invalidate(String)}, потому что перебирает все ключи в кеше.
     */
    public void invalidateByNamespace(String namespace) {
        String prefix = namespace + NAMESPACE_SEPARATOR;
        cache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * Статистика для метрик/мониторинга.
     */
    public CacheStats stats() {
        var s = cache.stats();
        return new CacheStats(s.hitCount(), s.missCount(), s.evictionCount(), cache.estimatedSize());
    }

    public record CacheStats(long hits, long misses, long evictions, long size) {}
}
