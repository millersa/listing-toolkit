package io.github.millersa.listing.dto;

/**
 * Offset-based пагинация. {@code limit} = размер страницы, {@code offset} = номер первой записи (0-based).
 *
 * <p>Эквивалент REST-параметрам {@code ?limit=20&offset=100}.</p>
 *
 * <p>Конструктор нормализует невалидные значения: отрицательный {@code limit} → 20, отрицательный {@code offset} → 0.
 * Это сделано умышленно для совместимости с {@code @RequestBody} DTO, где клиент может прислать что угодно.</p>
 */
public record Pagination(int limit, int offset) {

    public static final int DEFAULT_LIMIT = 20;

    public Pagination {
        // 0 и negative ведут к одному поведению: нормализация на DEFAULT_LIMIT.
        // Это согласовано с AbstractOffsetSortedRequest.defaultLimitIfNull.
        if (limit <= 0) limit = DEFAULT_LIMIT;
        if (offset < 0) offset = 0;
    }

    /** Первая страница заданного размера. */
    public static Pagination first(int limit) {
        return new Pagination(limit, 0);
    }

    /**
     * «Очень большой лимит» — {@code limit = Integer.MAX_VALUE, offset = 0}.
     * <p>
     * <b>Внимание:</b> это НЕ настоящий unpaged-режим. При передаче в JPA через
     * {@link io.github.millersa.listing.pagination.SimpleOffsetSortedRequest} Hibernate сгенерирует
     * {@code LIMIT 2147483647}. На PostgreSQL это нормализуется, но на других диалектах (Oracle и т.п.)
     * поведение может отличаться.
     * <p>
     * Для настоящего стриминга больших данных используйте {@code repo.streamAll(spec, Pageable.unpaged(), graph)}
     * со спринговой константой {@link org.springframework.data.domain.Pageable#unpaged()} — Hibernate
     * не добавит LIMIT вообще.
     */
    public static Pagination unpaged() {
        return new Pagination(Integer.MAX_VALUE, 0);
    }
}
