package de.bjarne.ahfakewarner;

import java.util.List;

import net.labymod.api.LabyModAddon;
import net.labymod.api.events.RenderIngameOverlayEvent;
import net.labymod.settings.elements.SettingsElement;

/**
 * Einstiegspunkt des LabyMod-3-Addons.
 *
 * Etappe 1 (Spike): Prüft nur, ob das Addon geladen wird, und schreibt
 * beim ersten Betreten einer Welt eine Zeile in den Chat.
 */
public class AhFakeWarnerAddon extends LabyModAddon {

    static final String PREFIX = "§c[AH-Fake-Warner] §f";

    /** Merkt sich, ob die Begrüßung in dieser Sitzung schon ausgegeben wurde. */
    private boolean greeted;

    @Override
    public void onEnable() {
        System.out.println("[AH-Fake-Warner] Addon geladen (LabyMod 3)");

        // Es gibt kein Tick- oder Welt-betreten-Event; das Overlay wird aber nur
        // im Spiel gezeichnet. Beim ersten Mal im Spiel einmalig grüßen.
        getApi().getEventManager().register(new RenderIngameOverlayEvent() {
            @Override
            public void onRender(float partialTicks) {
                if (greeted || !getApi().isIngame()) {
                    return;
                }
                greeted = true;
                getApi().displayMessageInChat(PREFIX + "Hallo! Das Addon läuft in LabyMod 3.");
            }
        });
    }

    @Override
    public void loadConfig() {
        // Noch keine Einstellungen (kommt in Etappe 4).
    }

    @Override
    protected void fillSettings(List<SettingsElement> list) {
        // Noch keine Einstellungen (kommt in Etappe 4).
    }
}
