package de.bjarne.ahfakewarner;

import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import de.bjarne.ahfakewarner.hook.SlotHook;
import de.bjarne.ahfakewarner.hook.TooltipHook;
import java.util.Collections;
import java.util.List;
import net.labymod.api.LabyModAddon;
import net.labymod.api.events.RenderIngameOverlayEvent;
import net.labymod.settings.elements.SettingsElement;

/**
 * Einstiegspunkt des LabyMod-3-Addons.
 *
 * Die Tooltip-Warnung selbst läuft über den ASM-Eingriff in {@code ItemStack.getTooltip}
 * (siehe {@code AhFakeWarnerTransformer}); hier wird nur die Prüflogik bereitgestellt.
 */
public class AhFakeWarnerAddon extends LabyModAddon {

  static final String PREFIX = "§c[AH-Fake-Warner] §f";

  // Standardwerte wie im LabyMod-4-Addon; Einstellungen folgen in Etappe 4
  private static final List<String> DEFAULT_WHITELIST = Collections.singletonList("AbgegrieftHD");
  private static final int DEFAULT_THRESHOLD = 2;

  /** Merkt sich, ob der Status in dieser Sitzung schon im Chat stand. */
  private boolean statusShown;

  @Override
  public void onEnable() {
    TooltipHook.setWarner(new TooltipWarner(DEFAULT_WHITELIST, DEFAULT_THRESHOLD,
        SignatureExtractor.DEFAULT_PATTERN, true));
    System.out.println("[AH-Fake-Warner] Addon geladen, Tooltip-Eingriff "
        + (TooltipHook.isInstalled() ? "aktiv" : "fehlt") + ", Slot-Eingriff "
        + (SlotHook.isInstalled() ? "aktiv" : "fehlt"));

    // Es gibt kein Tick- oder Welt-betreten-Event; das Overlay wird aber nur
    // im Spiel gezeichnet. Beim ersten Mal im Spiel einmalig den Status zeigen.
    getApi().getEventManager().register(new RenderIngameOverlayEvent() {
      @Override
      public void onRender(float partialTicks) {
        if (statusShown || !getApi().isIngame()) {
          return;
        }
        statusShown = true;
        if (TooltipHook.isInstalled() && SlotHook.isInstalled()) {
          getApi().displayMessageInChat(PREFIX + "Tooltip-Warnung und Slot-Rahmen aktiv.");
        } else {
          getApi().displayMessageInChat(PREFIX + "§cNicht alles konnte eingebaut werden"
              + " (Tooltip: " + (TooltipHook.isInstalled() ? "ja" : "nein")
              + ", Rahmen: " + (SlotHook.isInstalled() ? "ja" : "nein") + ").");
        }
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
