package dev.jbaby.shadwick;

import dev.jbaby.shadwick.components.SwButton;
import org.apache.wicket.markup.html.WebPage;

/** A page whose button switches the user to lyra over Ajax and re-renders itself. */
public class SkinSwitchPage extends WebPage {

    public SkinSwitchPage() {
        SwButton button = SwButton.onClick("switch", "Lyra", target -> {
            ShadwickSettings.get().setCurrentSkin(Skin.LYRA);
            target.add(target.getPage().get("switch"));
        });
        button.setOutputMarkupId(true);
        add(button);
    }
}
