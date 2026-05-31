package io.github.millersa.listing.uniquefield;

import jakarta.persistence.Tuple;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Формирует мапу с функциями для получения списка уникальных значений.
 * В {@code selectAndPredicateMap} — функция формирования запроса на получение уникальных значений.
 * В {@code transformerMap} — функция конвертации tuple в entity.
 *
 * @param <T> entity для которой хотим получить уникальные значения
 */
public class UniqueFieldHelperMap<T> {

    private final SelectAndPredicateMap<T> selectAndPredicateMap = new SelectAndPredicateMap<>();
    private final Map<String, Function<Tuple, T>> transformerMap = new HashMap<>();

    public void put(String key, SelectAndPredicateCreator<T> selectAndPredicateCreator,
                    Function<Tuple, T> tupleToEntityTransformer) {
        if (key == null) {
            throw new IllegalArgumentException("Field key must not be null");
        }
        selectAndPredicateMap.put(key, selectAndPredicateCreator);
        transformerMap.put(key.toLowerCase(Locale.ROOT), tupleToEntityTransformer);
    }

    /**
     * @throws FieldNotFoundException если поле не зарегистрировано через {@link #put}.
     */
    public SelectAndPredicateCreator<T> getCreator(String key) {
        SelectAndPredicateCreator<T> creator = selectAndPredicateMap.get(key);
        if (creator == null) {
            throw new FieldNotFoundException("Field '" + key + "' is not registered in UniqueFieldHelperMap");
        }
        return creator;
    }

    /**
     * @throws FieldNotFoundException если поле не зарегистрировано через {@link #put}.
     */
    public Function<Tuple, T> getTransformer(String key) {
        if (key == null) {
            throw new FieldNotFoundException("Field key must not be null");
        }
        Function<Tuple, T> transformer = transformerMap.get(key.toLowerCase(Locale.ROOT));
        if (transformer == null) {
            throw new FieldNotFoundException("Transformer for field '" + key + "' is not registered");
        }
        return transformer;
    }
}
