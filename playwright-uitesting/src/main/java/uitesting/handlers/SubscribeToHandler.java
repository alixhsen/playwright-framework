package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.logging.Logger;

/**
 * Handles steps of the form:
 *
 *   Subscribe to [ChildActionName].
 *
 * Examples:
 *   Subscribe to Line.
 *   Subscribe to PABX.
 *   Subscribe to Dedicated Internet.
 */
public class SubscribeToHandler {

    private static final Logger logger = Logger.getLogger(SubscribeToHandler.class.getName());

    private final Page page;

    public SubscribeToHandler(Page page) {
        this.page = page;
    }

    public void handle(String childName) {

        String childTrimmed = childName.trim();
        logger.info("SubscribeToHandler: clicking 'Subscribe To' → '" + childTrimmed + "'");

        // ── Step 1: find and click the Subscribe To button ────────────────────
        Locator subscribeBtn = page.locator("button[action-name='Subscribeto']").first();

        subscribeBtn.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(15_000));

        subscribeBtn.click();
        logger.info("SubscribeToHandler: button clicked.");

        // ── Step 2: wait for Angular ng-show to render the dropdown ───────────
        // Strategy: poll for ANY visible .child-action element, up to 10s.
        // Angular's ng-show toggles visibility via display:none / display:block,
        // so we use JavaScript to detect when child items are actually rendered.
        page.waitForFunction(
                "() => document.querySelectorAll('div.child-action').length > 0 && " +
                        "Array.from(document.querySelectorAll('div.child-action'))" +
                        ".some(el => el.offsetParent !== null)",
                null,
                new Page.WaitForFunctionOptions().setTimeout(10_000)
        );

        logger.info("SubscribeToHandler: dropdown items visible, looking for '" + childTrimmed + "'");

        // ── Step 3: click the child action ───────────────────────────────────
        // Try by action-name attribute first (most reliable)
        String actionNameNoSpaces = childTrimmed.replaceAll("\\s+", "");

        Locator childByAttr = page.locator(
                "div.child-action[action-name='" + actionNameNoSpaces + "']"
        ).first();

        if (childByAttr.count() > 0 && childByAttr.isVisible()) {
            childByAttr.click();
            logger.info("SubscribeToHandler: clicked by action-name='" + actionNameNoSpaces + "'");
            return;
        }

        // Fallback: match by visible span text inside any child-action
        // Note: no :not([ng-hide]) filter — Angular may use display:none instead
        Locator childByText = page.locator("div.child-action span")
                .filter(new Locator.FilterOptions().setHasText(childTrimmed))
                .first();

        childByText.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5_000));

        childByText.click();
        logger.info("SubscribeToHandler: clicked by text='" + childTrimmed + "'");
    }
}