package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.logging.Logger;

public class WaitWorkOrderHandler {

    private static final Logger logger = Logger.getLogger(WaitWorkOrderHandler.class.getName());
    private final Page page;

    public WaitWorkOrderHandler(Page page) {
        this.page = page;
    }

    public void waitForWorkOrder(String testStep) {
        int maxAttempts = 15;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                List<Locator> buttons = page.locator(".mdi-fullscreen.vr-fullscreen").all();
                boolean found = buttons.stream().anyMatch(b -> {
                    try { return b.isVisible(); } catch (Exception e) { return false; }
                });

                if (found) {
                    try {
                        List<Locator> searchBtns = page.locator("[title='Search']").all();
                        for (Locator btn : searchBtns) {
                            if (btn.isVisible() && btn.isEnabled()) {
                                btn.click();
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                    return;
                }

                page.waitForTimeout(2000);
            } catch (Exception e) {
                page.waitForTimeout(2000);
            }
        }
        logger.warning("Work order wait timed out after " + maxAttempts + " attempts");
    }
}
