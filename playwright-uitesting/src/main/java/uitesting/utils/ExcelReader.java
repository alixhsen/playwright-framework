package uitesting.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads test case data from Excel files using Apache POI.
 */
public class ExcelReader {

    private static final Logger logger = Logger.getLogger(ExcelReader.class.getName());

    // Column indices (0-based) — match your Excel layout
    public static final int COL_TC_ID               = 0;  // A
    public static final int COL_TC_TITLE             = 4;  // E
    public static final int COL_TC_DESCRIPTION       = 5;  // F
    public static final int COL_TEST_STEPS           = 6;  // G
    public static final int COL_VALIDATION_FLAG      = 15; // P
    public static final int COL_CONNECTION_STRING    = 16; // Q
    public static final int COL_VALIDATION_STEPS     = 17; // R
    public static final int COL_EXECUTED_QUERY       = 18; // S

    /**
     * Represents one row (test case) from the Excel sheet.
     */
    public static class TestCaseRow {
        public String tcId;
        public String title;
        public String description;
        public String testSteps;
        public String validationFlag;
        public String connectionString;
        public String validationQuery;
        public String executedQuery;

        public String[] getStepsArray() {
            if (testSteps == null || testSteps.isBlank()) return new String[0];
            return testSteps.split("\n");
        }
    }

    /**
     * Reads all test case rows from the first sheet of the given Excel file.
     */
    public List<TestCaseRow> readTestCases(String excelFilePath) {
        List<TestCaseRow> rows = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                TestCaseRow tc = new TestCaseRow();
                tc.tcId            = getCellValue(row, COL_TC_ID);
                tc.title           = getCellValue(row, COL_TC_TITLE);
                tc.description     = getCellValue(row, COL_TC_DESCRIPTION);
                tc.testSteps       = getCellValue(row, COL_TEST_STEPS);
                tc.validationFlag  = getCellValue(row, COL_VALIDATION_FLAG);
                tc.connectionString= getCellValue(row, COL_CONNECTION_STRING);
                tc.validationQuery = getCellValue(row, COL_VALIDATION_STEPS);
                tc.executedQuery   = getCellValue(row, COL_EXECUTED_QUERY);

                if (tc.tcId != null && !tc.tcId.isBlank()) {
                    rows.add(tc);
                }
            }

            logger.info("Read " + rows.size() + " test cases from: " + excelFilePath);

        } catch (Exception e) {
            logger.severe("Failed to read Excel file: " + e.getMessage());
        }

        return rows;
    }

    /**
     * Writes validation results back to an Excel file.
     */
    public void writeResults(String outputPath, List<String[]> results) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("ValidationResults");

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("TC ID");
            header.createCell(1).setCellValue("Validation Result");

            // Data rows
            for (int i = 0; i < results.size(); i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(results.get(i)[0]);
                row.createCell(1).setCellValue(results.get(i)[1]);
            }

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outputPath)) {
                workbook.write(fos);
            }

            logger.info("Results written to: " + outputPath);

        } catch (Exception e) {
            logger.severe("Failed to write results: " + e.getMessage());
        }
    }

    private String getCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";

        CellType type = cell.getCellType();
        if (type == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (type == CellType.NUMERIC) {
            return DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
        } else if (type == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (type == CellType.FORMULA) {
            return cell.getCachedFormulaResultType() == CellType.STRING
                    ? cell.getStringCellValue()
                    : String.valueOf(cell.getNumericCellValue());
        } else {
            return "";
        }
    }
}
