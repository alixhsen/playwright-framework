package uitesting.utils;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generates a Word (.docx) performance & timing report for the full test suite run.
 *
 * Report contains:
 *  - Run summary (date, total TCs, total duration, pass/fail count)
 *  - Per test case section: start time, end time, duration, status, step-level timing table
 */
public class PerformanceReportGenerator {

    private static final Logger logger =
            Logger.getLogger(PerformanceReportGenerator.class.getName());

    // ── Colours (hex, no #) ──────────────────────────────────────────────────
    private static final String COLOR_HEADER_BG   = "1E3A5F";  // dark navy
    private static final String COLOR_PASS_BG     = "D1FAE5";  // light green
    private static final String COLOR_FAIL_BG     = "FEE2E2";  // light red
    private static final String COLOR_ALT_ROW     = "F0F4FF";  // light blue-grey
    private static final String COLOR_WHITE       = "FFFFFF";
    private static final String COLOR_HEADER_TEXT = "FFFFFF";
    private static final String COLOR_TITLE_TEXT  = "1E3A5F";
    private static final String COLOR_ACCENT      = "3B82F6";  // blue
    private static final String COLOR_PASS_TEXT   = "065F46";  // dark green
    private static final String COLOR_FAIL_TEXT   = "991B1B";  // dark red
    private static final String COLOR_SLOW_BG     = "FEF3C7";  // amber — step > 5 s

    private static final long SLOW_STEP_THRESHOLD_MS = 5_000;  // 5 seconds

    private final String outputDir;

    public PerformanceReportGenerator(String outputDir) {
        this.outputDir = outputDir;
        new File(outputDir).mkdirs();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Public API
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Data holder for a single step's timing.
     */
    public static class StepTiming {
        public final String stepName;
        public final long   durationMs;
        public final boolean passed;   // false if the step threw an exception

        public StepTiming(String stepName, long durationMs, boolean passed) {
            this.stepName   = stepName;
            this.durationMs = durationMs;
            this.passed     = passed;
        }
    }

    /**
     * Data holder for a single test case's timing + result.
     */
    public static class TestCaseTiming {
        public final String          tcId;
        public final String          title;
        public final LocalDateTime   startTime;
        public final LocalDateTime   endTime;
        public final long            durationMs;
        public final boolean         passed;
        public final List<StepTiming> steps;

        public TestCaseTiming(String tcId, String title,
                              LocalDateTime startTime, LocalDateTime endTime,
                              long durationMs, boolean passed,
                              List<StepTiming> steps) {
            this.tcId       = tcId;
            this.title      = title;
            this.startTime  = startTime;
            this.endTime    = endTime;
            this.durationMs = durationMs;
            this.passed     = passed;
            this.steps      = steps;
        }
    }

    /**
     * Generate the full performance report.
     *
     * @param suiteStartTime  When the suite started
     * @param suiteEndTime    When the suite finished
     * @param testCases       All TC timing data collected during the run
     */
    public void generateReport(LocalDateTime suiteStartTime,
                               LocalDateTime suiteEndTime,
                               List<TestCaseTiming> testCases,
                               String fileSuffix)
    {
        try (XWPFDocument doc = new XWPFDocument()) {

            // ── Cover / Summary ──────────────────────────────────────────────
            addCoverSection(doc, suiteStartTime, suiteEndTime, testCases);

            // ── Summary Table ────────────────────────────────────────────────
            addSummaryTable(doc, testCases);

            // ── Per TC sections ──────────────────────────────────────────────
            for (TestCaseTiming tc : testCases) {
                addPageBreak(doc);
                addTestCaseSection(doc, tc);
            }

            // ── Save ─────────────────────────────────────────────────────────
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeSuffix = (fileSuffix == null || fileSuffix.isBlank())
                    ? ""
                    : fileSuffix + "_";

            String filePath = outputDir + File.separator
                    + "PerformanceReport_" + safeSuffix + timestamp + ".docx";


            try (FileOutputStream out = new FileOutputStream(filePath)) {
                doc.write(out);
            }
            logger.info("Performance report generated: " + filePath);

        } catch (Exception e) {
            logger.severe("Failed to generate performance report: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Cover & Summary
    // ════════════════════════════════════════════════════════════════════════

    private void addCoverSection(XWPFDocument doc,
                                 LocalDateTime suiteStart,
                                 LocalDateTime suiteEnd,
                                 List<TestCaseTiming> testCases) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

        long totalMs   = testCases.stream().mapToLong(t -> t.durationMs).sum();
        long passCount = testCases.stream().filter(t -> t.passed).count();
        long failCount = testCases.size() - passCount;
        long suiteDurationMs = java.time.Duration.between(suiteStart, suiteEnd).toMillis();

        // Main title
        XWPFParagraph title = doc.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun tr = title.createRun();
        tr.setText("UI Automation — Performance & Timing Report");
        tr.setBold(true);
        tr.setFontSize(22);
        tr.setColor(COLOR_TITLE_TEXT);
        tr.addBreak();

        // Subtitle
        XWPFParagraph sub = doc.createParagraph();
        sub.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun sr = sub.createRun();
        sr.setText("Generated: " + LocalDateTime.now().format(dtf));
        sr.setFontSize(10);
        sr.setColor("64748B");
        sr.addBreak();

        XWPFParagraph dev = doc.createParagraph();
        dev.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun dv = dev.createRun();
        dv.setText("Developed by Ali Hussein");
        dv.setFontSize(8);
        dv.setColor("64748B");
        dv.addBreak();
        dv.addBreak();

        // Stat pills row — 4 highlight boxes using a 4-column table
        XWPFTable stats = doc.createTable(1, 4);
        stats.setWidth("100%");
        removeBorders(stats);

        String[][] statData = {
                { String.valueOf(testCases.size()), "Total Test Cases" },
                { passCount + " / " + testCases.size(), "Passed" },
                { String.valueOf(failCount),  "Failed" },
                { formatDuration(suiteDurationMs), "Total Duration" },
        };
        String[] statColors = {
                COLOR_ACCENT, "065F46", failCount > 0 ? "991B1B" : "065F46", COLOR_HEADER_BG
        };
        String[] statBgColors = {
                "EFF6FF", COLOR_PASS_BG, failCount > 0 ? COLOR_FAIL_BG : COLOR_PASS_BG, "F0F4FF"
        };

        for (int i = 0; i < 4; i++) {
            XWPFTableCell cell = stats.getRow(0).getCell(i);
            setCellColor(cell, statBgColors[i]);
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

            XWPFParagraph cp = cell.getParagraphs().get(0);
            cp.setAlignment(ParagraphAlignment.CENTER);

            XWPFRun valRun = cp.createRun();
            valRun.setText(statData[i][0]);
            valRun.setBold(true);
            valRun.setFontSize(18);
            valRun.setColor(statColors[i]);
            valRun.addBreak();

            XWPFRun lblRun = cp.createRun();
            lblRun.setText(statData[i][1]);
            lblRun.setFontSize(9);
            lblRun.setColor("64748B");
        }

        doc.createParagraph(); // spacer

        // Suite timing info
        addInfoLine(doc, "Suite Start",    suiteStart.format(dtf));
        addInfoLine(doc, "Suite End",      suiteEnd.format(dtf));
        addInfoLine(doc, "Suite Duration", formatDuration(suiteDurationMs));
        addInfoLine(doc, "Total TC Time",  formatDuration(totalMs)
                + "  (execution time only, excludes setup/teardown)");

        doc.createParagraph();
    }

    private void addSummaryTable(XWPFDocument doc, List<TestCaseTiming> testCases) {

        addSectionHeading(doc, "Test Case Summary");

        String[] headers = { "#", "TC ID", "Title", "Start Time", "End Time", "Duration", "Steps", "Status" };
        int[]    widths  = {  4,    8,       28,       14,           14,          10,         6,        10 };

        XWPFTable table = doc.createTable(1 + testCases.size(), headers.length);
        setTableWidthPct(table, widths);

        // Header row
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            setCellColor(cell, COLOR_HEADER_BG);
            XWPFParagraph p = cell.getParagraphs().get(0);
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r = p.createRun();
            r.setText(headers[i]);
            r.setBold(true);
            r.setFontSize(9);
            r.setColor(COLOR_HEADER_TEXT);
        }

        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Data rows
        for (int i = 0; i < testCases.size(); i++) {
            TestCaseTiming tc  = testCases.get(i);
            XWPFTableRow   row = table.getRow(i + 1);
            String rowBg = tc.passed ? COLOR_PASS_BG
                    : (i % 2 == 0 ? COLOR_FAIL_BG : "FEE2E2");

            String[] values = {
                    String.valueOf(i + 1),
                    tc.tcId,
                    tc.title,
                    tc.startTime.format(tf),
                    tc.endTime.format(tf),
                    formatDuration(tc.durationMs),
                    String.valueOf(tc.steps.size()),
                    tc.passed ? "PASS" : "FAIL"
            };

            for (int j = 0; j < values.length; j++) {
                XWPFTableCell cell = row.getCell(j);
                setCellColor(cell, rowBg);
                XWPFParagraph p = cell.getParagraphs().get(0);
                p.setAlignment(j == 2 ? ParagraphAlignment.LEFT : ParagraphAlignment.CENTER);
                XWPFRun r = p.createRun();
                r.setText(values[j]);
                r.setFontSize(9);
                if (j == 7) { // Status column
                    r.setBold(true);
                    r.setColor(tc.passed ? COLOR_PASS_TEXT : COLOR_FAIL_TEXT);
                }
            }
        }

        doc.createParagraph();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Per Test Case Section
    // ════════════════════════════════════════════════════════════════════════

    private void addTestCaseSection(XWPFDocument doc, TestCaseTiming tc) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

        // TC heading
        XWPFParagraph heading = doc.createParagraph();
        heading.setStyle("Heading1");
        XWPFRun hr = heading.createRun();
        hr.setText(tc.tcId + "  —  " + tc.title);
        hr.setBold(true);
        hr.setFontSize(14);
        hr.setColor(COLOR_TITLE_TEXT);

        // TC meta info
        addInfoLine(doc, "Status",    tc.passed ? "PASS" : "FAIL",
                tc.passed ? COLOR_PASS_TEXT : COLOR_FAIL_TEXT);
        addInfoLine(doc, "Start",     tc.startTime.format(dtf));
        addInfoLine(doc, "End",       tc.endTime.format(dtf));
        addInfoLine(doc, "Duration",  formatDuration(tc.durationMs));
        addInfoLine(doc, "Steps",     String.valueOf(tc.steps.size()));

        // Slowest step callout
        tc.steps.stream()
                .max(java.util.Comparator.comparingLong(s -> s.durationMs))
                .ifPresent(slowest -> addInfoLine(doc, "Slowest Step",
                        "\"" + slowest.stepName + "\"  →  " + formatDuration(slowest.durationMs),
                        "B45309"));

        doc.createParagraph();

        // Step timing table
        addSectionHeading(doc, "Step-by-Step Timing");

        String[] headers = { "#", "Step", "Duration", "Status" };
        int[]    widths  = {  5,   65,      15,          15 };

        XWPFTable table = doc.createTable(1 + tc.steps.size(), headers.length);
        setTableWidthPct(table, widths);

        // Header
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            setCellColor(cell, COLOR_HEADER_BG);
            XWPFParagraph p = cell.getParagraphs().get(0);
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r = p.createRun();
            r.setText(headers[i]);
            r.setBold(true);
            r.setFontSize(9);
            r.setColor(COLOR_HEADER_TEXT);
        }

        // Step rows
        for (int i = 0; i < tc.steps.size(); i++) {
            StepTiming     step = tc.steps.get(i);
            XWPFTableRow   row  = table.getRow(i + 1);

            // Row colour: fail → red, slow → amber, alt → light blue, default → white
            String rowBg;
            if (!step.passed)                              rowBg = COLOR_FAIL_BG;
            else if (step.durationMs > SLOW_STEP_THRESHOLD_MS) rowBg = COLOR_SLOW_BG;
            else if (i % 2 == 1)                           rowBg = COLOR_ALT_ROW;
            else                                           rowBg = COLOR_WHITE;

            String[] values = {
                    String.valueOf(i + 1),
                    step.stepName,
                    formatDuration(step.durationMs),
                    step.passed ? "OK" : "FAIL"
            };

            for (int j = 0; j < values.length; j++) {
                XWPFTableCell cell = row.getCell(j);
                setCellColor(cell, rowBg);
                XWPFParagraph p = cell.getParagraphs().get(0);
                p.setAlignment(j == 1 ? ParagraphAlignment.LEFT : ParagraphAlignment.CENTER);
                XWPFRun r = p.createRun();
                r.setText(values[j]);
                r.setFontSize(8);
                if (j == 3) {
                    r.setBold(true);
                    r.setColor(step.passed ? COLOR_PASS_TEXT : COLOR_FAIL_TEXT);
                }
                if (j == 2 && step.durationMs > SLOW_STEP_THRESHOLD_MS) {
                    r.setBold(true);
                    r.setColor("B45309"); // amber for slow steps
                }
            }
        }

        // Legend
        doc.createParagraph();
        XWPFParagraph legend = doc.createParagraph();
        XWPFRun lr = legend.createRun();
        lr.setText("  Legend:  ");
        lr.setFontSize(8);
        lr.setColor("64748B");

        addLegendItem(legend, COLOR_PASS_BG,  COLOR_PASS_TEXT, "PASS");
        addLegendItem(legend, COLOR_FAIL_BG,  COLOR_FAIL_TEXT, "FAIL");
        addLegendItem(legend, COLOR_SLOW_BG,  "B45309",        "Slow (> 5 s)");
        addLegendItem(legend, COLOR_ALT_ROW,  "1E3A5F",        "Alternating row");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════

    /** Formats milliseconds as  "1m 23s 456ms"  or  "4s 210ms"  or  "830ms". */
    public static String formatDuration(long ms) {
        if (ms < 0) ms = 0;
        long minutes = ms / 60_000;
        long seconds = (ms % 60_000) / 1_000;
        long millis  = ms % 1_000;

        if (minutes > 0)
            return String.format("%dm %02ds %03dms", minutes, seconds, millis);
        if (seconds > 0)
            return String.format("%ds %03dms", seconds, millis);
        return millis + "ms";
    }

    private void addSectionHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("Heading2");
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(12);
        r.setColor(COLOR_TITLE_TEXT);
    }

    private void addInfoLine(XWPFDocument doc, String label, String value) {
        addInfoLine(doc, label, value, "374151");
    }

    private void addInfoLine(XWPFDocument doc, String label, String value, String valueColor) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(40);

        XWPFRun labelRun = p.createRun();
        labelRun.setText(label + ":   ");
        labelRun.setBold(true);
        labelRun.setFontSize(10);
        labelRun.setColor("1E3A5F");

        XWPFRun valueRun = p.createRun();
        valueRun.setText(value);
        valueRun.setFontSize(10);
        valueRun.setColor(valueColor);
    }

    private void addLegendItem(XWPFParagraph p, String bg, String fg, String label) {
        XWPFRun r = p.createRun();
        r.setText("  [" + label + "]  ");
        r.setFontSize(8);
        r.setColor(fg);
    }

    private void addPageBreak(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setPageBreak(true);
    }

    private void setCellColor(XWPFTableCell cell, String hexColor) {
        CTTc ctTc = cell.getCTTc();
        CTTcPr tcPr = ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setColor("auto");
        shd.setFill(hexColor);
    }

    private void removeBorders(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTTblBorders borders = tblPr.isSetTblBorders()
                ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
        for (CTBorder b : new CTBorder[]{
                borders.addNewTop(), borders.addNewBottom(),
                borders.addNewLeft(), borders.addNewRight(),
                borders.addNewInsideH(), borders.addNewInsideV()}) {
            b.setVal(STBorder.NONE);
        }
    }

    private void setTableWidthPct(XWPFTable table, int[] widthPcts) {
        // Apache POI doesn't expose percentage widths cleanly per cell,
        // so we set the overall table width and rely on relative proportions.
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTTblWidth tblWidth = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblWidth.setType(STTblWidth.PCT);
        tblWidth.setW(BigInteger.valueOf(5000)); // 100% in fiftieths of a percent
    }
}