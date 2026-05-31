package io.github.millersa.listing.xlsx;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class XlsxColumnTest {

    record Dto(String name, Long id, OffsetDateTime createdAt) {}

    @Test
    void string_kindIsString() {
        XlsxColumn<Dto> col = XlsxColumn.string("Имя", Dto::name);
        assertThat(col.kind()).isEqualTo(XlsxColumn.Kind.STRING);
        assertThat(col.header()).isEqualTo("Имя");
        assertThat(col.extract(new Dto("alice", 1L, null))).isEqualTo("alice");
    }

    @Test
    void longCol_kindIsLong() {
        XlsxColumn<Dto> col = XlsxColumn.longCol("ID", Dto::id);
        assertThat(col.kind()).isEqualTo(XlsxColumn.Kind.LONG);
        assertThat(col.extract(new Dto("alice", 42L, null))).isEqualTo(42L);
    }

    @Test
    void date_kindIsDate() {
        XlsxColumn<Dto> col = XlsxColumn.date("Создан", Dto::createdAt);
        assertThat(col.kind()).isEqualTo(XlsxColumn.Kind.DATE);
        OffsetDateTime now = OffsetDateTime.now();
        assertThat(col.extract(new Dto("alice", 1L, now))).isEqualTo(now);
    }

    @Test
    void width_defaultIs5555() {
        XlsxColumn<Dto> col = XlsxColumn.string("X", Dto::name);
        assertThat(col.width()).isEqualTo(5555);
    }

    @Test
    void width_canBeOverridden() {
        XlsxColumn<Dto> col = XlsxColumn.string("X", Dto::name).width(8000);
        assertThat(col.width()).isEqualTo(8000);
    }

    @Test
    void width_isImmutable() {
        XlsxColumn<Dto> original = XlsxColumn.string("X", Dto::name);
        XlsxColumn<Dto> wider = original.width(9999);
        assertThat(original.width()).isEqualTo(5555);  // не изменился
        assertThat(wider.width()).isEqualTo(9999);
    }

    @Test
    void extract_nullField_returnsNull() {
        XlsxColumn<Dto> col = XlsxColumn.string("Имя", Dto::name);
        assertThat(col.extract(new Dto(null, 1L, null))).isNull();
    }
}
