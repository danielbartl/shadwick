package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.SwUserMenu;

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/** An explicit demo entry point, without collecting credentials. */
public class UserSignInPage extends BasePage {

    public UserSignInPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(
                new SwUserMenu("account", UserMenuPage::name)
                        .setPlacement(SwUserMenu.Placement.HEADER)
                        .setSignInPage(UserSignInPage.class));
        Link<Void> enter =
                new Link<>("enter") {

                    @Override
                    public void onClick() {
                        UserMenuPage.signIn();
                        setResponsePage(UserMenuPage.class);
                    }
                };
        enter.add(Sw.Behaviors.button());
        add(enter);
    }

    @Override
    protected String getTitle() {
        return "Demo sign in";
    }
}
