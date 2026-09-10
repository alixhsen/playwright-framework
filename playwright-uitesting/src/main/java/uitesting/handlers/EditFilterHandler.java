package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditFilterHandler {

    private static final Logger logger = Logger.getLogger(EditFilterHandler.class.getName());
    private final Page page;

    public EditFilterHandler(Page page) {
        this.page = page;
    }

    public void editFilter(String testStep) {
        Pattern pattern = Pattern.compile("(?<=In )(.*?)(?=,)|(?<=press on )(.*?)(?=\\.)");
        Matcher matcher = pattern.matcher(testStep);

        String sectionTitle = "";
        while (matcher.find()) {
            if (matcher.group(1) != null) sectionTitle = matcher.group(1).trim();
        }

        try {
            Locator section = page.locator("xpath=//vr-section[@title='" + sectionTitle + "']").first();
            Locator editBtn = section.locator("xpath=.//button[contains(.,'Edit Filter')]").first();
            editBtn.click();
        } catch (Exception e) {
            logger.warning("Edit filter failed: " + e.getMessage());
        }
    }
}
