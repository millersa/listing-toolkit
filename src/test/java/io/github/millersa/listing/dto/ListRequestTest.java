package io.github.millersa.listing.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListRequestTest {

    record DummyFilter(String name) {}

    @Test
    void nullPagination_replacedWithDefault() {
        ListRequest<DummyFilter> r = new ListRequest<>(new DummyFilter("x"), null, null, null);
        assertThat(r.pagination().limit()).isEqualTo(Pagination.DEFAULT_LIMIT);
        assertThat(r.pagination().offset()).isZero();
    }

    @Test
    void nullSorting_replacedWithUnsorted() {
        ListRequest<DummyFilter> r = new ListRequest<>(null, null, null, null);
        assertThat(r.sorting().isEmpty()).isTrue();
    }

    @Test
    void isUniqueFieldMode_falseWhenNull() {
        ListRequest<DummyFilter> r = new ListRequest<>(null, null, null, null);
        assertThat(r.isUniqueFieldMode()).isFalse();
    }

    @Test
    void isUniqueFieldMode_falseWhenEmpty() {
        ListRequest<DummyFilter> r = new ListRequest<>(null, null, null, new UniqueField(null, "x"));
        assertThat(r.isUniqueFieldMode()).isFalse();
    }

    @Test
    void isUniqueFieldMode_trueWhenSet() {
        ListRequest<DummyFilter> r = new ListRequest<>(null, null, null, UniqueField.of("name", "a"));
        assertThat(r.isUniqueFieldMode()).isTrue();
    }

    @Test
    void of_convenienceFactory() {
        ListRequest<DummyFilter> r = ListRequest.of(new DummyFilter("x"), Pagination.first(50));
        assertThat(r.filter()).isEqualTo(new DummyFilter("x"));
        assertThat(r.pagination().limit()).isEqualTo(50);
        assertThat(r.uniqueField()).isNull();
        assertThat(r.sorting().isEmpty()).isTrue();
    }

    @Test
    void of_withSorting() {
        ListRequest<DummyFilter> r = ListRequest.of(
                new DummyFilter("x"),
                Pagination.first(10),
                Sorting.of("desc(id)"));
        assertThat(r.sorting().sortBy()).containsExactly("desc(id)");
        assertThat(r.uniqueField()).isNull();
    }
}
