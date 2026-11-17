package dev.jbaby.shadwick.util;

import dev.jbaby.shadwick.components.SwDialog;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** One check, one message, for factories and fixed slots alike. */
class ComponentIdsTest {
    final WicketTester tester = new WicketTester();

    @AfterEach void stop() { tester.destroy(); }

    @Test void aFactoryComponentMustUseTheIdItWasGiven() {
        Label label = new Label("step");
        assertThat(ComponentIds.fromFactory(label, "step", "A wizard step")).isSameAs(label);
        assertThatIllegalArgumentException().isThrownBy(() -> ComponentIds.fromFactory(new Label("other"), "step", "A wizard step"))
                .withMessage("A wizard step must use the id its factory is given (\"step\"), but was \"other\"");
        assertThatIllegalArgumentException().isThrownBy(() -> ComponentIds.fromFactory(null, "step", "A wizard step"))
                .withMessageEndingWith("but was null");
    }

    @Test void aSlotComponentMustUseItsFixedIdAndNullIsReportedTheSameWay() {
        SwDialog dialog = new SwDialog("dialog", "Title");
        assertThatIllegalArgumentException().isThrownBy(() -> dialog.setContent(new Label("body")))
                .withMessage("The dialog's content (SwDialog.CONTENT_ID) must use the id \"content\", but was \"body\"");
        // Was a NullPointerException
        assertThatIllegalArgumentException().isThrownBy(() -> dialog.setContent(null)).withMessageEndingWith("but was null");
    }
}
