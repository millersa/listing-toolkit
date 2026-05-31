package io.github.millersa.listing.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniqueFieldTest {

    @Test
    void empty_whenFieldNameNull() {
        assertThat(new UniqueField(null, "x").isEmpty()).isTrue();
    }

    @Test
    void empty_whenFieldNameBlank() {
        assertThat(new UniqueField("  ", "x").isEmpty()).isTrue();
    }

    @Test
    void notEmpty_whenFieldNamePresent() {
        assertThat(UniqueField.of("name", "ali").isEmpty()).isFalse();
    }

    @Test
    void of_singleArg_noSearch() {
        UniqueField uf = UniqueField.of("name");
        assertThat(uf.fieldName()).isEqualTo("name");
        assertThat(uf.fieldSearch()).isNull();
    }
}
