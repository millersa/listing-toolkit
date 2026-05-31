package io.github.millersa.listing.pagination;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.support.PageableExecutionUtils;
import io.github.millersa.listing.uniquefield.QueryPair;
import io.github.millersa.listing.uniquefield.UniqueFieldHelperMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

/**
 * Реализация {@link CustomJpaSpecificationExecutor}. Подключается через
 * {@code @EnableJpaRepositories(repositoryBaseClass = CustomJpaSpecificationExecutorImpl.class)}
 * на главном классе Spring Boot приложения.
 */
public class CustomJpaSpecificationExecutorImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID>
        implements CustomJpaSpecificationExecutor<T> {

    private static final int DEFAULT_STREAM_FETCH_SIZE = 500;

    private final EntityManager entityManager;

    public CustomJpaSpecificationExecutorImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    public CustomJpaSpecificationExecutorImpl(Class<T> domainClass, EntityManager em) {
        super(domainClass, em);
        this.entityManager = em;
    }

    @Override
    public Page<T> findAll(Specification<T> spec, Pageable pageable, EntityGraph<T> entityGraph) {
        TypedQuery<T> query = getQuery(spec, pageable);
        if (entityGraph != null) {
            query.setHint(GraphSemantic.FETCH.getJakartaHintName(), entityGraph);
        }
        return pageable.isUnpaged()
                ? new PageImpl<>(query.getResultList())
                : readPage(query, getDomainClass(), pageable, spec);
    }

    @Override
    public List<T> findAll(Specification<T> spec, EntityGraph<T> entityGraph) {
        TypedQuery<T> query = getQuery(spec, Sort.unsorted());
        if (entityGraph != null) {
            query.setHint(GraphSemantic.FETCH.getJakartaHintName(), entityGraph);
        }
        return query.getResultList();
    }

    @Override
    public Stream<T> streamAll(Specification<T> spec, Pageable pageable, EntityGraph<T> entityGraph) {
        Sort sort = pageable == null ? Sort.unsorted() : pageable.getSort();
        TypedQuery<T> query = getQuery(spec, sort);
        if (entityGraph != null) {
            query.setHint(GraphSemantic.FETCH.getJakartaHintName(), entityGraph);
        }
        query.setHint(HibernateHints.HINT_FETCH_SIZE, DEFAULT_STREAM_FETCH_SIZE);
        return query.getResultStream();
    }

    @Override
    public Stream<T> streamAll(Specification<T> spec) {
        TypedQuery<T> query = getQuery(spec, Sort.unsorted());
        query.setHint(HibernateHints.HINT_FETCH_SIZE, DEFAULT_STREAM_FETCH_SIZE);
        return query.getResultStream();
    }

    @Override
    public Page<T> findUniqueValues(Specification<T> spec, String fieldName, String fieldSearch,
                                    int offset, int limit, UniqueFieldHelperMap<T> helperMap,
                                    EntityGraph<T> entityGraph) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0, got: " + offset);
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0, got: " + limit);
        }
        TypedQuery<Tuple> typedQuery = buildUniqueValuesQuery(spec, helperMap, fieldName, fieldSearch, entityGraph);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        typedQuery.setFirstResult(offset);
        typedQuery.setMaxResults(limit);

        Function<Tuple, T> transformer = helperMap.getTransformer(fieldName);
        List<T> entities = typedQuery.getResultList().stream().map(transformer).toList();

        long totalCount = countDistinctValues(spec, helperMap, fieldName, fieldSearch);
        return PageableExecutionUtils.getPage(entities, pageable, () -> totalCount);
    }

    @Override
    public List<T> findUniqueValues(Specification<T> spec, String fieldName, String fieldSearch,
                                    UniqueFieldHelperMap<T> helperMap) {
        TypedQuery<Tuple> typedQuery = buildUniqueValuesQuery(spec, helperMap, fieldName, fieldSearch, null);
        Function<Tuple, T> transformer = helperMap.getTransformer(fieldName);
        return typedQuery.getResultList().stream().map(transformer).toList();
    }

    /**
     * Собирает CriteriaQuery для выборки уникальных значений по полю helperMap с учётом фильтров spec.
     * Сортировка по самим Selection-выражениям (переносимо между диалектами).
     */
    private TypedQuery<Tuple> buildUniqueValuesQuery(Specification<T> spec,
                                                     UniqueFieldHelperMap<T> helperMap,
                                                     String fieldName,
                                                     String fieldSearch,
                                                     EntityGraph<T> entityGraph) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> tupleQuery = cb.createTupleQuery();
        Root<T> root = tupleQuery.from(this.getDomainClass());

        QueryPair queryPair = helperMap.getCreator(fieldName).create(root, tupleQuery, cb, fieldSearch);
        List<Selection<?>> selections = queryPair.getSelections();
        tupleQuery.multiselect(selections);
        tupleQuery.distinct(true);

        List<Predicate> predicates = new ArrayList<>();
        if (nonNull(spec)) {
            Predicate specPredicate = spec.toPredicate(root, tupleQuery, cb);
            if (nonNull(specPredicate)) {
                predicates.add(specPredicate);
            }
        }
        if (nonNull(queryPair.getPredicate())) {
            predicates.add(queryPair.getPredicate());
        }
        if (!predicates.isEmpty()) {
            tupleQuery.where(cb.and(predicates.toArray(Predicate[]::new)));
        }

        List<Order> orders = selections.stream()
                .map(s -> cb.asc((Expression<?>) s))
                .toList();
        tupleQuery.orderBy(orders);

        TypedQuery<Tuple> typedQuery = entityManager.createQuery(tupleQuery);
        if (nonNull(entityGraph)) {
            typedQuery.setHint(GraphSemantic.FETCH.getJakartaHintName(), entityGraph);
        }
        return typedQuery;
    }

    /**
     * Отдельный COUNT-запрос для подсчёта уникальных значений вместо материализации стрима.
     * Для multi-field — concat через служебный символ.
     */
    private long countDistinctValues(Specification<T> spec, UniqueFieldHelperMap<T> helperMap,
                                     String fieldName, String fieldSearch) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<T> countRoot = countQuery.from(this.getDomainClass());

        QueryPair countPair = helperMap.getCreator(fieldName).create(countRoot, countQuery, cb, fieldSearch);
        List<Selection<?>> selections = countPair.getSelections();

        Expression<?> distinctExpression;
        if (selections.size() == 1) {
            distinctExpression = (Expression<?>) selections.get(0);
        } else {
            Expression<String> concat = ((Expression<?>) selections.get(0)).as(String.class);
            for (int i = 1; i < selections.size(); i++) {
                concat = cb.concat(cb.concat(concat, ""),
                        ((Expression<?>) selections.get(i)).as(String.class));
            }
            distinctExpression = concat;
        }
        countQuery.select(cb.countDistinct(distinctExpression));

        List<Predicate> countPredicates = new ArrayList<>();
        if (nonNull(spec)) {
            Predicate specPredicate = spec.toPredicate(countRoot, countQuery, cb);
            if (nonNull(specPredicate)) {
                countPredicates.add(specPredicate);
            }
        }
        if (nonNull(countPair.getPredicate())) {
            countPredicates.add(countPair.getPredicate());
        }
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(Predicate[]::new)));
        }

        Long result = entityManager.createQuery(countQuery).getSingleResult();
        return result == null ? 0L : result;
    }
}
