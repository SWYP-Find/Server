package com.swyp.picke.domain.funding.service;

import com.swyp.picke.domain.funding.dto.response.TumblbugWorkbookInspectResponse;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** 텀블벅 XLSX 열 제목을 자동 판별하고 후원자 표 값을 읽습니다. */
@Component
public class TumblbugSupporterExcelProcessor {
    private static final int MAX_DATA_ROWS = 5_000;
    private static final int MAX_HEADER_SCAN_ROWS = 50;
    private static final int MAX_INSPECT_COLUMNS = 40;

    private enum Column {
        SUPPORTER_NUMBER("후원번호", true, "후원번호", "후원자번호", "후원자고유번호", "backernumber"),
        BUYER_NAME("이름", true, "이름", "후원자명", "후원자이름", "닉네임", "name"),
        GIFT("선물", true, "선물", "선물이름", "선물구성", "리워드", "리워드명", "선택한선물", "gift"),
        ADDITIONAL_SUPPORT("추가 후원금", true, "추가후원금", "추가후원", "추가후원금액", "추가후원액", "additionalfunding"),
        TOTAL_SUPPORT("총 후원 금액", true, "총후원금액", "총후원금", "후원금액", "총결제금액", "totalfunding"),
        SUPPORTED_AT("후원 일시", true, "후원일시", "후원일자", "후원날짜", "결제일시", "결제일", "backedat"),
        NOTIFICATION_REQUEST("알림 신청", true, "알림신청", "알림신청여부", "후원자알림신청", "notification"),
        PAYMENT_STATUS("결제 상태", true, "결제상태", "결제여부", "paymentstatus"),
        GIFT_DELIVERY_STATUS("선물 전달", true, "선물전달", "선물전달상태", "전달상태", "리워드전달상태", "giftstatus");

        private final String label;
        private final boolean required;
        private final List<String> aliases;

        Column(String label, boolean required, String... aliases) {
            this.label = label;
            this.required = required;
            this.aliases = List.of(aliases);
        }

        private boolean matches(String header) {
            String normalized = normalize(header);
            return aliases.stream().map(TumblbugSupporterExcelProcessor::normalize).anyMatch(normalized::equals);
        }
    }

    public TumblbugWorkbookInspectResponse inspect(byte[] file) {
        try (Workbook workbook = openXlsx(file)) {
            DataFormatter formatter = new DataFormatter(Locale.KOREA);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<TumblbugWorkbookInspectResponse.SheetSample> samples = new ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                HeaderDetection header = detectHeader(sheet, formatter, evaluator);
                List<List<String>> sampleRows = new ArrayList<>();
                int maxColumns = 0;
                if (header != null) {
                    Row headerRow = sheet.getRow(header.rowIndex());
                    maxColumns = Math.min(Math.max(headerRow.getLastCellNum(), 0), MAX_INSPECT_COLUMNS);
                    for (int rowIndex = header.rowIndex(); rowIndex <= sheet.getLastRowNum() && sampleRows.size() < 8; rowIndex++) {
                        Row row = sheet.getRow(rowIndex);
                        if (row == null || isBlank(row, formatter, evaluator)) continue;
                        List<String> values = new ArrayList<>(maxColumns);
                        for (int columnIndex = 0; columnIndex < maxColumns; columnIndex++) {
                            values.add(value(row, columnIndex, formatter, evaluator));
                        }
                        sampleRows.add(List.copyOf(values));
                    }
                }
                samples.add(new TumblbugWorkbookInspectResponse.SheetSample(
                        sheet.getSheetName(), sheet.getLastRowNum() + 1,
                        header == null ? null : header.rowIndex() + 1,
                        header == null ? Map.of() : header.detectedColumns(),
                        header == null ? requiredColumnLabels() : missingColumnLabels(header.columns()),
                        List.copyOf(sampleRows)));
            }
            return new TumblbugWorkbookInspectResponse(List.copyOf(samples));
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
        }
    }

    public List<SupporterRow> read(byte[] file, String sheetName) {
        try (Workbook workbook = openXlsx(file)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);

            DataFormatter formatter = new DataFormatter(Locale.KOREA);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            HeaderDetection header = detectHeader(sheet, formatter, evaluator);
            if (header == null || header.columns().size() < requiredColumnCount()) {
                throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_COLUMNS_MISSING);
            }
            if (sheet.getLastRowNum() - header.rowIndex() > MAX_DATA_ROWS + MAX_HEADER_SCAN_ROWS) {
                throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_TOO_LARGE);
            }

            List<SupporterRow> rows = new ArrayList<>();
            for (int index = header.rowIndex() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || isBlank(row, formatter, evaluator)) continue;
                if (rows.size() >= MAX_DATA_ROWS) throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_TOO_LARGE);

                Map<String, String> values = new LinkedHashMap<>();
                for (Column column : Column.values()) {
                    values.put(column.label, value(row, header.columns().get(column), formatter, evaluator));
                }
                rows.add(new SupporterRow(index + 1,
                        values.get(Column.SUPPORTER_NUMBER.label),
                        values.get(Column.BUYER_NAME.label),
                        values.get(Column.GIFT.label),
                        values.get(Column.ADDITIONAL_SUPPORT.label),
                        values.get(Column.TOTAL_SUPPORT.label),
                        values.get(Column.SUPPORTED_AT.label),
                        values.get(Column.NOTIFICATION_REQUEST.label),
                        values.get(Column.PAYMENT_STATUS.label),
                        values.get(Column.GIFT_DELIVERY_STATUS.label),
                        Map.copyOf(values)));
            }
            return rows;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
        }
    }

    private Workbook openXlsx(byte[] file) throws Exception {
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file));
        if (!(workbook instanceof XSSFWorkbook)) {
            workbook.close();
            throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
        }
        return workbook;
    }

    private HeaderDetection detectHeader(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        HeaderDetection best = null;
        int start = Math.max(sheet.getFirstRowNum(), 0);
        int end = Math.min(sheet.getLastRowNum(), start + MAX_HEADER_SCAN_ROWS - 1);
        for (int rowIndex = start; rowIndex <= end; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            EnumMap<Column, Integer> columns = new EnumMap<>(Column.class);
            for (Cell cell : row) {
                String header = value(cell, formatter, evaluator);
                for (Column column : Column.values()) {
                    if (!columns.containsKey(column) && column.matches(header)) {
                        columns.put(column, cell.getColumnIndex());
                    }
                }
            }
            if (best == null || columns.size() > best.columns().size()) {
                best = new HeaderDetection(rowIndex, columns, detectedColumnNames(row, columns, formatter, evaluator));
            }
            if (columns.size() == Column.values().length) break;
        }
        return best;
    }

    private Map<String, String> detectedColumnNames(
            Row row, Map<Column, Integer> columns, DataFormatter formatter, FormulaEvaluator evaluator) {
        Map<String, String> result = new LinkedHashMap<>();
        columns.forEach((column, index) -> result.put(column.label, value(row, index, formatter, evaluator)));
        return Map.copyOf(result);
    }

    private List<String> missingColumnLabels(Map<Column, Integer> columns) {
        return java.util.Arrays.stream(Column.values())
                .filter(column -> column.required)
                .filter(column -> !columns.containsKey(column))
                .map(column -> column.label)
                .toList();
    }

    private List<String> requiredColumnLabels() {
        return java.util.Arrays.stream(Column.values()).filter(column -> column.required).map(column -> column.label).toList();
    }

    private int requiredColumnCount() {
        return (int) java.util.Arrays.stream(Column.values()).filter(column -> column.required).count();
    }

    private boolean isBlank(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (Cell cell : row) {
            if (!value(cell, formatter, evaluator).isBlank()) return false;
        }
        return true;
    }

    private String value(Row row, Integer index, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (index == null) return "";
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : value(cell, formatter, evaluator);
    }

    private String value(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().withSecond(0).withNano(0).toString();
        }
        return (evaluator == null ? formatter.formatCellValue(cell) : formatter.formatCellValue(cell, evaluator)).trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s()（）:：/·_\\-]", "");
    }

    private record HeaderDetection(int rowIndex, Map<Column, Integer> columns, Map<String, String> detectedColumns) { }

    public record SupporterRow(
            int rowNumber,
            String supporterNumber,
            String buyerName,
            String gift,
            String additionalSupportAmount,
            String totalSupportAmount,
            String supportedAt,
            String notificationRequest,
            String paymentStatus,
            String giftDeliveryStatus,
            Map<String, String> sourceColumns
    ) { }
}
