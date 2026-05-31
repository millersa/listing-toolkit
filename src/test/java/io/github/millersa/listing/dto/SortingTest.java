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

    @Test
    void sortBy_immutable_evenIfMutableInputProvided() {
        java.util.List<String> mutable = new java.util.ArrayList<>(java.util.List.of("asc(id)"));
        Sorting s = new Sorting(mutable);
        mutable.clear();
        // Sorting должен иметь свою defensive copy
        assertThat(s.sortBy()).containsExactly("asc(id)");
    }

    @Test
    void sortBy_returnedListNotMutable() {
        Sorting s = Sorting.of("asc(id)");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> s.sortBy().add("desc(name)"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
