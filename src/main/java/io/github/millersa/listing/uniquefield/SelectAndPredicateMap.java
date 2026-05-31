package io.github.millersa.listing.uniquefield;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Мапа для построения выборки уникальных значений. Ключи нормализуются к нижнему регистру
 * с {@link Locale#ROOT}, чтобы избежать Turkish-I проблем.
 *
 * <p>Композиция вместо наследования от {@link HashMap}, чтобы не нарушать контракт
 * {@link Map#get(Object)} (который должен возвращать {@code null}, а не бросать исключение).</p>
 *
 * @param <T> entity для которой строится выборка
 */
public final class SelectAndPredicateMap<T> {

    private final Map<String, SelectAndPredicateCreator<T>> delegate = new HashMap<>();

    public void put(String key, SelectAndPredicateCreator<T> value) {
        delegate.put(normalize(key), value);
    }

    public SelectAndPredicateCreator<T> get(String key) {
        if (key == null) {
            return null;
        }
        return delegate.get(normalize(key));
    }

    public boolean containsKey(String key) {
        return key != null && delegate.containsKey(normalize(key));
    }

    public int size() {
        return delegate.size();
    }

    private static String normalize(String key) {
        return key.toLowerCase(Locale.ROOT);
    }
}
