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
 * Handles all button press test steps.
 */
public class ButtonHandler {

    private static final Logger logger =
            Logger.getLogger(ButtonHandler.class.getName());

    private static final String[] ADDITIONAL_OPTIONS = {
            "Set Subscription Name", "Change Connectivity", "Submit Documents",
            "Canvassing", "CPE From Ogero", "CPE From Market", "Select Products",
            "Select IP Address", "Purchase from Stock", "Add to Wish List",
            "Upload Guarantee", "Link Products To Guarantees", "Advanced Actions",
            "Set Order as VIP", "Add Delay Complaint", "Cancel Order",
            "Select Reserved Phone Number", "Reactivate Old Phone Number",
            "Line", "PABX", "Add To Customer List", "Remove From Customer List",
            "Register CDR Details", "Sales Order", "Reserve Phone Number",
            "Test Speed", "Approve", "Reject", "Subscription", "Sub Products"
    };

    private final Page page;
    private final LoadingPageHandler loadingHandler;

    public ButtonHandler(Page page) {
        this.page = page;
        this.loadingHandler = new LoadingPageHandler(page);
    }

    /**
     * Main entry point.
     */
    public void buttonPress(String testStep) {

        loadingHandler.waitForPageLoad();

        if (testStep.contains("grid")) {
            handleGridButtonPress(testStep);
        } else if (testStep.contains("Show Filter")) {
            handleShowFilter();
        } else if (testStep.contains("Button for")) {
            handleLabeledButtonPress(testStep);
        } else {
            handleSimpleButtonPress(testStep);
        }
    }

    /**
     * Handles: "Press on X."
     */
    private void handleSimpleButtonPress(String testStep) {

        Pattern pattern = Pattern.compile("Press on (.+?)\\.?$");
        Matcher matcher = pattern.matcher(testStep);

        if (!matcher.find()) return;

        String buttonText = matcher.group(1).trim();

        Locator element = findElementByText(buttonText);

        if (element != null) {
            try {
                element.click();
                loadingHandler.waitForPageLoad();
            } catch (Exception e) {
                logger.warning("Click failed for: " + buttonText + " - " + e.getMessage());
            }
        } else {
            logger.warning("Button not found: " + buttonText);
        }
    }

    /**
     * Handles: "In the grid, Press on X."
     */
    private void handleGridButtonPress(String testStep) {

        Pattern pattern = Pattern.compile("Press on (.+?)\\.");
        Matcher matcher = pattern.matcher(testStep);

        if (!matcher.find()) return;

        String buttonText = matcher.group(1);

        try {
            Locator container = page.locator(".action-icon-container").first();
            container.hover();

            Locator expand = container.locator(".mdi-chevron-up").first();
            if (expand.isVisible()) {
                expand.click();
            }

            Locator gridButton =
                    page.locator("[title='" + buttonText + "'] button").first();

            gridButton.click();

        } catch (Exception e) {
            logger.warning("Grid button press failed: " + e.getMessage());
        }
    }

    /**
     * Handles "Show Filter".
     */
    private void handleShowFilter() {

        Locator showFilter =
                page.locator("span.filter-button-cursor a.filter-label").first();

        showFilter.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
        );

        showFilter.click();
    }

    /**
     * Handles: "Press on X Button for Y."
     */
    private void handleLabeledButtonPress(String testStep) {

        Pattern pattern =
                Pattern.compile("Press on (.+?) Button for (.+?)\\.");

        Matcher matcher = pattern.matcher(testStep);

        if (!matcher.find()) return;

        String buttonTitle = matcher.group(1);
        String fieldLabel = matcher.group(2);

        boolean modalOpen = BrowserFactory.isModalOpen(page);
        String scopeXpath = modalOpen ? "//div[contains(@class,'modal-content')]" : "";

        // Strategy 1: div[title='Search'] near the Number label — matches the exact rendered HTML
        // Structure: label.vr-control-label "Number" → ancestor col → following div[title='Search']
        try {
            Locator button = page.locator(
                    "xpath=" + scopeXpath +
                            "//label[contains(@class,'vr-control-label') and normalize-space()='" + fieldLabel + "']" +
                            "/ancestor::div[contains(@class,'col-')][1]" +
                            "/following-sibling::*//div[@title='" + buttonTitle + "']"
            ).first();
            button.waitFor(new Locator.WaitForOptions().setTimeout(4000));
            button.click();
            return;
        } catch (Exception ignored) {}

        // Strategy 2: div[title='Search'] anywhere after the label (broader search)
        try {
            Locator button = page.locator(
                    "xpath=" + scopeXpath +
                            "//label[contains(@class,'vr-control-label') and normalize-space()='" + fieldLabel + "']" +
                            "/following::div[@title='" + buttonTitle + "'][1]"
            ).first();
            button.waitFor(new Locator.WaitForOptions().setTimeout(4000));
            button.click();
            return;
        } catch (Exception ignored) {}

        // Strategy 3: span[title contains 'number'] → mdi-magnify icon inside it
        try {
            Locator button = page.locator(
                    "xpath=" + scopeXpath +
                            "//span[@title and contains(translate(@title,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + fieldLabel.toLowerCase() + "')]" +
                            "//span[contains(@class,'mdi-magnify')]"
            ).first();
            button.waitFor(new Locator.WaitForOptions().setTimeout(4000));
            button.click();
            return;
        } catch (Exception ignored) {}

        // Strategy 4: vr-button[type='Search'] → inner div (Angular component approach)
        try {
            Locator button = page.locator(
                    "xpath=" + scopeXpath +
                            "//label[contains(@class,'vr-control-label') and normalize-space()='" + fieldLabel + "']" +
                            "/following::vr-button[@type='" + buttonTitle + "'][1]//*[@ng-click]"
            ).first();
            button.waitFor(new Locator.WaitForOptions().setTimeout(4000));
            button.click();
            return;
        } catch (Exception ignored) {}

        logger.warning("Labeled button press failed for: " + buttonTitle + " / " + fieldLabel);
    }

    /**
     * Finds clickable element by visible text.
     */
    private Locator findElementByText(String text) {

        page.waitForTimeout(300);

        // Try multiple XPath strategies in order of specificity

        String[] xpaths = {
                // 1. Direct text match on any element
                "//*[normalize-space(text())='" + text + "']",
                // 2. Button whose direct or descendant span/text matches (covers Angular-wrapped buttons)
                "//button[normalize-space(.)='" + text + "']",
                // 3. btn-primary/btn-success class buttons — typical wizard Next/Submit buttons
                "//button[contains(@class,'btn-primary') and normalize-space(.)='" + text + "']",
                "//button[contains(@class,'btn-success') and normalize-space(.)='" + text + "']",
                // 4. vr-button rendered div with matching title or text
                "//div[@title='" + text + "' and @ng-click]",
                // 5. Any clickable element with matching aria-label
                "//*[@aria-label='" + text + "']",
                // 6. Span inside button matching text (covers <button><span>Next</span></button>)
                "//button[.//span[normalize-space(text())='" + text + "']]",
                // 7. Contains match as last resort
                "//button[contains(normalize-space(.),'" + text + "') and contains(@class,'btn')]"
        };


        for (String xpath : xpaths) {
            try {
                List<Locator> elements = page.locator("xpath=" + xpath).all();
                for (Locator element : elements) {
                    try {
                        if (element.isVisible() && element.isEnabled()) {
                            return element;
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Checks if text is part of CRM additional options.
     */
    private boolean isAdditionalOption(String text) {

        for (String option : ADDITIONAL_OPTIONS) {
            if (option.equals(text)) {
                return true;
            }
        }
        return false;
    }
}