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
}
