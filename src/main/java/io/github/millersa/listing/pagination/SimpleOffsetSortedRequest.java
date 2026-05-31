package io.github.millersa.listing.pagination;

import io.github.millersa.listing.dto.Pagination;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Реализация {@link AbstractOffsetSortedRequest} с дефолтными значениями.
 * <p>
 * Параметры {@code page}/{@code limit} в этом классе означают именно <b>смещение и размер страницы</b>
 * (а не номер страницы) — используйте, когда API клиента передаёт offset напрямую.
 * Имя «Page» сохранено в конструкторах для обратной совместимости с старыми вызовами.
 */
public class SimpleOffsetSortedRequest extends AbstractOffsetSortedRequest {

    public SimpleOffsetSortedRequest(Integer limit, Integer offset) {
        super(limit, offset, Sort.unsorted());
    }

    public SimpleOffsetSortedRequest(Integer limit, Integer offset, Sort sort) {
        super(limit, offset, sort);
    }

    /**
     * Удобный конструктор из {@link Pagination}-DTO.
     * <p>Если {@code pagination == null}, используются дефолты {@link AbstractOffsetSortedRequest}
     * (limit=20, offset=0). Это сделано умышленно для controllers, где {@code @RequestBody}
     * может не содержать пагинацию.</p>
     */
    public SimpleOffsetSortedRequest(Pagination pagination) {
        super(pagination == null ? null : pagination.limit(),
              pagination == null ? null : pagination.offset(),
              Sort.unsorted());
    }

    /**
     * Удобный конструктор из {@link Pagination}-DTO + явной {@link Sort}.
     * Поведение {@code pagination == null} аналогично {@link #SimpleOffsetSortedRequest(Pagination)}.
     */
    public SimpleOffsetSortedRequest(Pagination pagination, Sort sort) {
        super(pagination == null ? null : pagination.limit(),
              pagination == null ? null : pagination.offset(),
              sort);
    }

    @Override
    protected AbstractOffsetSortedRequest getNewOffsetSortedRequest(Integer limit, Integer offset, Sort sort) {
        return new SimpleOffsetSortedRequest(limit, offset, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new SimpleOffsetSortedRequest(getPageSize(), pageNumber, getSort());
    }
}
