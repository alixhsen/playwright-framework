package uitesting.core;

import com.microsoft.playwright.*;
import uitesting.utils.ConfigManager;

public class BrowserFactory {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private final ConfigManager config = ConfigManager.getInstance();

    /**
     * Initializes Playwright and launches the browser.
     */
    public Page initBrowser() {
        playwright = Playwright.create();

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(config.isHeadless())
                .setSlowMo(config.getSlowMo());

        String browserType = config.getBrowserType().toLowerCase();
        switch (browserType) {
            case "firefox":
                browser = playwright.firefox().launch(launchOptions);
                break;
            case "webkit":
                browser = playwright.webkit().launch(launchOptions);
                break;
            default:
                browser = playwright.chromium().launch(launchOptions);
                break;
        }

        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setIgnoreHTTPSErrors(true));

        context.setDefaultTimeout(config.getTimeout());

        page = context.newPage();
        return page;
    }

    /**
     * Returns the active Page instance.
     */
    public Page getPage() {
        return page;
    }

    /**
     * Closes the browser and Playwright instance.
     */
    public void closeBrowser() {
        try {
            if (page != null && !page.isClosed()) page.close();
            if (context != null) context.close();
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Checks if a modal with a specific title is open.
     */
    public static boolean isModalOpen(Page page) {
        return page.locator(".modal-content").count() > 0 &&
               page.locator(".modal-content").first().isVisible();
    }

    /**
     * Gets the count of open modal windows.
     */
    public static int getModalCount(Page page) {
        return page.locator(".modal-content").count();
    }

    /**
     * Checks if the page is a CRM billing page.
     */
    public static boolean isCRMBillingPage(Page page) {
        return page.content().contains("CRMBilling");
    }
}
