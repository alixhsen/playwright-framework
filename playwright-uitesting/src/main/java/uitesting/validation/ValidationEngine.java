package uitesting.validation;

import com.microsoft.playwright.*;
import uitesting.utils.DatabaseHelper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Validates test results against database or file system expectations.
 */
public class ValidationEngine {

    private static final Logger logger = Logger.getLogger(ValidationEngine.class.getName());
    private static final int NUMERIC_PRECISION = 4;

    private final Page page;
    private final DatabaseHelper db;

    public ValidationEngine(Page page) {
        this.page = page;
        this.db   = new DatabaseHelper();
    }

    public ValidationEngine(Page page, String connectionString) {
        this.page = page;
        this.db   = new DatabaseHelper();
    }

    /**
     * Validates a step based on the validation flag type.
     *
     * @param connectionString  JDBC connection string (may be overridden per TC)
     * @param validationQuery   SQL query or filename pattern
     * @param validationFlag    Type of validation: "Implementation Validation", "File Exported", "Search Validation"
     */
    public boolean validate(String connectionString, String validationQuery, String validationFlag) {
        if (validationFlag == null || validationFlag.isBlank()) return false;

        DatabaseHelper targetDb = (connectionString != null && !connectionString.isBlank())
                ? new DatabaseHelper()
                : this.db;

        if (validationFlag.contains("Implementation Validation")) {
            return validateImplementation(targetDb, validationQuery);
        } else if (validationFlag.contains("File Exported")) {
            return validateFileExported(validationQuery);
        } else if (validationFlag.contains("Search Validation")) {
            return validateSearchResults(targetDb, validationQuery);
        }

        return false;
    }

    // ─── Validation Types ─────────────────────────────────────────────────────

    /**
     * Checks that a query returns exactly 1 row.
     */
    private boolean validateImplementation(DatabaseHelper db, String query) {
        try {
            List<String[]> rows = db.executeQuery(query);
            return rows.size() == 1;
        } catch (Exception e) {
            logger.warning("Implementation validation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks that a downloaded file matching the pattern exists and is > 6KB.
     */
    private boolean validateFileExported(String fileNamePattern) {
        try {
            String downloadPath = System.getProperty("user.home") + File.separator + "Downloads";
            String todayPattern = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd

            File downloadDir = new File(downloadPath);
            File[] matches = downloadDir.listFiles((dir, name) ->
                    name.contains(fileNamePattern) && name.contains(todayPattern));

            if (matches != null && matches.length > 0) {
                long size = Files.size(Paths.get(matches[0].getAbsolutePath()));
                boolean valid = size > 6 * 1024;
                logger.info("File validation: " + matches[0].getName() + " size=" + size + " valid=" + valid);
                return valid;
            }
        } catch (Exception e) {
            logger.warning("File validation failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Compares UI grid values against database query results.
     */
    private boolean validateSearchResults(DatabaseHelper db, String query) {
        try {
            // Get values from the UI grid
            List<String> uiValues = extractGridValues();

            // Get values from the database
            List<String> dbValues = db.executeQueryFlat(query);

            if (uiValues.isEmpty() || dbValues.isEmpty()) {
                logger.warning("Validation: empty data - UI=" + uiValues.size() + " DB=" + dbValues.size());
                return false;
            }

            int count = Math.min(uiValues.size(), dbValues.size());
            for (int i = 0; i < count; i++) {
                String uiVal   = uiValues.get(i).replaceAll(",", "");
                String dbVal   = dbValues.get(i);

                if (uiVal.equals(dbVal)) continue;

                // Try numeric comparison with tolerance
                try {
                    BigDecimal ui = new BigDecimal(uiVal).setScale(NUMERIC_PRECISION, RoundingMode.HALF_UP);
                    BigDecimal dv = new BigDecimal(dbVal).setScale(NUMERIC_PRECISION, RoundingMode.HALF_UP);
                    if (ui.compareTo(dv) != 0) {
                        logger.info("Validation mismatch at index " + i + ": UI='" + uiVal + "' DB='" + dbVal + "'");
                        return false;
                    }
                } catch (NumberFormatException nfe) {
                    // String comparison (case-insensitive)
                    if (!uiVal.equalsIgnoreCase(dbVal)) {
                        logger.info("Validation mismatch at index " + i + ": UI='" + uiVal + "' DB='" + dbVal + "'");
                        return false;
                    }
                }
            }
            return true;

        } catch (Exception e) {
            logger.warning("Search validation failed: " + e.getMessage());
            return false;
        }
    }

    // ─── UI Extraction ────────────────────────────────────────────────────────

    /**
     * Extracts all text values from the visible data grid rows.
     */
    private List<String> extractGridValues() {
        List<String> values = new ArrayList<>();
        try {
            List<Locator> rows = page.locator("div.grid-base-row").all();
            for (Locator row : rows) {
                List<Locator> spans = row.locator("span").all();
                for (Locator span : spans) {
                    String text = span.innerText().trim();
                    values.add(text);
                }
            }
        } catch (Exception e) {
            logger.warning("Grid extraction failed: " + e.getMessage());
        }
        return values;
    }
}
