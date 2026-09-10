package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import uitesting.core.LoadingPageHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Handles Enable/Disable toggle button steps.
 */
public class EnableToggleButtonHandler {

    private static final Logger logger =
            Logger.getLogger(EnableToggleButtonHandler.class.getName());

    private final Page page;
    private final LoadingPageHandler loadingHandler;

    public EnableToggleButtonHandler(Page page) {
        this.page = page;
        this.loadingHandler = new LoadingPageHandler(page);
    }

    /* ───────────────────────────────────────────── */

    public void enableToggleButton(String testStep) {

        loadingHandler.waitForPageLoad();

        if (testStep.contains("grid") && !testStep.contains("Products")) {
            handleGridToggle(testStep);

        } else if (testStep.contains("Products")) {
            handleProductGridToggle(testStep);

        } else {
            handleLabelToggle(testStep);
        }
    }

    /* ───────────────────────────────────────────── */

    private void handleGridToggle(String testStep) {

        Pattern pattern =
                Pattern.compile("row:(\\d+) & Column:(\\d+)");

        Matcher matcher = pattern.matcher(testStep);
        if (!matcher.find()) return;

        int rowIndex = Integer.parseInt(matcher.group(1));
        int colIndex = Integer.parseInt(matcher.group(2));

        try {
            String cssSelector = String.format(
                    ".vr-datagrid-row.normal-row:nth-child(%d) " +
                            ".vr-datagrid-cell-container:nth-child(%d) " +
                            ".vr-switch span.switch",
                    rowIndex, colIndex
            );

            Locator toggle = page.locator(cssSelector).first();

            toggle.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE));

            toggle.click();

        } catch (Exception e) {
            logger.warning("Grid toggle failed: " + e.getMessage());
        }
    }

    /* ───────────────────────────────────────────── */

    private void handleProductGridToggle(String testStep) {

        Pattern pattern =
                Pattern.compile("Enable the (.+?)\\.");

        Matcher matcher = pattern.matcher(testStep);
        if (!matcher.find()) return;

        String productName = matcher.group(1).trim();

        try {
            String xpath =
                    "//div[@class='vr-datagrid-body normal-full-screen']" +
                            "//span[contains(text(),'" + productName + "')]" +
                            "/preceding::span[contains(@class,'switch')][1]";

            Locator toggle = page.locator("xpath=" + xpath).first();

            toggle.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE));

            toggle.scrollIntoViewIfNeeded();
            toggle.click();

        } catch (Exception e) {
            logger.warning("Product grid toggle failed: " + e.getMessage());
        }
    }

    /* ───────────────────────────────────────────── */

    private void handleLabelToggle(String testStep) {

        boolean isDisable = testStep.contains("Disable");

        String toggleText;

        if (isDisable) {
            int idx = testStep.indexOf("Disable the");
            if (idx == -1) return;
            toggleText = testStep.substring(idx + 11);

        } else {
            int idx = testStep.indexOf("Enable the");
            if (idx == -1) return;
            toggleText = testStep.substring(idx + 10);
        }

        toggleText = toggleText.replaceAll("\\.$", "").trim();

        String[] labels = toggleText.split("\\*\\*");

        for (String label : labels) {

            label = label.trim();

            Locator switchButton = findSwitchButton(label);

            if (switchButton != null) {

                try {
                    switchButton.waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE));

                    switchButton.click();

                } catch (Exception e) {
                    logger.warning("Toggle click failed for: " + label);
                }

            } else {
                logger.warning("Switch button not found for label: " + label);
            }
        }
    }

    /* ───────────────────────────────────────────── */

    private Locator findSwitchButton(String label) {

        try {
            String xpath1 =
                    "//vr-switch[.//label[normalize-space(.)='" +
                            label + "']]//span[contains(@class,'switch')]";

            Locator btn1 = page.locator("xpath=" + xpath1).first();
            if (btn1.count() > 0) return btn1;

        } catch (Exception ignored) {}

        try {
            String xpath2 =
                    "//label[normalize-space(.)='" + label +
                            "']/following::vr-switch//span[contains(@class,'switch')][1]";

            Locator btn2 = page.locator("xpath=" + xpath2).first();
            if (btn2.count() > 0) return btn2;

        } catch (Exception ignored) {}

        return null;
    }
}
