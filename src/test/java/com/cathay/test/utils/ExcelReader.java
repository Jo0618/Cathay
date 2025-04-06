package com.cathay.test.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.DataProvider;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelReader {
    private static final String TEST_CASES_PATH = "cases/fine_grained_test_cases.xlsx";

    @DataProvider(name = "testData")
    public static Object[][] getTestData() {
        List<Object[]> testData = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(TEST_CASES_PATH);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getLastRowNum();
            
            // 跳過標題行，從第二行開始讀取
            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    String loanAmount = getStringCellValue(row.getCell(0));
                    String loanTerm = getStringCellValue(row.getCell(1));
                    String interestRate = getStringCellValue(row.getCell(2));
                    String expectedMonthlyPayment = getStringCellValue(row.getCell(3));
                    
                    testData.add(new Object[]{
                            loanAmount,
                            loanTerm,
                            interestRate,
                            expectedMonthlyPayment
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return testData.toArray(new Object[0][]);
    }
    
    private static String getStringCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
} 