package dev.yanianz.essentials.terms;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Shows the terms inside a native minecraft dialog screen, the same ui the
 * vanilla client uses for server links and report screens.
 *
 * Requires paper 1.21.7+, the caller falls back to its chest interface
 * when this returns false.
 */
public final class TermsDialogs {

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private TermsDialogs() {
    }

    /**
     * Opens the dialog screen for the player.
     *
     * @param onAccept executed when the player clicks the accept button,
     *                 returning true marks the terms as accepted
     * @param onDeny   executed when the player clicks the refuse button
     * @return true when the dialog was shown, false when dialogs are unsupported.
     */
    public static boolean show(Player player, String titleLegacy, List<String> ruleLinesLegacy, String questionLegacy,
                               String acceptLabelLegacy, String denyLabelLegacy, String acceptHoverLegacy, String denyHoverLegacy,
                               Supplier<Boolean> onAccept, Runnable onDeny) {

        Component title = LEGACY.deserialize(colorize(titleLegacy));

        List<DialogBody> bodies = new ArrayList<>();
        if (questionLegacy != null && !questionLegacy.isBlank()) {
            bodies.add(DialogBody.plainMessage(LEGACY.deserialize(colorize(questionLegacy))));
        }
        for (String line : ruleLinesLegacy) {
            String colorized = colorize(line);
            if (!colorized.isBlank()) {
                bodies.add(DialogBody.plainMessage(LEGACY.deserialize(colorized)));
            }
        }

        DialogBase base = DialogBase.create(title, null, true, false,
                DialogBase.DialogAfterAction.CLOSE, bodies, List.of());

        // The options parameter is required, a null lifetime crashes the callback
        net.kyori.adventure.text.event.ClickCallback.Options callbackOptions =
                net.kyori.adventure.text.event.ClickCallback.Options.builder().build();

        ActionButton acceptButton = ActionButton.create(
                LEGACY.deserialize(colorize(acceptLabelLegacy)),
                LEGACY.deserialize(colorize(acceptHoverLegacy)),
                150,
                DialogAction.customClick((response, audience) -> onAccept.get(), callbackOptions));

        ActionButton denyButton = ActionButton.create(
                LEGACY.deserialize(colorize(denyLabelLegacy)),
                LEGACY.deserialize(colorize(denyHoverLegacy)),
                150,
                DialogAction.customClick((response, audience) -> onDeny.run(), callbackOptions));

        Dialog dialog = Dialog.create(builder -> {
            io.papermc.paper.registry.data.dialog.DialogRegistryEntry.Builder entry = builder.empty();
            entry.base(base);
            entry.type(DialogType.multiAction(List.of(acceptButton, denyButton), null, 2));
        });

        player.showDialog(dialog);
        return true;
    }

    private static String colorize(String text) {
        return dev.yanianz.essentials.util.ColorUtil.sections(text);
    }
}
