package io.github.millersa.listing.uniquefield;

/**
 * Бросается, когда запрошено поле, не зарегистрированное в {@link UniqueFieldHelperMap}.
 */
public class FieldNotFoundException extends RuntimeException {

    public FieldNotFoundException(String message) {
        super(message);
    }
}
