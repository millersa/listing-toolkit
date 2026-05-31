package io.github.millersa.listing.dto;

import java.util.List;

/**
 * Сортировка как список строк вида {@code "asc(field)"} / {@code "desc(field)"}.
 * Парсится через {@link io.github.millersa.listing.pagination.SortUtils#parsedSort(java.util.List)}.
 */
public record Sorting(List<String> sortBy) {

    public Sorting {
        // Defensive copy: гарантирует immutability даже если caller передал mutable ArrayList
        // (типичный случай при Jackson-десериализации) или потом мутирует исходник.
        sortBy = sortBy == null ? List.of() : List.copyOf(sortBy);
    }

    public static Sorting unsorted() {
        return new Sorting(List.of());
    }

    /** Удобный конструктор: {@code Sorting.of("desc(createdAt)", "asc(id)")}. */
    public static Sorting of(String... tokens) {
        return new Sorting(List.of(tokens));
    }

    public boolean isEmpty() {
        return sortBy.isEmpty();
    }
}
