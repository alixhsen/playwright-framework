package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.List;
import java.util.logging.Logger;

public class AdvanceFilterValueHandler {

    private static final Logger logger = Logger.getLogger(AdvanceFilterValueHandler.class.getName());
    private final Page page;

    public AdvanceFilterValueHandler(Page page) {
        this.page = page;
    }

    public void set(String testStep) {
        int toPosition = testStep.indexOf(" to ");
        if (toPosition == -1) return;

        String selectedValue = testStep.substring(toPosition + 4)
                .trim().replaceAll("^\"|\"$", "").trim();

        int modalCount = page.locator(".modal-content").count();
        Locator targetModal = null;

        if (modalCount > 1) {
            List<Locator> modals = page.locator(".modal-content").all();
            for (Locator modal : modals) {
                if (modal.innerText().contains("Advanced Filter")) {
                    targetModal = modal;
                    break;
                }
            }
        } else {
            Locator modal = page.locator(".modal-content").first();
            if (modal.count() > 0 && modal.innerText().contains("Advanced Filter")) {
                targetModal = modal;
            }
        }

        if (targetModal != null) {
            setValueInModal(targetModal, selectedValue);
        }
    }

    private void setValueInModal(Locator modal, String value) {
        try {
            Locator requiredDiv = modal.locator("xpath=//div[contains(@class,'required-inpute')]//input").first();
            String cls = requiredDiv.getAttribute("class");
            if (cls != null && cls.contains("vr-date")) {
                requiredDiv.click();
                requiredDiv.evaluate("el => el.value = ''");
                requiredDiv.fill(value);
                requiredDiv.press("Enter");
            } else {
                requiredDiv.fill(value);
            }
        } catch (Exception e) {
            try {
                String[] choices = value.split("\\*\\*");
                Locator dropdown = modal.locator("xpath=.//vr-select[contains(.,'Select...')]").first();
                dropdown.click();
                for (String choice : choices) {
                    Locator searchInput = modal.locator("#filterInput").first();
                    searchInput.clear();
                    searchInput.fill(choice.trim());
                    Locator result = page.locator("xpath=//*/a/div[1]").first();
                    result.click();
                }
                page.locator("body").click();
            } catch (Exception ex) {
                logger.warning("AdvanceFilterValueHandler set failed: " + ex.getMessage());
            }
        }
    }
}
