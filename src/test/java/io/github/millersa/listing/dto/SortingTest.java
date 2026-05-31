package io.github.millersa.listing.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SortingTest {

    @Test
    void nullSortBy_replacedWithEmptyList() {
        Sorting s = new Sorting(null);
        assertThat(s.sortBy()).isEmpty();
        assertThat(s.isEmpty()).isTrue();
    }

    @Test
    void unsorted_isEmpty() {
        assertThat(Sorting.unsorted().isEmpty()).isTrue();
    }

    @Test
    void of_variadicTokens() {
        Sorting s = Sorting.of("desc(createdAt)", "asc(id)");
        assertThat(s.sortBy()).containsExactly("desc(createdAt)", "asc(id)");
        assertThat(s.isEmpty()).isFalse();
    }
}
