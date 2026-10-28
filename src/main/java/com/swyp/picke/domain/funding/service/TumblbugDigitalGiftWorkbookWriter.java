package com.swyp.picke.domain.funding.service;

import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.TreeMap;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** 텀블벅 디지털 선물의 후원번호-텍스트 업로드 시트 형식으로 코드를 내보냅니다. */
@Component
public class TumblbugDigitalGiftWorkbookWriter {
    private static final int MAX_TEXT_LENGTH = 300;

    public byte[] write(Map<String, String> codeTextBySupporter) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("디지털 선물");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("후원번호");
            header.createCell(1).setCellValue("전달할 텍스트");
            int rowNumber = 1;
            for (Map.Entry<String, String> entry : new TreeMap<>(codeTextBySupporter).entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getValue() == null || entry.getValue().length() > MAX_TEXT_LENGTH) {
                    throw new CustomException(ErrorCode.FUNDING_TUMBLBUG_IMPORT_INVALID);
                }
                Row row = sheet.createRow(rowNumber++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            }
            sheet.setColumnWidth(0, 18 * 256);
            sheet.setColumnWidth(1, 48 * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
