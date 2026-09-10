package uitesting.core;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import uitesting.handlers.*;
import uitesting.utils.DatabaseHelper;

import java.util.logging.Logger;

public class TestStepProcessor {

    private static final Logger logger = Logger.getLogger(TestStepProcessor.class.getName());

    private final Page page;
    private final String loginUrl;
    private final String loginEmail;
    private final String loginPassword;
    private String executedQuery;

    // Handlers
    private final NavigationHandler     navigationHandler;
    private final ButtonHandler         buttonHandler;
    private final InsertHandler         insertHandler;
    private final DropdownHandler       dropdownHandler;
    private final EnableToggleButtonHandler toggleHandler;
    private final GroupHandler          groupHandler;
    private final SubscribeToHandler        subscribeToHandler;
    private final SelectPhoneNumberHandler selectPhoneNumberHandler;

    public TestStepProcessor(Page page, String loginUrl, String loginEmail, String loginPassword) {
        this.page          = page;
        this.loginUrl      = loginUrl;
        this.loginEmail    = loginEmail;
        this.loginPassword = loginPassword;

        this.navigationHandler = new NavigationHandler(page);
        this.buttonHandler     = new ButtonHandler(page);
        this.insertHandler     = new InsertHandler(page);
        this.dropdownHandler   = new DropdownHandler(page);
        this.toggleHandler     = new EnableToggleButtonHandler(page);
        this.groupHandler      = new GroupHandler(page);
        this.subscribeToHandler  = new SubscribeToHandler(page);
        this.selectPhoneNumberHandler = new SelectPhoneNumberHandler(page);
    }

    /** Sets the SQL query to run when a "Run Database Query" step is encountered. */
    public void setExecutedQuery(String query) {
        this.executedQuery = query;
    }

    /**
     * Processes a single test step string.
     */
    public void process(String testStep) {
        if (testStep == null || testStep.isBlank()) return;
        testStep = testStep.replaceFirst("^\\d+-", "").trim();

        logger.info("Processing step: " + testStep);

        try {
            if (testStep.contains("Login")) {
                handleLogin();

            } else if (testStep.contains("Go to")) {
                navigationHandler.goToStep(testStep);

            } else if (testStep.contains("In") && testStep.contains("Edit Filter")) {
                new EditFilterHandler(page).editFilter(testStep);

            } else if (testStep.contains("Press")
                    && !testStep.contains("In Group")
                    && !testStep.contains("Under")
                    && !testStep.contains("section")) {
                buttonHandler.buttonPress(testStep);

            } else if (testStep.contains("In Advanced Filter") && testStep.contains("set")) {
                new AdvanceFilterValueHandler(page).set(testStep);

            } else if (testStep.toLowerCase().contains("logout")
                    || testStep.toLowerCase().contains("log out")) {
                handleLogout();
            } else if (testStep.toLowerCase().contains("select phone number")) {
                // "Select Phone Number 01379481."  →  extract "01379481"
                String number = testStep.substring("Select Phone Number".length()).trim();
                if (number.endsWith(".")) number = number.substring(0, number.length() - 1).trim();
                selectPhoneNumberHandler.handle(number);

            } else if (testStep.contains("Select")
                    && !testStep.contains("In Group")
                    && !testStep.contains("Query")) {
                dropdownHandler.dropdown(testStep);

            } else if (testStep.contains("Insert")
                    && !testStep.contains("In Group")) {
                insertHandler.insert(testStep);

            } else if (testStep.contains("In Group")) {
                groupHandler.handleGroup(testStep);

            } else if (testStep.contains("Enable") || testStep.contains("Disable")) {
                toggleHandler.enableToggleButton(testStep);

            } else if (testStep.contains("Drag")) {
                new DragAndDropHandler(page).dragAndDrop(testStep);

            } else if (testStep.contains("Upload")) {
                new UploadHandler(page).upload(testStep);

            } else if (testStep.contains("Export")) {
                new ExportHandler(page).export(testStep);

            } else if (testStep.contains("Drill")) {
                new DrillDownHandler(page).drillDown(testStep);

            } else if (testStep.contains("Under")) {
                new UnderRowHandler(page).underRow(testStep);

            } else if (testStep.contains("Bussiness Process") || testStep.contains("Business Process")) {
                new BPHandler(page).closeBp(testStep);

            } else if (testStep.contains("View") && testStep.contains("360")) {
                new View360Handler(page).view(testStep);

            } else if (testStep.contains("section") && testStep.contains("Press")) {
                new SectionHandler(page).handleSectionPress(testStep);

            } else if (testStep.contains("Wait") && testStep.contains("Work Order")) {
                new WaitWorkOrderHandler(page).waitForWorkOrder(testStep);

            } else if (testStep.contains("Run") && testStep.contains("Database")
                    && testStep.contains("Query") && executedQuery != null) {
                new DatabaseQueriesAction().executeQuery(executedQuery,
                        uitesting.utils.ConfigManager.getInstance().getDbConnectionString());
            } else if (testStep.toLowerCase().contains("subscribe to")) {
                // Extract child name — everything after "Subscribe to "
                // e.g. "Subscribe to Line."  →  "Line"
                String child = testStep.substring("Subscribe to".length()).trim();
                // Strip trailing period if present
                if (child.endsWith(".")) child = child.substring(0, child.length() - 1).trim();
                subscribeToHandler.handle(child);
            }else {
                logger.warning("No handler matched for step: " + testStep);
            }

        } catch (Exception e) {
            logger.severe("Step failed [" + testStep + "]: " + e.getMessage());
            throw e;
        }
    }

    // ─── Login ──────────────────────────────────────────────────────────────

    private void handleLogin() {
        page.navigate(loginUrl);

        page.locator("#validator-container > div:nth-child(1) > input")
                .waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                        .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));
        page.locator("#validator-container > div:nth-child(1) > input").fill(loginEmail);
        page.locator("#validator-container > div:nth-child(1) > form > input").fill(loginPassword);

        // Click the submit button (second btn element)
        page.locator(".btn").all().get(1).click();

        new LoadingPageHandler(page).waitForPageLoad();
        logger.info("Logged in as: " + loginEmail);
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    private void handleLogout() {

        try {
            logger.info("Logging out from CRM...");

            Locator userMenuButton = page.locator("#page-header-user-dropdown");
            userMenuButton.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            userMenuButton.click();

            Locator logoutButton = page.locator(
                    "xpath=//a[contains(@class,'dropdown-item') and normalize-space()='Logout']"
            );

            logoutButton.waitFor(new Locator.WaitForOptions().setTimeout(5000));

            logoutButton.click();

            page.waitForLoadState();

            logger.info("Successfully logged out from CRM.");

        } catch (Exception e) {
            logger.warning("Logout failed: " + e.getMessage());
        }
    }
}
