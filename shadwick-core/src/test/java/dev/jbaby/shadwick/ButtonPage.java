package dev.jbaby.shadwick;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;

/** A page with one Shadwick-styled link. */
public class ButtonPage extends WebPage {

    public ButtonPage() {
        add(new Link<Void>("link") {
            @Override
            public void onClick() {
            }
        }.add(Sw.Behaviors.button()));
    }
}
