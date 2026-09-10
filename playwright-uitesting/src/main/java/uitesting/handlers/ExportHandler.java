package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.logging.Logger;

public class ExportHandler {

    private static final Logger logger = Logger.getLogger(ExportHandler.class.getName());
    private final Page page;

    public ExportHandler(Page page) {
        this.page = page;
    }

    public void export(String testStep) {
        try {
            Locator cogIcon = page.locator(".vr-grid-menu-icon.hand-cursor.mdi.mdi-cog").first();
            cogIcon.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            cogIcon.click();

            Locator exportBtn = page.locator(
                "li.hand-cursor.vr-grid-menu-header[ng-click*='ctrl.onExportClicked()']"
            ).first();
            exportBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            exportBtn.click();
        } catch (Exception e) {
            logger.warning("Export failed: " + e.getMessage());
        }
    }
}
