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
        // null-элементы отбрасываются с понятным сообщением (иначе List.copyOf бросил бы безымянный NPE).
        if (sortBy == null) {
            sortBy = List.of();
        } else {
            for (String token : sortBy) {
                if (token == null) {
                    throw new IllegalArgumentException("sortBy must not contain null tokens");
                }
            }
            sortBy = List.copyOf(sortBy);
        }
    }

    public static Sorting unsorted() {
        return new Sorting(List.of());
    }

    /**
     * Удобный конструктор: {@code Sorting.of("desc(createdAt)", "asc(id)")}.
     *
     * @throws IllegalArgumentException если среди tokens есть {@code null}
     */
    public static Sorting of(String... tokens) {
        if (tokens == null) {
            return unsorted();
        }
        return new Sorting(java.util.Arrays.asList(tokens));  // через конструктор → null-проверка + copy
    }

    public boolean isEmpty() {
        return sortBy.isEmpty();
    }
}
