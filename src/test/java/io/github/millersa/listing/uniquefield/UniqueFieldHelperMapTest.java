package io.github.millersa.listing.uniquefield;

import jakarta.persistence.Tuple;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class UniqueFieldHelperMapTest {

    @Test
    void getCreator_registered_returnsCreator() {
        UniqueFieldHelperMap<Object> map = new UniqueFieldHelperMap<>();
        SelectAndPredicateCreator<Object> creator = (r, q, cb, s) -> null;
        Function<Tuple, Object> transformer = t -> new Object();
        map.put("name", creator, transformer);

        assertThat(map.getCreator("name")).isSameAs(creator);
    }

    @Test
    void getCreator_caseInsensitive() {
        UniqueFieldHelperMap<Object> map = new UniqueFieldHelperMap<>();
        SelectAndPredicateCreator<Object> creator = (r, q, cb, s) -> null;
        Function<Tuple, Object> transformer = t -> new Object();
        map.put("Name", creator, transformer);

        assertThat(map.getCreator("NAME")).isSameAs(creator);
        assertThat(map.getCreator("name")).isSameAs(creator);
    }

    @Test
    void getCreator_notRegistered_throws() {
        UniqueFieldHelperMap<Object> map = new UniqueFieldHelperMap<>();
        assertThatThrownBy(() -> map.getCreator("missing"))
                .isInstanceOf(FieldNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getCreator_nullKey_throws() {
        UniqueFieldHelperMap<Object> map = new UniqueFieldHelperMap<>();
        assertThatThrownBy(() -> map.getCreator(null))
                .isInstanceOf(FieldNotFoundException.class);
    }

    @Test
    void getTransformer_registered_returnsTransformer() {
        UniqueFieldHelperMap<String> map = new UniqueFieldHelperMap<>();
        Function<Tuple, String> transformer = t -> "result";
        map.put("name", (r, q, cb, s) -> null, transformer);

        assertThat(map.getTransformer("name")).isSameAs(transformer);
    }

    @Test
    void getTransformer_caseInsensitive() {
        UniqueFieldHelperMap<String> map = new UniqueFieldHelperMap<>();
        Function<Tuple, String> transformer = t -> "result";
        map.put("Name", (r, q, cb, s) -> null, transformer);

        assertThat(map.getTransformer("NAME")).isSameAs(transformer);
    }

    @Test
    void getTransformer_notRegistered_throws() {
        UniqueFieldHelperMap<Object> map = new UniqueFieldHelperMap<>();
        assertThatThrownBy(() -> map.getTransformer("missing"))
                .isInstanceOf(FieldNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getTransformer_nullKey_throws() {
        UniqueFieldHelperMap<Object> map = new UniqueFieldHelperMap<>();
        assertThatThrownBy(() -> map.getTransformer(null))
                .isInstanceOf(FieldNotFoundException.class);
    }

    @Test
    void put_nullKey_throws() {
        UniqueFieldHelperMap<Object> map = new UniqueFieldHelperMap<>();
        assertThatThrownBy(() -> map.put(null, (r, q, cb, s) -> null, t -> new Object()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void put_overwritesPreviousValue() {
        UniqueFieldHelperMap<Object> map = new UniqueFieldHelperMap<>();
        SelectAndPredicateCreator<Object> first = (r, q, cb, s) -> null;
        SelectAndPredicateCreator<Object> second = (r, q, cb, s) -> mock(Object.class).hashCode() == 0 ? null : null;
        map.put("name", first, t -> new Object());
        map.put("name", second, t -> new Object());

        assertThat(map.getCreator("name")).isSameAs(second);
    }
}
