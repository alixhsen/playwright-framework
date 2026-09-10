package uitesting.handlers;

import com.microsoft.playwright.Page;
import uitesting.core.LoadingPageHandler;
import uitesting.utils.ConfigManager;
import uitesting.utils.DatabaseHelper;

import java.util.logging.Logger;

/**
 * Handles navigation steps like:
 * "Go to Business CRM/Individuals"
 */
public class NavigationHandler {

    private static final Logger logger =
            Logger.getLogger(NavigationHandler.class.getName());

    private final Page page;
    private final DatabaseHelper databaseHelper;
    private final LoadingPageHandler loadingHandler;
    private final String navigationTable;

    public NavigationHandler(Page page) {
        this.page = page;
        this.databaseHelper = new DatabaseHelper();
        this.loadingHandler = new LoadingPageHandler(page);
        this.navigationTable =
                ConfigManager.getInstance().getNavigationDbTable();
    }

    /* ───────────────────────────────────────────── */

    /**
     * Handles test step like: "Go to X/Y/Z."
     */
    public void goToStep(String testStep) {

        if (testStep == null || !testStep.contains("Go to")) {
            return;
        }

        int index = testStep.indexOf("Go to");

        String rawPath = testStep.substring(index + 5)
                .replaceAll("\\.$", "")
                .trim();

        navigateToPath(rawPath);
    }

    /* ───────────────────────────────────────────── */

    /**
     * Navigates using DB mapping (case-insensitive).
     */
    public void navigateToPath(String targetPath) {

        if (targetPath == null || targetPath.isBlank()) {
            logger.warning("Navigation path is empty.");
            return;
        }

        // Normalize input
        String normalizedPath = targetPath.trim().toLowerCase();

        String url;

        try {
            url = databaseHelper.getUrlForPath(
                    normalizedPath,
                    navigationTable
            );
        } catch (Exception e) {
            logger.severe("Failed to retrieve URL from DB for path: "
                    + targetPath + " | " + e.getMessage());
            return;
        }

        if (url == null || url.isBlank()) {
            logger.warning("No URL found for path: " + targetPath);
            return;
        }

        try {
            logger.info("Navigating to URL: " + url);

            page.navigate(url);

            loadingHandler.waitForPageLoad();

            logger.info("Successfully navigated to: " + url);

        } catch (Exception e) {
            logger.severe("Navigation failed for URL: "
                    + url + " | " + e.getMessage());
        }
    }

    /* ───────────────────────────────────────────── */

    /**
     * Direct navigation without DB lookup.
     */
    public void navigateTo(String url) {

        if (url == null || url.isBlank()) {
            logger.warning("URL is empty.");
            return;
        }

        try {
            logger.info("Direct navigation to: " + url);

            page.navigate(url);

            loadingHandler.waitForPageLoad();

        } catch (Exception e) {
            logger.severe("Direct navigation failed: "
                    + url + " | " + e.getMessage());
        }
    }
}
