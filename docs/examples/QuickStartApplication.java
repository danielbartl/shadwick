package dev.example;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.Skin;
import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

public class QuickStartApplication extends WebApplication {
    @Override
    public Class<? extends Page> getHomePage() {
        return QuickStartPage.class;
    }

    @Override
    protected void init() {
        super.init();
        Shadwick.install(this).setSkin(Skin.VEGA);
        getMarkupSettings().setStripWicketTags(true);
        mountPage("/start", QuickStartPage.class);
    }
}
