package io.github.millersa.listing.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Ответ листинга: страница данных + метаданные пагинации.
 *
 * @param <T> тип DTO-строки таблицы
 */
public record ListResponse<T>(
        List<T> data,
        long totalElements,
        int totalPages
) {
    /** Завернуть Spring {@link Page} как есть. */
    public static <T> ListResponse<T> from(Page<T> page) {
        return new ListResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages());
    }

    /** Завернуть Spring {@link Page} с маппингом каждой строки. */
    public static <S, T> ListResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new ListResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(), page.getTotalPages());
    }

    public static <T> ListResponse<T> empty() {
        return new ListResponse<>(List.of(), 0L, 0);
    }
}
