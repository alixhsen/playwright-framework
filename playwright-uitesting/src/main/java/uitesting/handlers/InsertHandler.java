package uitesting.handlers;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import uitesting.core.BrowserFactory;
import uitesting.core.LoadingPageHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Handles all "Insert into" test steps.
 */
public class InsertHandler {

    private static final Logger logger =
            Logger.getLogger(InsertHandler.class.getName());

    private final Page page;
    private final LoadingPageHandler loadingHandler;

    public InsertHandler(Page page) {
        this.page = page;
        this.loadingHandler = new LoadingPageHandler(page);
    }

    /* ───────────────────────────────────────────── */

    public void insert(String testStep) {

        loadingHandler.waitForPageLoad();

        if (testStep.contains("Insert Datetime")) {
            handleDateTimeInsert(testStep);

        } else if (testStep.contains("Insert & Add")) {
            handleInsertAndAdd(testStep);

        } else if (testStep.contains("Insert into grid")) {
            handleGridInsert(testStep);

        } else if (testStep.contains("Insert into")) {
            handleTextInsert(testStep);
        }
    }

    /* ───────────────────────────────────────────── */

    private void handleDateTimeInsert(String testStep) {

        int idx = testStep.toLowerCase().indexOf("insert datetime");
        if (idx == -1) return;

        String combo = testStep.substring(idx + 15)
                .trim().replaceAll("\\.$", "");

        String[] parts = combo.split("\\*\\*");
        if (parts.length < 2) return;

        String label = parts[0].trim();
        String value = parts[1].trim();

        // The actual HTML structure:
        // <vr-label><label class="vr-control-label">Birth Date</label></vr-label>
        // <vr-directivewrapper>
        //   <vr-datetimepicker>
        //     <input class="vr-date-input vanrise-inpute" type="text" ctrltype="date">
        //   </vr-datetimepicker>
        // </vr-directivewrapper>
        // Both are siblings inside the same parent col div.
        // The vr-datetimepicker has NO label attribute — label is a separate sibling element.

        // Strategy 1: walk up from label text to the col container, then find the date input
        if (tryDateFill(
                "xpath=//label[contains(@class,'vr-control-label') and normalize-space()='" + label + "']"
                        + "/ancestor::div[contains(@class,'col-')][1]"
                        + "//input[contains(@class,'vr-date-input')]", value)) return;

        // Strategy 2: same but broader ancestor search
        if (tryDateFill(
                "xpath=//label[normalize-space()='" + label + "']"
                        + "/ancestor::div[contains(@class,'col-')][1]"
                        + "//input[@ctrltype='date' or contains(@class,'vr-date')]", value)) return;

        // Strategy 3: find label then take the very next vr-date-input in the DOM
        if (tryDateFill(
                "xpath=//label[normalize-space()='" + label + "']"
                        + "/following::input[contains(@class,'vr-date-input')][1]", value)) return;

        // Strategy 4: find label then next input with ctrltype=date
        if (tryDateFill(
                "xpath=//label[normalize-space()='" + label + "']"
                        + "/following::input[@ctrltype='date'][1]", value)) return;

        logger.warning("All DateTime strategies failed for label: " + label);
    }

    /**
     * Tries to fill a date input located by the given selector.
     * Returns true on success, false if the element is not found or fill fails.
     */
    private boolean tryDateFill(String selector, String value) {
        try {
            Locator input = page.locator(selector).first();
            input.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(5000));
            // Click to focus, clear, then type the date value
            input.click();
            input.evaluate("el => el.value = ''");
            input.fill(value);
            input.press("Enter");
            logger.info("DateTime filled using selector: " + selector);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /* ───────────────────────────────────────────── */

    private void handleInsertAndAdd(String testStep) {

        int idx = testStep.indexOf("Insert & Add into");
        if (idx == -1) return;

        String combo = testStep.substring(idx + 17)
                .trim().replaceAll("\\.$", "");

        String[] parts = combo.split("\\*\\*");
        if (parts.length < 2) return;

        String label = parts[0].trim();

        Locator scope = BrowserFactory.isModalOpen(page)
                ? page.locator(".modal-content").first()
                : page.locator("body");

        for (int i = 1; i < parts.length; i++) {

            String value = parts[i].trim();

            try {
                Locator inputField = scope.locator(
                        "xpath=//vr-label[label[contains(text(),'"
                                + label + "')]]/following-sibling::div//input"
                ).first();

                inputField.fill(value);

                Locator addBtn = scope.locator(
                        "xpath=//vr-label[label[contains(text(),'"
                                + label + "')]]//following::span[contains(@class,'mdi-plus-circle-outline')]"
                ).first();

                addBtn.click();

            } catch (Exception e) {
                logger.warning("Insert & Add failed for label "
                        + label + ": " + e.getMessage());
            }
        }
    }

    /* ───────────────────────────────────────────── */

    private void handleGridInsert(String testStep) {

        Pattern pattern =
                Pattern.compile("row:(\\d+) & Column (\\d+) with value (.+?)\\.");

        Matcher matcher = pattern.matcher(testStep);
        if (!matcher.find()) return;

        int rowIndex = Integer.parseInt(matcher.group(1));
        int columnIndex = Integer.parseInt(matcher.group(2));
        String value = matcher.group(3);

        try {
            String xpath = String.format(
                    "(//div[contains(@class,'grid-base-row')])[%d]" +
                            "//div[contains(@class,'vr-datagrid-cell-container')][%d]//input",
                    rowIndex, columnIndex
            );

            Locator cell = page.locator("xpath=" + xpath).first();

            cell.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE));

            cell.fill(value);
            cell.press("Enter");

        } catch (Exception e) {
            logger.warning("Grid insert failed: " + e.getMessage());
        }
    }

    /* ───────────────────────────────────────────── */

    private void handleTextInsert(String testStep) {

        int idx = testStep.indexOf("Insert into");
        if (idx == -1) return;

        String combo = testStep.substring(idx + 11)
                .trim().replaceAll("\\.$", "");

        String[] parts = combo.split("\\*\\*");
        if (parts.length < 2) return;

        String label = parts[0].trim();
        String value = parts[1].trim();

        boolean modalOpen = BrowserFactory.isModalOpen(page);
        // scopeXpath prefix ensures XPath stays within modal when one is open
        String scopeXpath = modalOpen
                ? "//div[contains(@class,'modal-content')]"
                : "";

        // Strategy 1: vr-textbox[label='...'] — works when Angular keeps the label attribute
        try {
            Locator scope = modalOpen
                    ? page.locator(".modal-content").first()
                    : page.locator("body");
            Locator textbox = scope.locator(
                    "vr-textbox[label='" + label + "'] input.form-control"
            ).first();
            if (textbox.count() > 0) {
                textbox.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE).setTimeout(3000));
                textbox.fill(value);
                return;
            }
        } catch (Exception ignored) {}

        // Strategy 2: vr-textarea[label='...']
        try {
            Locator scope = modalOpen
                    ? page.locator(".modal-content").first()
                    : page.locator("body");
            Locator textarea = scope.locator(
                    "vr-textarea[label='" + label + "'] textarea.form-control"
            ).first();
            if (textarea.count() > 0) {
                textarea.fill(value);
                return;
            }
        } catch (Exception ignored) {}

        // Strategy 3: vr-control-label → ancestor col → non-hidden non-disabled input
        // Uses scoped XPath (scopeXpath prefix keeps search inside modal)
        try {
            Locator input = page.locator(
                    "xpath=" + scopeXpath +
                            "//label[contains(@class,'vr-control-label') and normalize-space()='" + label + "']" +
                            "/ancestor::div[contains(@class,'col-')][1]" +
                            "//input[@type='text' and not(@tabindex='-1') and not(@type='hidden')]"
            ).first();
            if (input.count() > 0) {
                input.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE).setTimeout(3000));
                input.fill(value);
                return;
            }
        } catch (Exception ignored) {}

        // Strategy 4: same but allow any tabindex (field may be enabled but tabindex not set)
        try {
            Locator input = page.locator(
                    "xpath=" + scopeXpath +
                            "//label[contains(@class,'vr-control-label') and normalize-space()='" + label + "']" +
                            "/ancestor::div[contains(@class,'col-')][1]" +
                            "//input[not(@type='hidden')]"
            ).first();
            if (input.count() > 0) {
                input.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE).setTimeout(3000));
                input.fill(value);
                return;
            }
        } catch (Exception ignored) {}

        // Strategy 5: label normalize-space → following sibling input (scoped)
        try {
            Locator input = page.locator(
                    "xpath=" + scopeXpath +
                            "//label[normalize-space()='" + label + "']" +
                            "/following::input[@type='text' and not(@tabindex='-1')][1]"
            ).first();
            if (input.count() > 0) {
                input.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE).setTimeout(3000));
                input.fill(value);
                return;
            }
        } catch (Exception ignored) {}

        // Strategy 6: label normalize-space → any following input (broadest fallback, scoped)
        try {
            Locator input = page.locator(
                    "xpath=" + scopeXpath +
                            "//label[normalize-space()='" + label + "']/following::input[not(@type='hidden')][1]"
            ).first();
            if (input.count() > 0) {
                input.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE).setTimeout(3000));
                input.fill(value);
                return;
            }
        } catch (Exception ignored) {}

        try {
            // iframe body
            if (label.equalsIgnoreCase("Body")) {
                FrameLocator frame = page.frameLocator("iframe").first();
                frame.locator("body").fill(value);
                return;
            }

        } catch (Exception ignored) {}

        logger.warning("All insert strategies failed for label: " + label);
    }

}