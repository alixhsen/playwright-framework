package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.logging.Logger;

/**
 * Selects a phone number from the Available Numbers grid.
 *
 * HTML structure (confirmed from DOM):
 *   div.vr-datagrid-body.normal-full-screen          ← row
 *     div.grid-base-row
 *       div[columnname=""]                           ← switch cell
 *         vr-switch
 *           div.vr-switch
 *             span.switch.green [ng-click="ctrl.toogleCheck()"]   ← CLICK THIS
 *       div[columnname="Number"]                     ← number cell
 *         div.vr-datagrid-celltext                   ← contains "01379485"
 */
public class SelectPhoneNumberHandler {

    private static final Logger logger =
            Logger.getLogger(SelectPhoneNumberHandler.class.getName());

    private final Page page;

    public SelectPhoneNumberHandler(Page page) {
        this.page = page;
    }

    public void handle(String phoneNumber) {

        String num = phoneNumber.trim();
        logger.info("SelectPhoneNumberHandler: selecting '" + num + "'");

        // Wait for the grid to load rows
        try {
            page.waitForSelector("div.vr-datagrid-body.normal-full-screen",
                    new Page.WaitForSelectorOptions().setTimeout(15_000));
        } catch (Exception e) {
            logger.warning("Grid did not appear: " + e.getMessage());
        }

        // XPath: find the row (vr-datagrid-body) that contains a Number cell with our number,
        // then find the switch span inside that same row
        String switchXpath =
                "//div[contains(@class,'vr-datagrid-body') and contains(@class,'normal-full-screen')]" +
                        "[.//div[@columnname='Number']//div[contains(@class,'vr-datagrid-celltext') and normalize-space(text())='" + num + "']]" +
                        "//span[contains(@class,'switch') and contains(@class,'green')]";

        try {
            Locator switchSpan = page.locator("xpath=" + switchXpath).first();
            switchSpan.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(10_000));
            switchSpan.scrollIntoViewIfNeeded();
            switchSpan.click();
            logger.info("SelectPhoneNumberHandler: clicked switch for '" + num + "'");
            return;
        } catch (Exception e) {
            logger.warning("Primary strategy failed: " + e.getMessage());
        }

        // Fallback: find the Number cell text, walk up to grid-base-row, click switch in same row
        try {
            String cellXpath =
                    "//div[@columnname='Number']//div[contains(@class,'vr-datagrid-celltext') and normalize-space(text())='" + num + "']";

            Locator cell = page.locator("xpath=" + cellXpath).first();
            cell.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(10_000));

            // Walk up to grid-base-row then find switch
            Locator row = cell.locator("xpath=ancestor::div[contains(@class,'grid-base-row')][1]");
            Locator sw = row.locator("span.switch.green").first();
            sw.scrollIntoViewIfNeeded();
            sw.click();
            logger.info("SelectPhoneNumberHandler: fallback clicked switch for '" + num + "'");

        } catch (Exception e) {
            logger.warning("SelectPhoneNumberHandler: all strategies failed for '" + num + "': " + e.getMessage());
        }
    }
}