package io.github.millersa.listing.uniquefield;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Пара select-where для получения уникальных значений по полю сущности.
 */
@Getter
public class QueryPair {

    @NonNull
    private final List<Selection<?>> selections;

    @Nullable
    private final Predicate predicate;

    public QueryPair(@NonNull List<Selection<?>> selections, @Nullable Predicate predicate) {
        this.selections = selections;
        this.predicate = predicate;
    }

    public QueryPair(@NonNull Selection<?> selection, @Nullable Predicate predicate) {
        this.selections = Collections.singletonList(selection);
        this.predicate = predicate;
    }
}
