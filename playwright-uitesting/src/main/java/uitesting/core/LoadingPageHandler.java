package uitesting.core;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.logging.Logger;

public class LoadingPageHandler {

    private static final Logger logger = Logger.getLogger(LoadingPageHandler.class.getName());
    private final Page page;

    public LoadingPageHandler(Page page) {
        this.page = page;
    }

    public void waitForPageLoad() {
        try {
            if (BrowserFactory.isCRMBillingPage(page)) {
                waitForCRMBillingPage();
            } else {
                waitForStandardPage();
            }
        } catch (Exception e) {
            logger.warning("Page load wait issue: " + e.getMessage());
        }
    }

    private void waitForCRMBillingPage() {
        try {
            Locator gearLocator = page.locator(
                "div[ng-show='!scopeModel.showBPTaskEditor && scopeModel.openInlineGenericTask']:not(.ng-hide)"
            );
            for (int attempt = 0; attempt < 10; attempt++) {
                if (gearLocator.count() == 0 || !gearLocator.first().isVisible()) break;
                page.waitForTimeout(2000);
            }
        } catch (Exception e) {
            logger.fine("CRM task editor check: " + e.getMessage());
        }

        try {
            Locator loadingModal = page.locator(
                "div.vr-loader-container[ng-show='scopeModel.isLoading']:not(.ng-hide)"
            );
            if (loadingModal.count() > 0 && loadingModal.first().isVisible()) {
                loadingModal.first().waitFor(
                    new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(120000)
                );
            }
        } catch (Exception e) {
            logger.fine("Loading modal wait: " + e.getMessage());
        }
    }

    private void waitForStandardPage() {
        try {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        } catch (Exception e) {
            logger.fine("Standard page load: " + e.getMessage());
        }
    }

    public void waitForNetworkIdle() {
        try {
            page.waitForLoadState(
                LoadState.NETWORKIDLE,
                new Page.WaitForLoadStateOptions().setTimeout(30000)
            );
        } catch (Exception e) {
            logger.fine("Network idle timed out: " + e.getMessage());
        }
    }
}
