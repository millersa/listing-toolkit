/**
 * Опциональные DTO-records для контракта REST endpoint-ов: {@link io.github.millersa.listing.dto.Pagination},
 * {@link io.github.millersa.listing.dto.Sorting}, {@link io.github.millersa.listing.dto.UniqueField},
 * {@link io.github.millersa.listing.dto.ListRequest}, {@link io.github.millersa.listing.dto.ListResponse}.
 *
 * <p>Можно не использовать — все основные API в {@code pagination} и {@code uniquefield} принимают
 * сырые {@code int}/{@code String}/{@code List}. DTO-overload-ы добавлены как удобный fast-path.</p>
 */
package io.github.millersa.listing.dto;
