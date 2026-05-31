package io.github.millersa.listing.dto;

/**
 * Описание режима «выборка уникальных значений по одному полю».
 *
 * <p>{@code fieldName} — ключ, зарегистрированный в
 * {@link io.github.millersa.listing.uniquefield.UniqueFieldHelperMap}.
 * {@code fieldSearch} — необязательная подстрока поиска (например, ввод в autocomplete).</p>
 */
public record UniqueField(String fieldName, String fieldSearch) {

    public boolean isEmpty() {
        return fieldName == null || fieldName.isBlank();
    }

    public static UniqueField of(String fieldName) {
        return new UniqueField(fieldName, null);
    }

    public static UniqueField of(String fieldName, String fieldSearch) {
        return new UniqueField(fieldName, fieldSearch);
    }
}
