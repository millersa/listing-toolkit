package io.github.millersa.listing.pagination;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;

/**
 * Базовый класс {@link Pageable}, использующий пару (offset, limit) вместо номера страницы.
 * Подходит, когда клиент API передаёт смещение, а не страницу.
 */
public abstract class AbstractOffsetSortedRequest implements Pageable {

    private static final int DEFAULT_LIMIT = 20;

    private final Integer limit;
    private final Integer offset;
    private final Sort sort;

    protected AbstractOffsetSortedRequest(Integer limit, Integer offset, Sort sort) {
        this.limit = defaultLimitIfNull(limit);
        this.offset = defaultOffsetIfNull(offset);
        this.sort = sort == null ? Sort.unsorted() : sort;
    }

    private static int defaultLimitIfNull(Integer limit) {
        return (Objects.isNull(limit) || limit < 1) ? DEFAULT_LIMIT : limit;
    }

    private static int defaultOffsetIfNull(Integer offset) {
        return (Objects.isNull(offset) || offset < 0) ? 0 : offset;
    }

    @Override
    public int getPageNumber() {
        return offset / limit;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public boolean hasPrevious() {
        return offset >= limit;
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }

    private Pageable previous() {
        return getNewOffsetSortedRequest(getPageSize(), Math.toIntExact(getOffset() - getPageSize()), sort);
    }

    @Override
    public Pageable first() {
        return getNewOffsetSortedRequest(getPageSize(), 0, sort);
    }

    @Override
    public Pageable next() {
        return getNewOffsetSortedRequest(getPageSize(), Math.toIntExact(getOffset() + getPageSize()), sort);
    }

    protected abstract AbstractOffsetSortedRequest getNewOffsetSortedRequest(Integer limit, Integer offset, Sort sort);
}
