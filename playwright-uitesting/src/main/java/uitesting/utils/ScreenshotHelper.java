package uitesting.utils;

import com.microsoft.playwright.Page;

import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Handles screenshot capture operations.
 */
public class ScreenshotHelper {

    private static final Logger logger = Logger.getLogger(ScreenshotHelper.class.getName());
    private final Page page;
    private final String screenshotsDir;

    public ScreenshotHelper(Page page) {
        this.page = page;
        this.screenshotsDir = ConfigManager.getInstance().getScreenshotsDir();
        ensureDirectoryExists();
    }

    public ScreenshotHelper(Page page, String screenshotsDir) {
        this.page = page;
        this.screenshotsDir = screenshotsDir;
        ensureDirectoryExists();
    }

    /**
     * Takes a screenshot and saves it with the given step name.
     */
    public String capture(String stepName) {
        try {
            // Sanitize the filename
            String safeName = stepName
                    .replaceAll("[\\\\/:*?\"<>|]", "_")
                    .replaceAll("\\*\\*", " ")
                    .replaceAll("/", " ")
                    .trim();

            if (safeName.length() > 150) {
                safeName = safeName.substring(0, 150);
            }

            String filePath = screenshotsDir + File.separator + safeName + ".png";

            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(filePath))
                    .setFullPage(false));

            logger.info("Screenshot saved: " + filePath);
            return filePath;
        } catch (Exception e) {
            logger.warning("Failed to capture screenshot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Takes a full-page screenshot.
     */
    public String captureFullPage(String name) {
        try {
            String filePath = screenshotsDir + File.separator + name + "_full.png";
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(filePath))
                    .setFullPage(true));
            return filePath;
        } catch (Exception e) {
            logger.warning("Failed to capture full-page screenshot: " + e.getMessage());
            return null;
        }
    }

    private void ensureDirectoryExists() {
        File dir = new File(screenshotsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
