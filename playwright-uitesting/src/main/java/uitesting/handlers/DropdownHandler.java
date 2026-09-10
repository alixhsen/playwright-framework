package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import uitesting.core.BrowserFactory;
import uitesting.core.LoadingPageHandler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Handles dropdown steps like:
 * - Select the
 * - Search & Select
 */
public class DropdownHandler {

    private static final Logger logger =
            Logger.getLogger(DropdownHandler.class.getName());

    private final Page page;
    private final LoadingPageHandler loadingHandler;

    public DropdownHandler(Page page) {
        this.page = page;
        this.loadingHandler = new LoadingPageHandler(page);
    }

    public void dropdown(String testStep) {

        loadingHandler.waitForPageLoad();

        if (testStep.contains("Search & Select") &&
                testStep.contains("In Advanced Filter")) {

            handleAdvancedFilterSearchSelect(testStep);

        } else if (testStep.contains("Search & Select into grid")) {

            handleGridSearchSelect(testStep);

        } else if (testStep.contains("Search & Select")) {

            handleSearchAndSelect(testStep);

        } else {

            handleSelectThe(testStep);
        }
    }

    /* ───────────────────────────────────────────── */

    private void handleAdvancedFilterSearchSelect(String testStep) {

        Pattern pattern = Pattern.compile("row\\s(\\d+)\\sfor (.+?)\\.");
        Matcher matcher = pattern.matcher(testStep);

        if (!matcher.find()) return;

        int rowNumber = Integer.parseInt(matcher.group(1));
        String fieldText = matcher.group(2);

        String[] choice = fieldText.split("\\*\\*");
        if (choice.length < 2) return;

        String label = choice[0].trim();
        String value = choice[1].trim();

        try {
            List<Locator> dropdowns =
                    page.locator("vr-select[label='" + label + "'] button.vr-dropdown-select").all();

            if (rowNumber <= dropdowns.size()) {
                dropdowns.get(rowNumber - 1).click();
            }

            Locator input =
                    page.locator("vr-select[label='" + label + "'] input.form-control").first();

            input.fill(value);

            selectResultByText(value);

        } catch (Exception e) {
            logger.warning("Advanced Filter search failed: " + e.getMessage());
        }

        dismissDropdown();
    }

    /* ───────────────────────────────────────────── */

    private void handleSearchAndSelect(String testStep) {

        Pattern pattern = Pattern.compile("Search & Select for (.+?)\\.");
        Matcher matcher = pattern.matcher(testStep);

        if (!matcher.find()) return;

        String selection = matcher.group(1);
        String[] choice = selection.split("\\*\\*");

        if (choice.length < 2) return;

        String label = choice[0].trim();
        String value = choice[1].trim();

        try {
            openDropdownByLabel(label);

            // Use the corrected findDropdownInput (no longer needs label param)
            Locator input = findDropdownInput(label);
            if (input != null) {
                input.fill(value);
                loadingHandler.waitForPageLoad(); // ✅ wait for results to filter
            }

            selectResultByText(value);

        } catch (Exception e) {
            logger.warning("Search & Select failed: " + e.getMessage());
        }

        dismissDropdown();
    }

    /* ───────────────────────────────────────────── */

    private void handleGridSearchSelect(String testStep) {

        Pattern pattern =
                Pattern.compile("row:(\\d+) & Column (\\d+) with value (.+?)\\.");

        Matcher matcher = pattern.matcher(testStep);

        if (!matcher.find()) return;

        int rowIndex = Integer.parseInt(matcher.group(1));
        int colIndex = Integer.parseInt(matcher.group(2));
        String value = matcher.group(3);

        try {
            String xpath = String.format(
                    "(//div[contains(@class,'grid-base-row')])[%d]" +
                            "//div[contains(@class,'vr-datagrid-cell-container')][%d]//vr-select",
                    rowIndex, colIndex
            );

            Locator cell = page.locator("xpath=" + xpath).first();
            cell.click();

            Locator input = page.locator("#filterInput").first();
            input.fill(value);

            selectResultByText(value);

        } catch (Exception e) {
            logger.warning("Grid Search & Select failed: " + e.getMessage());
        }
    }

    /* ───────────────────────────────────────────── */

    private void handleSelectThe(String testStep) {

        int idx = testStep.indexOf("Select the");
        if (idx == -1) return;

        String combo = testStep.substring(idx + 10)
                .trim().replaceAll("\\.$", "");

        String[] choice = combo.split("\\*\\*");
        if (choice.length < 2) return;

        String label = choice[0].trim();
        String value = choice[1].trim(); // ✅ extract value

        try {
            openDropdownByLabel(label);

            // Try search input only if it exists (some dropdowns are static)
            try {
                Locator input = page.locator(
                        "ul.vr-select-dropdown-menu:visible input, " +
                                "ul.vr-select-dropdown-menu:visible #filterInput"
                ).first();

                input.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(2000));

                input.fill(value);
                loadingHandler.waitForPageLoad();

            } catch (Exception ignored) {
                // Static dropdown — no search input needed, proceed directly
            }

            selectResultByText(value);

        } catch (Exception e) {
            logger.warning("Select the failed: " + e.getMessage());
        }

        dismissDropdown();
    }

    /* ───────────────────────────────────────────── */
    /* Helpers */
    /* ───────────────────────────────────────────── */

    private void openDropdownByLabel(String labelText) {
        try {
            boolean modalOpen = BrowserFactory.isModalOpen(page);

            // Strategy 1: vr-select[label='...'] — works when label attribute is set on the component
            try {
                Locator button = modalOpen
                        ? page.locator(".modal-content vr-select[label='" + labelText + "'] button.vr-dropdown-select").first()
                        : page.locator("vr-select[label='" + labelText + "'] button.vr-dropdown-select").first();

                button.waitFor(new Locator.WaitForOptions().setTimeout(4000));
                button.scrollIntoViewIfNeeded();
                button.click();

                page.waitForSelector("ul.vr-select-dropdown-menu:visible",
                        new Page.WaitForSelectorOptions().setTimeout(5000));
                return;

            } catch (Exception ignored) {}

            // Strategy 2: find label text, then its sibling/ancestor button — covers searchable dropdowns
            // where the label is rendered separately from the vr-select component
            try {
                String scopePrefix = modalOpen ? "//div[contains(@class,'modal-content')]" : "";
                Locator button = page.locator(
                        "xpath=" + scopePrefix +
                                "//label[contains(@class,'vr-control-label') and normalize-space()='" + labelText + "']" +
                                "/ancestor::div[contains(@class,'col-')][1]" +
                                "//button[contains(@class,'vr-dropdown-select')]"
                ).first();

                button.waitFor(new Locator.WaitForOptions().setTimeout(4000));
                button.scrollIntoViewIfNeeded();
                button.click();

                page.waitForSelector("ul.vr-select-dropdown-menu:visible",
                        new Page.WaitForSelectorOptions().setTimeout(5000));
                return;

            } catch (Exception ignored) {}

            // Strategy 3: label text → nearest following vr-dropdown-select button
            try {
                String scopePrefix = modalOpen ? "//div[contains(@class,'modal-content')]" : "";
                Locator button = page.locator(
                        "xpath=" + scopePrefix +
                                "//label[normalize-space()='" + labelText + "']" +
                                "/following::button[contains(@class,'vr-dropdown-select')][1]"
                ).first();

                button.waitFor(new Locator.WaitForOptions().setTimeout(4000));
                button.scrollIntoViewIfNeeded();
                button.click();

                page.waitForSelector("ul.vr-select-dropdown-menu:visible",
                        new Page.WaitForSelectorOptions().setTimeout(5000));
                return;

            } catch (Exception ignored) {}

            logger.warning("Failed to open dropdown for label: " + labelText);

        } catch (Exception e) {
            logger.warning("Failed to open dropdown for label: " + labelText);
        }
    }


    private Locator findDropdownInput(String labelText) {
        try {
            boolean modalOpen = BrowserFactory.isModalOpen(page);
            String scope = modalOpen ? ".modal-content " : "";

            Locator input = page.locator(
                    scope + "ul.vr-select-dropdown-menu:visible input.filter-input, " +
                            scope + "ul.vr-select-dropdown-menu:visible #filterInput, " +
                            scope + "ul.vr-select-dropdown-menu:visible input.form-control"
            ).first();

            input.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(3000));

            return input;

        } catch (Exception ignored) {}

        return null;
    }

    private void selectResultByText(String value) {
        try {
            // Strategy 1: exact match on anchor tag in visible dropdown
            Locator option = page.locator(
                    "xpath=//ul[contains(@class,'vr-select-dropdown-menu') and not(ancestor::*[@style='display: none;'])]" +
                            "//a[normalize-space()='" + value + "']"
            ).first();

            option.waitFor(new Locator.WaitForOptions().setTimeout(7000));
            option.click();
            return;

        } catch (Exception ignored) {}

        try {
            // Strategy 2: contains match — handles extra whitespace or nested spans
            Locator option = page.locator(
                    "xpath=//ul[contains(@class,'vr-select-dropdown-menu') and not(ancestor::*[@style='display: none;'])]" +
                            "//div[contains(@class,'select-item') and contains(normalize-space(),'" + value + "')]"
            ).first();

            option.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            option.click();
            return;

        } catch (Exception ignored) {}

        logger.warning("Dropdown selection failed for value: " + value);
    }


    private void dismissDropdown() {
        try {
            page.locator("body").click();
        } catch (Exception ignored) {}
    }
}