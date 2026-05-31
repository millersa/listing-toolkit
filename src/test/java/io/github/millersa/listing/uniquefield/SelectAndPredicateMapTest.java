package io.github.millersa.listing.uniquefield;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SelectAndPredicateMapTest {

    @Test
    void put_get_caseInsensitive() {
        SelectAndPredicateMap<Object> map = new SelectAndPredicateMap<>();
        SelectAndPredicateCreator<Object> creator = (r, q, cb, s) -> null;
        map.put("FieldName", creator);

        assertThat(map.get("fieldname")).isSameAs(creator);
        assertThat(map.get("FIELDNAME")).isSameAs(creator);
        assertThat(map.get("FieldName")).isSameAs(creator);
    }

    @Test
    void get_unknownKey_returnsNull() {
        SelectAndPredicateMap<Object> map = new SelectAndPredicateMap<>();
        assertThat(map.get("missing")).isNull();
    }

    @Test
    void get_nullKey_returnsNull() {
        SelectAndPredicateMap<Object> map = new SelectAndPredicateMap<>();
        assertThat(map.get(null)).isNull();
    }

    @Test
    void containsKey_works() {
        SelectAndPredicateMap<Object> map = new SelectAndPredicateMap<>();
        map.put("name", (r, q, cb, s) -> null);

        assertThat(map.containsKey("name")).isTrue();
        assertThat(map.containsKey("NAME")).isTrue();
        assertThat(map.containsKey("missing")).isFalse();
        assertThat(map.containsKey(null)).isFalse();
    }

    @Test
    void size_reflectsPuts() {
        SelectAndPredicateMap<Object> map = new SelectAndPredicateMap<>();
        assertThat(map.size()).isZero();
        map.put("a", (r, q, cb, s) -> null);
        map.put("b", (r, q, cb, s) -> null);
        assertThat(map.size()).isEqualTo(2);
    }

    @Test
    void put_sameKeyDifferentCase_overwrites() {
        SelectAndPredicateMap<Object> map = new SelectAndPredicateMap<>();
        SelectAndPredicateCreator<Object> first = (r, q, cb, s) -> null;
        SelectAndPredicateCreator<Object> second = (r, q, cb, s) -> null;
        map.put("name", first);
        map.put("NAME", second);

        assertThat(map.size()).isEqualTo(1);
        assertThat(map.get("Name")).isSameAs(second);
    }
}
