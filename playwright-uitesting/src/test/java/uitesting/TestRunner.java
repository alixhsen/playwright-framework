package uitesting;

import com.microsoft.playwright.Page;
import org.testng.annotations.*;
import uitesting.core.BrowserFactory;
import uitesting.core.TestStepProcessor;
import uitesting.utils.ConfigManager;
import uitesting.utils.DatabaseHelper;
import uitesting.utils.ExcelReader;
import uitesting.utils.ExcelReader.TestCaseRow;
import uitesting.utils.ScreenshotHelper;
import uitesting.validation.ValidationEngine;
import uitesting.utils.WordReportGenerator;
import java.time.LocalDateTime;

import uitesting.utils.PerformanceReportGenerator;
import uitesting.utils.PerformanceReportGenerator.TestCaseTiming;
import uitesting.utils.PerformanceReportGenerator.StepTiming;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Main TestNG test runner.
 * Reads test cases from Excel, executes each step, validates, and writes results.
 * Run with:
 *   mvn test
 * Or directly via testng.xml
 */
@Test
public class TestRunner {

    private static final Logger logger = Logger.getLogger(TestRunner.class.getName());

    private BrowserFactory browserFactory;
    private Page page;
    private ConfigManager config;
    private DatabaseHelper db;
    private ScreenshotHelper screenshotHelper;

    List<String> stepTitles = new ArrayList<>();
    List<String> screenshotPaths = new ArrayList<>();


    @BeforeSuite
    public void globalSetup() {
        config = ConfigManager.getInstance();
        db     = new DatabaseHelper();
        db.clearValidationTable();
        logger.info("Global setup complete.");
    }

    @BeforeMethod
    public void setUp() {
        browserFactory  = new BrowserFactory();
        page            = browserFactory.initBrowser();
        screenshotHelper = new ScreenshotHelper(page);
        logger.info("Browser started.");
    }

    @AfterMethod
    public void tearDown() {
        if (browserFactory != null) {
            browserFactory.closeBrowser();
        }
        logger.info("Browser closed.");
    }

    // ─── Main Test Method ──────────────────────────────────────────────────────

    /**
     * Reads all test cases from Excel, executes steps, captures screenshots,
     */
    @Test
    public void runAllTestCases() {

        LocalDateTime suiteStartTime = LocalDateTime.now();

        String excelFile      = config.getExcelFile();
        String validationFile = config.getValidationFile();
        String loginUrl       = config.getBaseUrl();
        String loginEmail     = config.getLoginEmail();
        String loginPassword  = config.getLoginPassword();

        ExcelReader reader = new ExcelReader();
        List<TestCaseRow> testCases = reader.readTestCases(excelFile);

        if (testCases.isEmpty()) {
            logger.warning("No test cases found in: " + excelFile);
            return;
        }

        List<String[]> results = new ArrayList<>();
        List<TestCaseTiming> performanceData = new ArrayList<>();

        TestStepProcessor processor = new TestStepProcessor(page, loginUrl, loginEmail, loginPassword);
       // ValidationEngine validator  = new ValidationEngine(page);

        for (TestCaseRow tc : testCases) {
            logger.info("===== Running TC: " + tc.tcId + " - " + tc.title + " =====");

            stepTitles.clear();
            screenshotPaths.clear();

            LocalDateTime tcStartTime = LocalDateTime.now();
            long tcStartMillis = System.currentTimeMillis();

            boolean tcPassed = true;
            List<StepTiming> stepTimings = new ArrayList<>();

            processor.setExecutedQuery(tc.executedQuery);

            String[] steps = tc.getStepsArray();
            for (String step : steps) {
                step = step.trim();
                if (step.isEmpty()) continue;

                long stepStart = System.currentTimeMillis();
                boolean stepPassed = true;

                try {
                    processor.process(step);

                    uitesting.core.LoadingPageHandler loader =
                            new uitesting.core.LoadingPageHandler(page);

                    loader.waitForPageLoad();
                    loader.waitForNetworkIdle();
                    page.waitForTimeout(500);

                } catch (Exception e) {
                    stepPassed = false;
                    tcPassed = false;
                    logger.severe("Step failed: " + step + " | " + e.getMessage());
                }

                long stepDuration = System.currentTimeMillis() - stepStart;

                stepTimings.add(new StepTiming(step, stepDuration, stepPassed));

                String screenshotPath = screenshotHelper.capture(step);
                stepTitles.add(step);
                screenshotPaths.add(screenshotPath);
            }

            long tcDuration = System.currentTimeMillis() - tcStartMillis;
            LocalDateTime tcEndTime = LocalDateTime.now();

            performanceData.add(new TestCaseTiming(
                    tc.tcId,
                    tc.title,
                    tcStartTime,
                    tcEndTime,
                    tcDuration,
                    tcPassed,
                    stepTimings
            ));


            WordReportGenerator reportGenerator =
                    new WordReportGenerator("C:/Framework/WordReports");

            reportGenerator.generateReport(
                    tc.tcId,
                    tc.title,
                    stepTitles,
                    screenshotPaths
            );

            LocalDateTime suiteEndTime = LocalDateTime.now();

            // Generate Performance Report
            PerformanceReportGenerator performanceReport =
                    new PerformanceReportGenerator("C:/Framework/PerformanceReports");

            performanceReport.generateReport(
                    suiteStartTime,
                    suiteEndTime,
                    performanceData,
                    tc.tcId
            );
            // Validate after all steps
           /* boolean isValid = validator.validate(
                tc.connectionString,
                tc.validationQuery,
                tc.validationFlag
            );*/

            //logger.info("TC " + tc.tcId + " result: " + (isValid ? "PASS" : "FAIL"));

            // Store in DB
           // db.insertValidationResult(tc.tcId, isValid, Timestamp.from(Instant.now()));

            // Collect for Excel output
           // results.add(new String[]{tc.tcId, isValid ? "Valid" : "Invalid"});
        }

        // Write results Excel
        reader.writeResults(validationFile, results);
        logger.info("Results written to: " + validationFile);



    }

    // ─── Single TC execution ───────────────────────────

    @Test(enabled = true)
    public void runSingleTestCase() {

        LocalDateTime suiteStartTime = LocalDateTime.now();

        String targetTcId   = "TC002"; // <-- change as needed
        String excelFile    = config.getExcelFile();
        String loginUrl     = config.getBaseUrl();
        String loginEmail   = config.getLoginEmail();
        String loginPassword= config.getLoginPassword();

        ExcelReader reader = new ExcelReader();
        List<TestCaseRow> testCases = reader.readTestCases(excelFile);

        TestCaseRow tc = testCases.stream()
                .filter(t -> targetTcId.equals(t.tcId))
                .findFirst()
                .orElse(null);

        if (tc == null) {
            logger.warning("TC not found: " + targetTcId);
            return;
        }

        logger.info("Running single TC: " + tc.tcId + " - " + tc.title);

        stepTitles.clear();
        screenshotPaths.clear();

        TestStepProcessor processor =
                new TestStepProcessor(page, loginUrl, loginEmail, loginPassword);

        processor.setExecutedQuery(tc.executedQuery);

        // ───── Performance Tracking ─────
        LocalDateTime tcStartTime = LocalDateTime.now();
        long tcStartMillis = System.currentTimeMillis();

        boolean tcPassed = true;
        List<StepTiming> stepTimings = new ArrayList<>();

        for (String step : tc.getStepsArray()) {

            step = step.trim();
            if (step.isEmpty()) continue;

            long stepStart = System.currentTimeMillis();
            boolean stepPassed = true;

            try {
                processor.process(step);

                uitesting.core.LoadingPageHandler loader =
                        new uitesting.core.LoadingPageHandler(page);

                loader.waitForPageLoad();
                loader.waitForNetworkIdle();
                page.waitForTimeout(500);

            } catch (Exception e) {
                stepPassed = false;
                tcPassed = false;
                logger.severe("Step failed: " + step + " | " + e.getMessage());
            }

            long stepDuration = System.currentTimeMillis() - stepStart;

            stepTimings.add(new StepTiming(step, stepDuration, stepPassed));

            String screenshotPath = screenshotHelper.capture(step);
            stepTitles.add(step);
            screenshotPaths.add(screenshotPath);
        }

        long tcDuration = System.currentTimeMillis() - tcStartMillis;
        LocalDateTime tcEndTime = LocalDateTime.now();
        LocalDateTime suiteEndTime = LocalDateTime.now();

        // ───── Screenshot Report (Existing) ─────
        WordReportGenerator reportGenerator =
                new WordReportGenerator("C:/Framework/WordReports");

        reportGenerator.generateReport(
                tc.tcId,
                tc.title,
                stepTitles,
                screenshotPaths
        );

        // ───── Performance Report (New) ─────
        List<TestCaseTiming> performanceData = new ArrayList<>();

        performanceData.add(new TestCaseTiming(
                tc.tcId,
                tc.title,
                tcStartTime,
                tcEndTime,
                tcDuration,
                tcPassed,
                stepTimings
        ));

        PerformanceReportGenerator performanceReport =
                new PerformanceReportGenerator("C:/Framework/PerformanceReports");

        performanceReport.generateReport(
                suiteStartTime,
                suiteEndTime,
                performanceData,
                tc.tcId
        );

        logger.info("Single TC performance report generated.");
    }

}
