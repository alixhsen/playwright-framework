package uitesting.utils;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public class WordReportGenerator {

    private static final Logger logger =
            Logger.getLogger(WordReportGenerator.class.getName());

    private final String outputDir;

    public WordReportGenerator(String outputDir) {
        this.outputDir = outputDir;
        ensureDirectoryExists();
    }

    public void generateReport(String testCaseId,
                               String testCaseTitle,
                               List<String> stepTitles,
                               List<String> screenshotPaths) {

        try (XWPFDocument document = new XWPFDocument()) {

            // ===== Test Case Title =====
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setStyle("Heading1");

            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setText("Test Case: " + testCaseId + " - " + testCaseTitle);

            document.createParagraph(); // empty line

            // ===== Add Steps + Screenshots =====
            for (int i = 0; i < stepTitles.size(); i++) {

                // Step Title
                XWPFParagraph stepParagraph = document.createParagraph();
                stepParagraph.setStyle("Heading2");

                XWPFRun stepRun = stepParagraph.createRun();
                stepRun.setBold(true);
                stepRun.setFontSize(14);
                stepRun.setText(stepTitles.get(i));

                // Screenshot
                if (i < screenshotPaths.size()) {
                    String imagePath = screenshotPaths.get(i);

                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {

                        XWPFParagraph imageParagraph = document.createParagraph();
                        XWPFRun imageRun = imageParagraph.createRun();

                        try (FileInputStream fis = new FileInputStream(imageFile)) {
                            imageRun.addPicture(
                                    fis,
                                    XWPFDocument.PICTURE_TYPE_PNG,
                                    imageFile.getName(),
                                    Units.toEMU(300), // width
                                    Units.toEMU(200)  // height
                            );
                        }
                    }
                }

                document.createParagraph(); // spacing
            }

            // ===== Save File =====
            String filePath = outputDir + File.separator
                    + testCaseId + "_Report.docx";

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }

            logger.info("Word report generated: " + filePath);

        } catch (Exception e) {
            logger.severe("Failed to generate Word report: " + e.getMessage());
        }
    }

    private void ensureDirectoryExists() {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
