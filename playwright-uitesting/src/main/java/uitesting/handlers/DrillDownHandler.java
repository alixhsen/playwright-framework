package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.List;
import java.util.logging.Logger;

public class DrillDownHandler {

    private static final Logger logger = Logger.getLogger(DrillDownHandler.class.getName());
    private final Page page;

    public DrillDownHandler(Page page) {
        this.page = page;
    }

    public void drillDown(String testStep) {
        int idx = testStep.indexOf("Drill down");
        String drillButton = testStep.substring(idx + 10).trim().replaceAll("\\.$", "").trim();

        try {
            Locator row = page.locator(
                "xpath=//div[@class='vr-datagrid-body normal-full-screen']//div[contains(.,'" + drillButton + "')]"
            ).first();
            Locator btn = row.locator(".mdi-chevron-right, .mdi-chevron-down").first();
            btn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            btn.click();
        } catch (Exception e) {
            try {
                List<Locator> rows = page.locator(
                    "xpath=//div[@class='vr-datagrid-body normal-full-screen']//span[contains(text(),'" + drillButton +
                    "')]/ancestor::div[@class='vr-datagrid-body normal-full-screen']"
                ).all();
                if (rows.size() >= 2) {
                    rows.get(1).locator(".mdi-chevron-right, .mdi-chevron-down").first().click();
                }
            } catch (Exception ex) {
                logger.warning("Drill down failed: " + ex.getMessage());
            }
        }
    }
}
