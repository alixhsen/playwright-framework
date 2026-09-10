package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnderRowHandler {

    private static final Logger logger = Logger.getLogger(UnderRowHandler.class.getName());
    private final Page page;

    public UnderRowHandler(Page page) {
        this.page = page;
    }

    public void underRow(String testStep) {
        Pattern pattern = Pattern.compile("(\\d+)-Under the (.+?) row, press on the (\\w+) button\\.");
        Matcher matcher = pattern.matcher(testStep);
        if (!matcher.find()) return;

        String rowDescription = matcher.group(2);
        String buttonAction   = matcher.group(3);

        try {
            List<Locator> buttons = page.locator(
                "xpath=//div[@class='vr-datagrid-body normal-full-screen']" +
                "//span[contains(text(),'" + rowDescription + "')]" +
                "/ancestor::div[@class='vr-datagrid-body normal-full-screen']" +
                "//vr-button[@type='" + buttonAction + "']"
            ).all();

            if (buttons.size() >= 2) {
                buttons.get(buttons.size() - 1).click();
            }
        } catch (Exception e) {
            logger.warning("UnderRow failed: " + e.getMessage());
        }
    }
}
