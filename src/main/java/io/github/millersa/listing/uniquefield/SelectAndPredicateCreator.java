package io.github.millersa.listing.uniquefield;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Интерфейс позволяет определить пары select-where для выборки одной колонки сущности.
 *
 * @param <R> сущность для которой будет выполняться запрос
 */
@FunctionalInterface
public interface SelectAndPredicateCreator<R> {
    QueryPair create(Root<R> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder, String fieldValue);
}
