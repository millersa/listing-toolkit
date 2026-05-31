package io.github.millersa.listing.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListResponseTest {

    @Test
    void from_pageWrapsCorrectly() {
        Page<String> page = new PageImpl<>(List.of("a", "b", "c"), PageRequest.of(0, 10), 23);
        ListResponse<String> resp = ListResponse.from(page);
        assertThat(resp.data()).containsExactly("a", "b", "c");
        assertThat(resp.totalElements()).isEqualTo(23);
        assertThat(resp.totalPages()).isEqualTo(3);
    }

    @Test
    void from_pageWithMapper() {
        Page<Integer> page = new PageImpl<>(List.of(1, 2, 3), PageRequest.of(0, 10), 3);
        ListResponse<String> resp = ListResponse.from(page, i -> "n=" + i);
        assertThat(resp.data()).containsExactly("n=1", "n=2", "n=3");
        assertThat(resp.totalElements()).isEqualTo(3);
    }

    @Test
    void empty_zeroEverything() {
        ListResponse<Object> resp = ListResponse.empty();
        assertThat(resp.data()).isEmpty();
        assertThat(resp.totalElements()).isZero();
        assertThat(resp.totalPages()).isZero();
    }

    @Test
    void from_nullPage_throws() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ListResponse.from(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page");
    }

    @Test
    void fromWithMapper_nullPage_throws() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> ListResponse.from(null, x -> x))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page");
    }

    @Test
    void fromWithMapper_nullMapper_throws() {
        Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(0, 10), 1);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> ListResponse.from(page, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mapper");
    }

    @Test
    void data_immutable_evenIfMutableInputProvided() {
        java.util.ArrayList<String> mutable = new java.util.ArrayList<>(List.of("a", "b"));
        ListResponse<String> resp = new ListResponse<>(mutable, 2L, 1);
        mutable.clear();
        // Defensive copy в record-constructor
        assertThat(resp.data()).containsExactly("a", "b");
    }

    @Test
    void data_returnedListNotMutable() {
        ListResponse<String> resp = new ListResponse<>(List.of("a"), 1L, 1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> resp.data().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
