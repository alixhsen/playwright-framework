package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.logging.Logger;

public class BPHandler {

    private static final Logger logger = Logger.getLogger(BPHandler.class.getName());
    private final Page page;

    public BPHandler(Page page) {
        this.page = page;
    }

    public void closeBp(String testStep) {
        try {
            Locator statusElement = page.locator("div.vr-status-progress").first();
            statusElement.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

            page.waitForFunction(
                "el => el.textContent.trim() === 'Completed'",
                statusElement.elementHandle(),
                new Page.WaitForFunctionOptions().setTimeout(60000)
            );

            Locator closeButton = page.locator("button.close").first();
            closeButton.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            closeButton.click();
        } catch (Exception e) {
            logger.warning("Close BP failed: " + e.getMessage());
        }
    }
}
