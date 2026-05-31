package io.github.millersa.listing.dto;

/**
 * Запрос на листинг: фильтр пользователя + пагинация + сортировка + (опционально) режим уникальных значений.
 *
 * <p>Подходит как {@code @RequestBody} для контроллеров. JSON-форма:</p>
 * <pre>{@code
 * {
 *   "filter":     { ... your filter DTO ... },
 *   "pagination": { "limit": 20, "offset": 0 },
 *   "sorting":    { "sortBy": ["desc(createdAt)", "asc(name)"] },
 *   "uniqueField": { "fieldName": "name", "fieldSearch": "ali" }
 * }
 * }</pre>
 *
 * @param <F> тип фильтра приложения (свой для каждого endpoint-а)
 */
public record ListRequest<F>(
        F filter,
        Pagination pagination,
        Sorting sorting,
        UniqueField uniqueField
) {
    public ListRequest {
        if (pagination == null) pagination = new Pagination(Pagination.DEFAULT_LIMIT, 0);
        if (sorting == null) sorting = Sorting.unsorted();
    }

    /** Запрос работает в режиме «уникальные значения» (autocomplete), а не «список». */
    public boolean isUniqueFieldMode() {
        return uniqueField != null && !uniqueField.isEmpty();
    }

    /** Удобный конструктор только с фильтром и пагинацией. */
    public static <F> ListRequest<F> of(F filter, Pagination pagination) {
        return new ListRequest<>(filter, pagination, Sorting.unsorted(), null);
    }
}
