package io.github.millersa.listing.pagination;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Attribute;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public final class RepositoryUtils {

    private static final String PERCENT = "%";

    /** Формат PostgreSQL для {@code to_char} — 19 девяток, чтобы вместить {@code Long.MAX_VALUE} (9 223 372 036 854 775 807). */
    private static final String LONG_TO_CHAR_FORMAT = "FM9999999999999999999";

    private RepositoryUtils() {
    }

    /**
     * Ищет существующий join в {@link From} по имени поля и типу, либо создаёт новый.
     * Помогает избежать дублирующих JOIN-ов в одном запросе.
     */
    public static <T extends From<?, ?>> Join<?, ?> getOrCreateJoin(T table, String fieldName, JoinType joinType) {
        Optional<? extends Join<?, ?>> foundJoin = table.getJoins().stream()
                .filter(j -> fieldName.equals(j.getAttribute().getName()) && joinType.equals(j.getJoinType()))
                .findFirst();
        return foundJoin.isPresent() ? foundJoin.get() : table.join(fieldName, joinType);
    }

    /**
     * Типизированный вариант для использования с JPA Static Metamodel.
     */
    @SuppressWarnings("unchecked")
    public static <T extends From<F, L>, F, L, X, Y> Join<X, Y> getOrCreateJoin(
            T table, Attribute<X, Y> field, JoinType joinType) {
        return (Join<X, Y>) getOrCreateJoin(table, field.getName(), joinType);
    }

    /**
     * Case-insensitive LIKE-фильтр с нормализацией множественных пробелов.
     * <p>
     * <b>Производительность:</b> ведущий wildcard {@code '%x%'} + функция по полю заставляет PostgreSQL
     * делать seq scan. Для часто используемых полей рекомендуется создать GIN-индекс через расширение {@code pg_trgm}:
     * <pre>{@code CREATE INDEX ... USING gin (lower(regexp_replace(field, '\s+', ' ')) gin_trgm_ops)}</pre>
     */
    public static Predicate likeIgnoreCaseWithWhitespaceNormalize(
            CriteriaBuilder cb, Expression<String> field, String fieldSearch) {
        return cb.like(
                cb.trim(cb.function("regexp_replace", String.class, cb.lower(field),
                        cb.literal("\\s+"), cb.literal(" "))),
                cb.lower(cb.literal(StringUtils.wrap(fieldSearch.replaceAll("\\s+", " "), PERCENT)))
        );
    }

    /**
     * LIKE-поиск по числовому полю через приведение к строке.
     * Использует формат с {@code FM} — без ведущих пробелов и с 19 цифрами для {@code Long.MAX_VALUE}.
     */
    public static Predicate likeIgnoreCaseWithWhitespaceNormalizeForLong(
            CriteriaBuilder cb, Expression<Long> field, String fieldSearch) {
        String normalizedFieldSearch = fieldSearch.trim().replaceAll("\\s+", " ").toLowerCase();
        Expression<String> fieldAsString = cb.function("to_char", String.class, field, cb.literal(LONG_TO_CHAR_FORMAT));
        return cb.like(cb.lower(fieldAsString), cb.literal(StringUtils.wrap(normalizedFieldSearch, PERCENT)));
    }
}
