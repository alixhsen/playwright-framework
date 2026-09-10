package uitesting.handlers;

import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.nio.file.Paths;
import java.util.logging.Logger;

public class UploadHandler {

    private static final Logger logger = Logger.getLogger(UploadHandler.class.getName());
    private final Page page;

    public UploadHandler(Page page) {
        this.page = page;
    }

    public void upload(String testStep) {
        int idx = testStep.indexOf("Upload file");
        String filePath = testStep.substring(idx + 11).trim().replaceAll("\\.$", "").trim();

        try {
            Locator fileInput = page.locator("input[type='file']").first();
            fileInput.setInputFiles(Paths.get(filePath));
            page.waitForTimeout(3000);
        } catch (Exception e) {
            try {
                FileChooser fileChooser = page.waitForFileChooser(() -> {
                    page.locator(
                        "xpath=//span[contains(@class,'btn-primary') and contains(@class,'fileinput-button')]"
                    ).first().click();
                });
                fileChooser.setFiles(Paths.get(filePath));
                page.waitForTimeout(3000);
            } catch (Exception ex) {
                logger.warning("Upload failed: " + ex.getMessage());
            }
        }
    }
}
