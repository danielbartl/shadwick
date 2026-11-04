package dev.jbaby.shadwick;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.AlertBehavior;
import dev.jbaby.shadwick.behaviors.BadgeBehavior;
import dev.jbaby.shadwick.behaviors.CardBehavior;
import dev.jbaby.shadwick.behaviors.FeedbackToastsBehavior;
import dev.jbaby.shadwick.behaviors.SeparatorBehavior;
import dev.jbaby.shadwick.behaviors.SkeletonBehavior;
import dev.jbaby.shadwick.behaviors.SpinnerBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.behaviors.SwitchBehavior;
import dev.jbaby.shadwick.behaviors.ToggleBehavior;
import dev.jbaby.shadwick.behaviors.TooltipBehavior;
import dev.jbaby.shadwick.behaviors.ToastBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import dev.jbaby.shadwick.behaviors.ToggleGroupBehavior;
import dev.jbaby.shadwick.behaviors.InputGroupBehavior;
import dev.jbaby.shadwick.behaviors.InputGroupAddonBehavior;
import dev.jbaby.shadwick.behaviors.InputGroupButtonBehavior;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.util.SerializableConsumer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;

/**
 * Shortcuts for Shadwick behaviors ({@link Behaviors}) and for the components driven by a callback
 * ({@link Components}, also available as static factories on the classes themselves, e.g.
 * {@link SwButton#onClick(String, String, SerializableConsumer)}). Every other component is created with its
 * constructor, like any Wicket component. {@link #toast(IPartialPageRequestHandler, String)}
 * sends a direct notification after the page adds {@code Sw.Behaviors.toasts()}.
 */
public final class Sw {

    private Sw() {
    }

    public static void toast(IPartialPageRequestHandler target, String message) { ToastBehavior.toast(target, message); }
    public static void toast(IPartialPageRequestHandler target, String message, ToastBehavior.Variant variant) {
        ToastBehavior.toast(target, message, variant);
    }
    public static void toast(IPartialPageRequestHandler target, String message, ToastBehavior.Variant variant, String title) {
        ToastBehavior.toast(target, message, variant, title);
    }
    public static void toast(IPartialPageRequestHandler target, String message, ToastBehavior.Variant variant, String title,
                             java.time.Duration duration) {
        ToastBehavior.toast(target, message, variant, title, duration);
    }

    /** Factory for Shadwick behaviors, which style an existing Wicket component. */
    public static final class Behaviors {

        private Behaviors() {
        }

        public static ButtonBehavior button() {
            return new ButtonBehavior();
        }

        public static ButtonBehavior button(ButtonBehavior.Variant variant) {
            return new ButtonBehavior(variant);
        }

        public static CardBehavior card() { return new CardBehavior(); }
        public static BadgeBehavior badge() { return new BadgeBehavior(); }
        public static BadgeBehavior badge(BadgeBehavior.Variant variant) { return new BadgeBehavior(variant); }
        public static BadgeBehavior badge(IModel<BadgeBehavior.Variant> variant) { return new BadgeBehavior(variant); }
        public static AlertBehavior alert() { return new AlertBehavior(); }
        public static AlertBehavior alert(AlertBehavior.Variant variant) { return new AlertBehavior(variant); }
        public static AlertBehavior alert(IModel<AlertBehavior.Variant> variant) { return new AlertBehavior(variant); }
        public static SeparatorBehavior separator() { return new SeparatorBehavior(); }
        public static StyledBehavior kbd() { return new StyledBehavior("sw-kbd"); }
        public static SkeletonBehavior skeleton() { return new SkeletonBehavior(); }
        public static SkeletonBehavior skeleton(SkeletonBehavior.Shape shape) { return new SkeletonBehavior(shape); }
        public static SpinnerBehavior spinner() { return new SpinnerBehavior(); }
        public static SpinnerBehavior spinner(SpinnerBehavior.Size size) { return new SpinnerBehavior(size); }
        public static ToggleBehavior toggle() { return new ToggleBehavior(); }
        public static TooltipBehavior tooltip(String label) { return new TooltipBehavior(label); }
        public static TooltipBehavior tooltip(IModel<String> label) { return new TooltipBehavior(label); }
        public static DropdownMenuBehavior dropdownMenu(Component trigger) { return new DropdownMenuBehavior(trigger); }
        public static DropdownMenuItemBehavior dropdownMenuItem() { return new DropdownMenuItemBehavior(); }
        public static DropdownMenuItemBehavior dropdownMenuItem(DropdownMenuItemBehavior.Variant variant) { return new DropdownMenuItemBehavior(variant); }
        public static DropdownMenuItemBehavior dropdownMenuCheckboxItem(IModel<Boolean> checked) { return DropdownMenuItemBehavior.checkbox(checked); }
        public static DropdownMenuItemBehavior dropdownMenuRadioItem(IModel<Boolean> checked) { return DropdownMenuItemBehavior.radio(checked); }
        public static StyledBehavior dropdownMenuLabel() { return new StyledBehavior("sw-dropdown-menu-label"); }
        public static StyledBehavior dropdownMenuShortcut() { return new StyledBehavior("sw-dropdown-menu-shortcut"); }
        public static StyledBehavior dropdownMenuSeparator() { return menuPart("sw-dropdown-menu-separator", "separator"); }
        public static StyledBehavior dropdownMenuGroup() { return menuPart("sw-dropdown-menu-group", "group"); }
        private static StyledBehavior menuPart(String css, String role) {
            return new StyledBehavior(css) {
                @Override public void onComponentTag(Component component, ComponentTag tag) {
                    super.onComponentTag(component, tag); tag.put("role", role);
                }
            };
        }
        public static ToggleGroupBehavior toggleGroup() { return new ToggleGroupBehavior(); }
        public static InputGroupBehavior inputGroup() { return new InputGroupBehavior(); }
        public static InputGroupAddonBehavior inputGroupAddon() { return new InputGroupAddonBehavior(); }
        public static InputGroupButtonBehavior inputGroupButton() { return new InputGroupButtonBehavior(); }
        public static StyledBehavior inputGroupInput() { return new StyledBehavior("sw-input-group-input"); }
        public static StyledBehavior inputGroupTextArea() { return new StyledBehavior("sw-input-group-textarea"); }
        public static StyledBehavior inputGroupText() { return new StyledBehavior("sw-input-group-text"); }
        public static SwitchBehavior swSwitch() { return new SwitchBehavior(); }
        public static FeedbackToastsBehavior feedbackToasts() { return new FeedbackToastsBehavior(); }
        public static ToastBehavior toasts() { return new ToastBehavior(); }
        public static FeedbackToastsBehavior feedbackToasts(org.apache.wicket.feedback.IFeedbackMessageFilter filter) {
            return new FeedbackToastsBehavior(filter);
        }
    }

    /** Factory for the Shadwick components driven by a callback. */
    public static final class Components {

        private Components() {
        }

        public static SwButton button(String id, String label, SerializableConsumer<AjaxRequestTarget> onClick) {
            return SwButton.onClick(id, label, onClick);
        }

        public static SwButton button(String id, IModel<String> label, SerializableConsumer<AjaxRequestTarget> onClick) {
            return SwButton.onClick(id, label, onClick);
        }

        public static SwSubmitButton submitButton(String id, String label, SerializableConsumer<AjaxRequestTarget> onSubmit) {
            return SwSubmitButton.onSubmit(id, label, onSubmit);
        }

        public static SwSubmitButton submitButton(String id, IModel<String> label, SerializableConsumer<AjaxRequestTarget> onSubmit) {
            return SwSubmitButton.onSubmit(id, label, onSubmit);
        }

        public static SwSubmitButton submitButton(String id, String label, SerializableConsumer<AjaxRequestTarget> onSubmit,
                                                  SerializableConsumer<AjaxRequestTarget> onError) {
            return SwSubmitButton.onSubmit(id, label, onSubmit, onError);
        }

        public static SwSubmitButton submitButton(String id, IModel<String> label, SerializableConsumer<AjaxRequestTarget> onSubmit,
                                                  SerializableConsumer<AjaxRequestTarget> onError) {
            return SwSubmitButton.onSubmit(id, label, onSubmit, onError);
        }
    }
}
