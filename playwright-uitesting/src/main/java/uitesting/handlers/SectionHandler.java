package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SectionHandler {

    private static final Logger logger = Logger.getLogger(SectionHandler.class.getName());
    private final Page page;

    public SectionHandler(Page page) {
        this.page = page;
    }

    public void handleSectionPress(String testStep) {
        if (testStep.contains("Button")) {
            handleSectionButtonPress(testStep);
        } else {
            handleSectionLabelPress(testStep);
        }
    }

    private void handleSectionButtonPress(String testStep) {
        Pattern pattern = Pattern.compile("In (.+?) section, Press on the (\\d+)(?:st|nd|rd|th) (.+?) Button\\.");
        Matcher matcher = pattern.matcher(testStep);
        if (!matcher.find()) return;

        String sectionName = matcher.group(1);
        int buttonNumber = Integer.parseInt(matcher.group(2));
        String buttonName = matcher.group(3);

        try {
            String css = "vr-section[header='" + sectionName + "'] vr-button[type='" + buttonName + "'] span.btn-label";
            List<Locator> elements = page.locator(css).all();
            List<Locator> visible = elements.stream()
                    .filter(e -> {
                        try { return e.isVisible(); } catch (Exception ex) { return false; }
                    })
                    .collect(Collectors.toList());

            if (buttonNumber <= visible.size()) {
                visible.get(buttonNumber - 1).click();
            }
        } catch (Exception e) {
            logger.warning("Section button press failed: " + e.getMessage());
        }
    }

    private void handleSectionLabelPress(String testStep) {
        Pattern pattern = Pattern.compile("In (.+?) (\\d) section, Press on (.+?)\\.");
        Matcher matcher = pattern.matcher(testStep);
        if (!matcher.find()) return;

        String sectionName = matcher.group(1);
        int sectionNumber = Integer.parseInt(matcher.group(2));
        String buttonName = matcher.group(3);

        try {
            String xpath = "//vr-section[@title='" + sectionName + "']//label[contains(.,'" + buttonName + "')]//span";
            List<Locator> elements = page.locator("xpath=" + xpath).all();

            if (sectionNumber <= elements.size()) {
                Locator el = elements.get(sectionNumber - 1);
                el.evaluate("el => el.scrollIntoView(true)");
                el.click();
            }
        } catch (Exception e) {
            logger.warning("Section label press failed: " + e.getMessage());
        }
    }
}
