package io.github.millersa.listing.pagination;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortUtilsTest {

    @Test
    void parsedSort_emptyInput_returnsEmptyList() {
        assertThat(SortUtils.parsedSort(null)).isEmpty();
        assertThat(SortUtils.parsedSort(List.of())).isEmpty();
    }

    @Test
    void parsedSort_validAsc_returnsAscPair() {
        List<Pair<String, Sort.Direction>> result = SortUtils.parsedSort(List.of("asc(name)"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKey()).isEqualTo("name");
        assertThat(result.get(0).getValue()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void parsedSort_validDesc_returnsDescPair() {
        List<Pair<String, Sort.Direction>> result = SortUtils.parsedSort(List.of("desc(createdAt)"));
        assertThat(result.get(0).getValue()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void parsedSort_multipleExpressions_keepsOrder() {
        List<Pair<String, Sort.Direction>> result = SortUtils.parsedSort(List.of("asc(a)", "desc(b)", "asc(c)"));
        assertThat(result).extracting(Pair::getKey).containsExactly("a", "b", "c");
        assertThat(result).extracting(Pair::getValue)
                .containsExactly(Sort.Direction.ASC, Sort.Direction.DESC, Sort.Direction.ASC);
    }

    @Test
    void parsedSort_caseInsensitiveDirection() {
        assertThat(SortUtils.parsedSort(List.of("ASC(field)")).get(0).getValue()).isEqualTo(Sort.Direction.ASC);
        assertThat(SortUtils.parsedSort(List.of("Desc(field)")).get(0).getValue()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void parsedSort_missingParens_throws() {
        assertThatThrownBy(() -> SortUtils.parsedSort(List.of("asc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort expression");
    }

    @Test
    void parsedSort_emptyFieldName_throws() {
        assertThatThrownBy(() -> SortUtils.parsedSort(List.of("asc()")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort expression");
    }

    @Test
    void parsedSort_invalidDirection_throws() {
        assertThatThrownBy(() -> SortUtils.parsedSort(List.of("up(name)")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort direction")
                .hasMessageContaining("up");
    }
}
