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
        if (limit < 0) limit = DEFAULT_LIMIT;
        if (offset < 0) offset = 0;
    }

    /** Первая страница заданного размера. */
    public static Pagination first(int limit) {
        return new Pagination(limit, 0);
    }

    /** «Без пагинации» — для streaming-экспортов. */
    public static Pagination unpaged() {
        return new Pagination(Integer.MAX_VALUE, 0);
    }
}
