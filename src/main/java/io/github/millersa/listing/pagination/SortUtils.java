package io.github.millersa.listing.pagination;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static org.springframework.data.domain.Sort.Direction.ASC;

public final class SortUtils {

    private SortUtils() {
    }

    /**
     * Парсинг условий сортировки вида {@code "asc(fieldName)"}, {@code "desc(fieldName)"}.
     *
     * @throws IllegalArgumentException если строка не соответствует формату или direction некорректен
     */
    public static List<Pair<String, Sort.Direction>> parsedSort(List<String> sortBy) {
        List<Pair<String, Sort.Direction>> result = new ArrayList<>();
        if (Objects.isNull(sortBy) || sortBy.isEmpty()) {
            return result;
        }
        for (String str : sortBy) {
            String[] parts = str.split("[()]");
            if (parts.length < 2 || parts[1].isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid sort expression '" + str + "': expected 'asc(field)' or 'desc(field)'");
            }
            Sort.Direction direction;
            try {
                direction = Sort.Direction.fromString(parts[0]);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid sort direction '" + parts[0] + "' in '" + str + "': expected 'asc' or 'desc'", e);
            }
            String fieldName = parts[1];
            result.add(new ImmutablePair<>(fieldName, direction));
        }
        return result;
    }

    /**
     * Конвертация условий сортировки в список {@link Order} с маппером "имя поля → один {@link Expression}".
     */
    public static List<Order> covertToCriteriaOrders(List<String> sortBy, CriteriaBuilder cb,
                                                     Function<String, Expression<?>> databaseFieldMapper) {
        List<Order> result = new ArrayList<>();
        parsedSort(sortBy).forEach(pair -> {
            Expression<?> orderField = databaseFieldMapper.apply(pair.getKey());
            if (pair.getValue().equals(ASC)) {
                result.add(cb.asc(orderField));
            } else {
                result.add(cb.desc(orderField));
            }
        });
        return result;
    }

    /**
     * Вариант для маппера, возвращающего несколько выражений на одно поле сортировки.
     */
    public static <T extends Expression<?>> List<Order> covertToCriteriaOrdersFromList(
            List<String> sortBy, CriteriaBuilder cb, Function<String, List<T>> databaseFieldMapper) {
        List<Order> result = new ArrayList<>();
        parsedSort(sortBy).forEach(pair -> {
            List<T> orderFields = databaseFieldMapper.apply(pair.getKey());
            Function<T, Order> orderByClause = pair.getValue().equals(ASC) ? cb::asc : cb::desc;
            orderFields.stream().map(orderByClause).forEach(result::add);
        });
        return result;
    }
}
