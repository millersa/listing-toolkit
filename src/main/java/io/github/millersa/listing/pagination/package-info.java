/**
 * Stream-aware расширение Spring Data JPA для пагинации и фильтрации через {@link org.springframework.data.jpa.domain.Specification}.
 *
 * <p>Главная точка входа — {@link io.github.millersa.listing.pagination.CustomJpaSpecificationExecutor},
 * который добавляет поддержку EntityGraph, {@link java.util.stream.Stream}-чтения и выборки уникальных значений
 * к стандартному {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor}.</p>
 */
package io.github.millersa.listing.pagination;
