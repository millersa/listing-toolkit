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
    public ListResponse {
        // Defensive copy для public API records — caller не должен мутировать ответ
        // через ссылку на исходный Page.getContent().
        data = data == null ? List.of() : List.copyOf(data);
    }

    /** Завернуть Spring {@link Page} как есть. */
    public static <T> ListResponse<T> from(Page<T> page) {
        if (page == null) {
            throw new IllegalArgumentException("Page must not be null");
        }
        return new ListResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages());
    }

    /** Завернуть Spring {@link Page} с маппингом каждой строки. */
    public static <S, T> ListResponse<T> from(Page<S> page, Function<S, T> mapper) {
        if (page == null) {
            throw new IllegalArgumentException("Page must not be null");
        }
        if (mapper == null) {
            throw new IllegalArgumentException("Mapper must not be null");
        }
        return new ListResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(), page.getTotalPages());
    }

    public static <T> ListResponse<T> empty() {
        return new ListResponse<>(List.of(), 0L, 0);
    }
}
