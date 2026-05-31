package io.github.millersa.listing.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilteredCountCacheTest {

    record FilterDto(String name, int age) {}

    private static FilteredCountCache newCache() {
        return new FilteredCountCache(Duration.ofMinutes(1), 1000, new ObjectMapper());
    }

    @Test
    void getOrCompute_firstCall_computes() {
        FilteredCountCache cache = newCache();
        AtomicInteger counter = new AtomicInteger();
        long result = cache.getOrCompute("k", () -> {
            counter.incrementAndGet();
            return 42L;
        });
        assertThat(result).isEqualTo(42L);
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void getOrCompute_secondCall_returnsCached() {
        FilteredCountCache cache = newCache();
        AtomicInteger counter = new AtomicInteger();
        cache.getOrCompute("k", () -> { counter.incrementAndGet(); return 42L; });
        cache.getOrCompute("k", () -> { counter.incrementAndGet(); return 99L; });
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void getOrCompute_differentKeys_computesEach() {
        FilteredCountCache cache = newCache();
        AtomicInteger counter = new AtomicInteger();
        cache.getOrCompute("k1", () -> { counter.incrementAndGet(); return 1L; });
        cache.getOrCompute("k2", () -> { counter.incrementAndGet(); return 2L; });
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void key_sameFilter_sameKey() {
        FilteredCountCache cache = newCache();
        String k1 = cache.key("test", new FilterDto("alice", 30));
        String k2 = cache.key("test", new FilterDto("alice", 30));
        assertThat(k1).isEqualTo(k2);
    }

    @Test
    void key_differentFilter_differentKey() {
        FilteredCountCache cache = newCache();
        String k1 = cache.key("test", new FilterDto("alice", 30));
        String k2 = cache.key("test", new FilterDto("bob", 30));
        assertThat(k1).isNotEqualTo(k2);
    }

    @Test
    void key_nullFilter_returnsStaticKey() {
        FilteredCountCache cache = newCache();
        assertThat(cache.key("test", null)).isEqualTo("test:null");
    }

    @Test
    void key_noObjectMapper_throws() {
        FilteredCountCache cache = new FilteredCountCache(Duration.ofMinutes(1), 100, null);
        assertThatThrownBy(() -> cache.key("test", new FilterDto("alice", 30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ObjectMapper");
    }

    @Test
    void key_namespaceIncluded() {
        FilteredCountCache cache = newCache();
        FilterDto filter = new FilterDto("alice", 30);
        assertThat(cache.key("ns1", filter)).startsWith("ns1:");
        assertThat(cache.key("ns2", filter)).startsWith("ns2:");
    }

    @Test
    void invalidate_removesKey() {
        FilteredCountCache cache = newCache();
        AtomicInteger counter = new AtomicInteger();
        cache.getOrCompute("k", () -> { counter.incrementAndGet(); return 1L; });
        cache.invalidate("k");
        cache.getOrCompute("k", () -> { counter.incrementAndGet(); return 2L; });
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void invalidateByNamespace_removesMatching() {
        FilteredCountCache cache = newCache();
        cache.getOrCompute("matrix:abc", () -> 1L);
        cache.getOrCompute("matrix:xyz", () -> 2L);
        cache.getOrCompute("other:def", () -> 3L);
        cache.invalidateByNamespace("matrix");

        AtomicInteger counter = new AtomicInteger();
        cache.getOrCompute("matrix:abc", () -> { counter.incrementAndGet(); return 99L; });
        cache.getOrCompute("other:def", () -> { counter.incrementAndGet(); return 99L; });
        assertThat(counter.get()).isEqualTo(1);  // matrix:abc evicted, other:def cached
    }

    @Test
    void invalidateAll_removesEverything() {
        FilteredCountCache cache = newCache();
        cache.getOrCompute("k1", () -> 1L);
        cache.getOrCompute("k2", () -> 2L);
        cache.invalidateAll();

        AtomicInteger counter = new AtomicInteger();
        cache.getOrCompute("k1", () -> { counter.incrementAndGet(); return 1L; });
        cache.getOrCompute("k2", () -> { counter.incrementAndGet(); return 2L; });
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void stats_recordsHitsAndMisses() {
        FilteredCountCache cache = newCache();
        cache.getOrCompute("k", () -> 1L);  // miss
        cache.getOrCompute("k", () -> 2L);  // hit
        cache.getOrCompute("k", () -> 3L);  // hit
        FilteredCountCache.CacheStats stats = cache.stats();
        assertThat(stats.hits()).isEqualTo(2);
        assertThat(stats.misses()).isEqualTo(1);
        assertThat(stats.size()).isEqualTo(1);
    }
}
