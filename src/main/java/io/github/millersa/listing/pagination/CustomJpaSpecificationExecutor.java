package io.github.millersa.listing.pagination;

import jakarta.persistence.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.Nullable;
import io.github.millersa.listing.dto.ListRequest;
import io.github.millersa.listing.dto.Pagination;
import io.github.millersa.listing.dto.UniqueField;
import io.github.millersa.listing.uniquefield.UniqueFieldHelperMap;

import java.util.List;
import java.util.stream.Stream;

/**
 * Расширение {@link JpaSpecificationExecutor} с поддержкой:
 * <ul>
 *   <li>EntityGraph для управления загрузкой ассоциаций;</li>
 *   <li>{@link Stream}-возврата с дефолтным {@code fetchSize} для PostgreSQL-курсоров;</li>
 *   <li>выборки уникальных значений по конкретному полю через {@link UniqueFieldHelperMap}.</li>
 * </ul>
 *
 * <p><b>Важно про Stream:</b> возвращаемый стрим привязан к JPA-курсору. Чтение должно происходить
 * в той же транзакции, в которой стрим открыт — иначе будет {@code LazyInitializationException}.
 * Caller обязан быть {@code @Transactional}; рекомендуется явно использовать
 * {@code @Transactional(propagation = Propagation.MANDATORY)} в методах, возвращающих Stream дальше.</p>
 */
@NoRepositoryBean
public interface CustomJpaSpecificationExecutor<T> extends JpaSpecificationExecutor<T> {

    /**
     * Пагинированная выборка с {@link EntityGraph}-загрузкой ассоциаций.
     */
    Page<T> findAll(@Nullable Specification<T> spec, Pageable pageable, @Nullable EntityGraph<T> entityGraph);

    /**
     * Полная выборка с {@link EntityGraph}-загрузкой (без пагинации).
     */
    List<T> findAll(@Nullable Specification<T> spec, @Nullable EntityGraph<T> entityGraph);

    /**
     * Возвращает {@link Stream} сущностей с курсорным чтением.
     * Сортировка берётся из {@code pageable}, параметры пагинации игнорируются.
     * Caller должен закрывать стрим (try-with-resources) и быть в активной транзакции.
     */
    Stream<T> streamAll(@Nullable Specification<T> spec, Pageable pageable, @Nullable EntityGraph<T> entityGraph);

    /**
     * Простой стрим без сортировки и graph.
     */
    Stream<T> streamAll(@Nullable Specification<T> spec);

    /**
     * Уникальные значения по полю, описанному в {@code helperMap}, с пагинацией.
     *
     * @param fieldName   имя поля (ключ в helperMap)
     * @param fieldSearch строка поиска (LIKE %x%) или null/пустая
     * @param offset      смещение (>=0)
     * @param limit       размер страницы (>0)
     */
    Page<T> findUniqueValues(@Nullable Specification<T> spec,
                             String fieldName,
                             @Nullable String fieldSearch,
                             int offset,
                             int limit,
                             UniqueFieldHelperMap<T> helperMap,
                             @Nullable EntityGraph<T> entityGraph);

    /**
     * Уникальные значения по полю без пагинации.
     */
    List<T> findUniqueValues(@Nullable Specification<T> spec,
                             String fieldName,
                             @Nullable String fieldSearch,
                             UniqueFieldHelperMap<T> helperMap);

    // ---- DTO-overload-ы (опциональные удобства) ----

    /**
     * Удобный overload: распаковывает {@link UniqueField} + {@link Pagination} в позиционные параметры.
     */
    default Page<T> findUniqueValues(@Nullable Specification<T> spec,
                                     UniqueField uniqueField,
                                     Pagination pagination,
                                     UniqueFieldHelperMap<T> helperMap,
                                     @Nullable EntityGraph<T> entityGraph) {
        if (uniqueField == null || uniqueField.isEmpty()) {
            throw new IllegalArgumentException("UniqueField.fieldName must be set");
        }
        Pagination p = pagination == null ? Pagination.first(Pagination.DEFAULT_LIMIT) : pagination;
        return findUniqueValues(spec, uniqueField.fieldName(), uniqueField.fieldSearch(),
                p.offset(), p.limit(), helperMap, entityGraph);
    }

    /**
     * Удобный overload: берёт {@code uniqueField} и {@code pagination} прямо из {@link ListRequest}.
     */
    default <F> Page<T> findUniqueValues(@Nullable Specification<T> spec,
                                         ListRequest<F> request,
                                         UniqueFieldHelperMap<T> helperMap,
                                         @Nullable EntityGraph<T> entityGraph) {
        if (request == null || !request.isUniqueFieldMode()) {
            throw new IllegalArgumentException("ListRequest must be in uniqueField mode");
        }
        return findUniqueValues(spec, request.uniqueField(), request.pagination(), helperMap, entityGraph);
    }
}
