package io.github.millersa.listing.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractOffsetSortedRequestTest {

    @Test
    void nullLimit_usesDefault() {
        Pageable p = new SimpleOffsetSortedRequest(null, 0);
        assertThat(p.getPageSize()).isEqualTo(20);
    }

    @Test
    void negativeLimit_usesDefault() {
        Pageable p = new SimpleOffsetSortedRequest(-5, 0);
        assertThat(p.getPageSize()).isEqualTo(20);
    }

    @Test
    void zeroLimit_usesDefault() {
        Pageable p = new SimpleOffsetSortedRequest(0, 0);
        assertThat(p.getPageSize()).isEqualTo(20);
    }

    @Test
    void nullOffset_usesZero() {
        Pageable p = new SimpleOffsetSortedRequest(10, null);
        assertThat(p.getOffset()).isZero();
    }

    @Test
    void negativeOffset_usesZero() {
        Pageable p = new SimpleOffsetSortedRequest(10, -100);
        assertThat(p.getOffset()).isZero();
    }

    @Test
    void normalValues_passThrough() {
        Pageable p = new SimpleOffsetSortedRequest(25, 50);
        assertThat(p.getPageSize()).isEqualTo(25);
        assertThat(p.getOffset()).isEqualTo(50);
        assertThat(p.getPageNumber()).isEqualTo(2);  // 50/25
    }

    @Test
    void nullSort_isUnsorted() {
        Pageable p = new SimpleOffsetSortedRequest(10, 0, null);
        assertThat(p.getSort().isUnsorted()).isTrue();
    }

    @Test
    void hasPrevious_offsetAtZero_false() {
        Pageable p = new SimpleOffsetSortedRequest(10, 0);
        assertThat(p.hasPrevious()).isFalse();
    }

    @Test
    void hasPrevious_offsetEqualsLimit_true() {
        Pageable p = new SimpleOffsetSortedRequest(10, 10);
        assertThat(p.hasPrevious()).isTrue();
    }

    @Test
    void previousOrFirst_atZero_returnsFirst() {
        Pageable p = new SimpleOffsetSortedRequest(10, 0);
        Pageable prev = p.previousOrFirst();
        assertThat(prev.getOffset()).isZero();
    }

    @Test
    void previousOrFirst_atMiddle_decrementsByPageSize() {
        Pageable p = new SimpleOffsetSortedRequest(10, 50);
        Pageable prev = p.previousOrFirst();
        assertThat(prev.getOffset()).isEqualTo(40);
    }

    @Test
    void next_incrementsByPageSize() {
        Pageable p = new SimpleOffsetSortedRequest(10, 50);
        Pageable next = p.next();
        assertThat(next.getOffset()).isEqualTo(60);
    }

    @Test
    void withPage_replacesOffset() {
        SimpleOffsetSortedRequest p = new SimpleOffsetSortedRequest(20, 100, Sort.by("name"));
        Pageable updated = p.withPage(300);
        assertThat(updated.getOffset()).isEqualTo(300);
        assertThat(updated.getPageSize()).isEqualTo(20);
        assertThat(updated.getSort().toString()).contains("name");
    }
}
