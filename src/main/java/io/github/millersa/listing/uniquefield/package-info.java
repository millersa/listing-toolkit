/**
 * Инфраструктура для выборки уникальных значений по полю сущности с фильтром поиска
 * (типичный сценарий — autocomplete на UI-фильтре).
 *
 * <p>Регистрация полей через {@link io.github.millersa.listing.uniquefield.UniqueFieldHelperMap}.
 * Каждое поле описывается {@link io.github.millersa.listing.uniquefield.SelectAndPredicateCreator}
 * (как построить запрос) и transformer-функцией ({@code Function<Tuple, Entity>}, как разобрать результат).</p>
 */
package io.github.millersa.listing.uniquefield;
