package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.logging.Logger;

public class View360Handler {

    private static final Logger logger = Logger.getLogger(View360Handler.class.getName());
    private final Page page;

    public View360Handler(Page page) {
        this.page = page;
    }

    public void view(String testStep) {
        try {
            page.waitForTimeout(1000);
            List<Locator> buttons = page.locator(".mdi-fullscreen.vr-fullscreen").all();
            for (Locator btn : buttons) {
                if (btn.isVisible()) {
                    btn.click();
                    return;
                }
            }
        } catch (Exception e) {
            logger.warning("360 View failed: " + e.getMessage());
        }
    }
}
