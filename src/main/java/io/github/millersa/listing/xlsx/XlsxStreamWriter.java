package io.github.millersa.listing.xlsx;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

/**
 * Потоковый писатель XLSX-файла поверх SXSSF.
 * Память ограничена окном строк ({@code windowSize}); остальное флашится в gzip-temp-файлы.
 * Не потокобезопасен: один писатель — один файл.
 *
 * <pre>{@code
 * try (XlsxStreamWriter<Dto> w = new XlsxStreamWriter<>("Sheet", COLUMNS)) {
 *     source.forEach(w::writeRow);
 *     w.writeTo(outputStream);
 * }
 * }</pre>
 */
public final class XlsxStreamWriter<T> implements Closeable {

    /** Дефолтный размер окна — 100 строк в памяти (рекомендованное значение SXSSF). */
    public static final int DEFAULT_WINDOW_SIZE = 100;

    private final SXSSFWorkbook workbook;
    private final SXSSFSheet sheet;
    private final List<XlsxColumn<T>> columns;
    private final CellStyle headerStyle;
    private final CellStyle commonStyle;
    private final CellStyle dateStyle;
    private int rowIndex = 1;

    public XlsxStreamWriter(String sheetName, List<XlsxColumn<T>> columns) {
        this(sheetName, columns, DEFAULT_WINDOW_SIZE);
    }

    /**
     * @param windowSize число строк, удерживаемых в памяти.
     *                   Большее значение быстрее, но требует больше heap;
     *                   меньшее снижает память за счёт более частого flush.
     */
    public XlsxStreamWriter(String sheetName, List<XlsxColumn<T>> columns, int windowSize) {
        if (windowSize < 1) {
            throw new IllegalArgumentException("windowSize must be >= 1, got: " + windowSize);
        }
        this.columns = List.copyOf(columns);
        this.workbook = new SXSSFWorkbook(windowSize);
        this.workbook.setCompressTempFiles(true);
        try {
            this.sheet = workbook.createSheet(sheetName);
            this.sheet.setRandomAccessWindowSize(windowSize);
            this.headerStyle = buildHeaderStyle(workbook);
            this.commonStyle = buildCommonStyle(workbook);
            this.dateStyle = buildDateStyle(workbook);
            writeHeader();
        } catch (RuntimeException e) {
            try {
                workbook.close();
            } catch (IOException ignore) {
                // suppressed
            }
            throw e;
        }
    }

    public void writeRow(T item) {
        Row row = sheet.createRow(rowIndex++);
        for (int i = 0; i < columns.size(); i++) {
            XlsxColumn<T> col = columns.get(i);
            Cell cell = row.createCell(i);
            Object value = col.extract(item);
            // Exhaustive switch — если добавится новый Kind, компилятор предупредит
            switch (col.kind()) {
                case STRING -> writeString(cell, value);
                case LONG -> writeLong(cell, value);
                case DATE -> writeDate(cell, value);
            }
        }
    }

    private void writeString(Cell cell, Object value) {
        if (value != null) {
            cell.setCellValue((String) value);
        }
        cell.setCellStyle(commonStyle);
    }

    private void writeLong(Cell cell, Object value) {
        if (value != null) {
            cell.setCellValue((Long) value);
        }
        cell.setCellStyle(commonStyle);
    }

    private void writeDate(Cell cell, Object value) {
        if (value != null) {
            cell.setCellValue(Date.from(((OffsetDateTime) value).toInstant()));
            cell.setCellStyle(dateStyle);
        } else {
            cell.setCellStyle(commonStyle);
        }
    }

    public void writeTo(OutputStream out) throws IOException {
        workbook.write(out);
        out.flush();
    }

    @Override
    public void close() throws IOException {
        workbook.close();
    }

    private void writeHeader() {
        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.size(); i++) {
            XlsxColumn<T> col = columns.get(i);
            Cell cell = header.createCell(i);
            cell.setCellValue(col.header());
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, col.width());
        }
    }

    private static CellStyle buildHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFColor headerColor = new XSSFColor(new byte[]{(byte) 209, (byte) 218, (byte) 239}, null);
        style.setFillForegroundColor(headerColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.INDIGO.getIndex());
        font.setFontName("Calibri");
        style.setFont(font);
        return style;
    }

    private static CellStyle buildCommonStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        return style;
    }

    private static CellStyle buildDateStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper helper = workbook.getCreationHelper();
        style.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        return style;
    }
}
