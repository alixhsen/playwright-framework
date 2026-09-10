package uitesting.handlers;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.logging.Logger;

public class DragAndDropHandler {

    private static final Logger logger = Logger.getLogger(DragAndDropHandler.class.getName());
    private final Page page;

    public DragAndDropHandler(Page page) {
        this.page = page;
    }

    public void dragAndDrop(String testStep) {
        int idx = testStep.indexOf("Drag & Drop");
        String dragText = testStep.substring(idx + 11).trim().replaceAll("\\.$", "").trim();

        try {
            Locator source = page.locator("[title='" + dragText + "']").first();
            Locator target = page.locator(
                "xpath=/html/body/div[2]/div/div/div[2]/div[2]/vr-modalbody/div/vr-form/div/" +
                "vr-validation-group/vr-tabs/vr-tab[3]/vr-row/div/vr-columns/div/vr-validation-group/" +
                "div/vr-directivewrapper/vr-rules-normalizenumbersettings/div/vr-row[2]/div/vr-columns/" +
                "div/div[2]/div/vr-validator/div/div[1]/vr-datagrid/vr-datagridrows/div[1]/div/div[2]/div[1]/div[2]/div/div"
            ).first();
            source.dragTo(target);
        } catch (Exception e) {
            logger.warning("Drag & Drop failed: " + e.getMessage());
        }
    }
}
