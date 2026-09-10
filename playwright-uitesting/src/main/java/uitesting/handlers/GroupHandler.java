package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Handles "In Group N, ..." test steps.
 */
public class GroupHandler {

    private static final Logger logger =
            Logger.getLogger(GroupHandler.class.getName());

    private final Page page;

    public GroupHandler(Page page) {
        this.page = page;
    }

    public void handleGroup(String testStep) {

        try {

            if (testStep.contains("Press")) {
                handleGroupPress(testStep);

            } else if (testStep.contains("Search & Select")) {
                handleGroupSearchSelect(testStep);
            }

        } catch (Exception e) {
            logger.warning("GroupHandler failed: " + e.getMessage());
        }

        page.waitForTimeout(500);
    }

    /* ───────────────────────────────────────────── */

    private void handleGroupPress(String testStep) {

        Pattern pattern =
                Pattern.compile("(\\d+)-In Group(\\d+), Press on (\\w+)\\.");

        Matcher matcher = pattern.matcher(testStep);

        if (!matcher.find()) return;

        int groupNumber = Integer.parseInt(matcher.group(2));
        String action = matcher.group(3);

        List<Locator> groups =
                page.locator("vr-row[ng-repeat='group in ctrl.groups']").all();

        if (groupNumber > groups.size()) return;

        Locator groupEl = groups.get(groupNumber - 1);

        try {
            groupEl.locator("xpath=.//button[contains(.,'" + action + "')]")
                    .first()
                    .click();

        } catch (Exception ex) {

            try {
                groupEl.locator(
                        "xpath=.//vr-choice[label/span[contains(@class,'ng-binding') and normalize-space(.)='" + action + "']]"
                ).first().click();

            } catch (Exception innerEx) {
                logger.warning("Group button press failed for action: " + action);
            }
        }
    }

    /* ───────────────────────────────────────────── */

    private void handleGroupSearchSelect(String testStep) {

        Pattern pattern =
                Pattern.compile("Group (\\d+).*?row (\\d+).*?for (.+?)\\.");

        Matcher matcher = pattern.matcher(testStep);

        if (!matcher.find()) return;

        int groupNumber = Integer.parseInt(matcher.group(1));
        int rowNumber = Integer.parseInt(matcher.group(2));
        String fieldText = matcher.group(3);

        String[] choice = fieldText.split("\\*\\*");
        if (choice.length < 2) return;

        String label = choice[0].trim();

        List<Locator> groups =
                page.locator("vr-row[ng-repeat='group in ctrl.groups']").all();

        if (groupNumber > groups.size()) return;

        Locator groupEl = groups.get(groupNumber - 1);

        for (int i = 1; i < choice.length; i++) {

            String criteria = choice[i].trim();

            try {

                String cssSelector =
                        "vr-select[label='" + label + "'] button.vr-dropdown-select";

                List<Locator> buttons =
                        groupEl.locator(cssSelector).all();

                if (rowNumber <= buttons.size()) {
                    buttons.get(rowNumber - 1).click();
                }

                Locator searchInput =
                        groupEl.locator(
                                "vr-select[label='" + label + "'] input.form-control"
                        ).first();

                searchInput.waitFor(
                        new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                );

                searchInput.fill(criteria);

                Locator result =
                        page.locator("vr-select ul.dropdown-menu li a")
                                .filter(new Locator.FilterOptions()
                                        .setHasText(criteria))
                                .first();

                result.waitFor(
                        new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                );

                result.click();

            } catch (Exception e) {
                logger.warning("Group search select failed: " + e.getMessage());
            }

            // close dropdown
            page.locator("body").click();
        }
    }
}
