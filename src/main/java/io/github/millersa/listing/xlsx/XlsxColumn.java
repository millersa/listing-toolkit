package io.github.millersa.listing.xlsx;

import java.time.OffsetDateTime;
import java.util.function.Function;

/**
 * Декларативное описание одной колонки в XLSX-выгрузке: заголовок, тип ячейки и экстрактор значения из DTO.
 */
public final class XlsxColumn<T> {

    public enum Kind {STRING, LONG, DATE}

    private static final int DEFAULT_WIDTH = 5555;

    private final String header;
    private final Kind kind;
    private final Function<T, ?> extractor;
    private final int width;

    private XlsxColumn(String header, Kind kind, Function<T, ?> extractor, int width) {
        this.header = header;
        this.kind = kind;
        this.extractor = extractor;
        this.width = width;
    }

    public static <T> XlsxColumn<T> string(String header, Function<T, String> extractor) {
        return new XlsxColumn<>(header, Kind.STRING, extractor, DEFAULT_WIDTH);
    }

    public static <T> XlsxColumn<T> longCol(String header, Function<T, Long> extractor) {
        return new XlsxColumn<>(header, Kind.LONG, extractor, DEFAULT_WIDTH);
    }

    public static <T> XlsxColumn<T> date(String header, Function<T, OffsetDateTime> extractor) {
        return new XlsxColumn<>(header, Kind.DATE, extractor, DEFAULT_WIDTH);
    }

    public XlsxColumn<T> width(int width) {
        return new XlsxColumn<>(header, kind, extractor, width);
    }

    public String header() {
        return header;
    }

    public Kind kind() {
        return kind;
    }

    public Object extract(T item) {
        return extractor.apply(item);
    }

    public int width() {
        return width;
    }
}
