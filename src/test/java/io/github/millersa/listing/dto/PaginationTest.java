package io.github.millersa.listing.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationTest {

    @Test
    void normalValues_passThrough() {
        Pagination p = new Pagination(50, 100);
        assertThat(p.limit()).isEqualTo(50);
        assertThat(p.offset()).isEqualTo(100);
    }

    @Test
    void negativeLimit_normalizedToDefault() {
        Pagination p = new Pagination(-5, 0);
        assertThat(p.limit()).isEqualTo(Pagination.DEFAULT_LIMIT);
    }

    @Test
    void zeroLimit_normalizedToDefault() {
        // Согласовано с AbstractOffsetSortedRequest: 0 трактуется как «не указано».
        Pagination p = new Pagination(0, 0);
        assertThat(p.limit()).isEqualTo(Pagination.DEFAULT_LIMIT);
    }

    @Test
    void negativeOffset_normalizedToZero() {
        Pagination p = new Pagination(10, -100);
        assertThat(p.offset()).isZero();
    }

    @Test
    void first_zeroOffset() {
        Pagination p = Pagination.first(25);
        assertThat(p.limit()).isEqualTo(25);
        assertThat(p.offset()).isZero();
    }

    @Test
    void unpaged_largeLimit() {
        Pagination p = Pagination.unpaged();
        assertThat(p.limit()).isEqualTo(Integer.MAX_VALUE);
        assertThat(p.offset()).isZero();
    }
}
