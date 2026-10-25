package de.bjarne.ahfakewarner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.bjarne.ahfakewarner.hook.SlotHook;
import de.bjarne.ahfakewarner.hook.TooltipHook;
import java.util.List;
import net.labymod.api.LabyModAddon;
import net.labymod.api.events.RenderIngameOverlayEvent;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.settings.elements.HeaderElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.SliderElement;
import net.labymod.settings.elements.StringElement;
import net.labymod.utils.Consumer;
import net.labymod.utils.Material;

/**
 * Einstiegspunkt des LabyMod-3-Addons.
 *
 * Die Warnungen selbst laufen über die ASM-Eingriffe in {@code ItemStack.getTooltip} und
 * {@code GuiContainer.drawSlot} (siehe {@code AhFakeWarnerTransformer}); hier werden
 * Einstellungen geladen, gespeichert und an die Hooks übergeben.
 */
public class AhFakeWarnerAddon extends LabyModAddon {

  static final String PREFIX = "§c[AH-Fake-Warner] §f";

  private static final String KEY_ENABLED = "enabled";
  private static final String KEY_WHITELIST = "whitelist";
  private static final String KEY_THRESHOLD = "threshold";
  private static final String KEY_ICED_OUT = "icedOutCheck";
  private static final String KEY_SLOT_FRAME = "slotFrame";

  private final AddonSettings settings = new AddonSettings();

  /** Merkt sich, ob der Eingriffs-Status in dieser Sitzung schon geprüft wurde. */
  private boolean statusChecked;

  @Override
  public void onEnable() {
    System.out.println("[AH-Fake-Warner] Addon geladen, Tooltip-Eingriff "
        + (TooltipHook.isInstalled() ? "aktiv" : "fehlt") + ", Slot-Eingriff "
        + (SlotHook.isInstalled() ? "aktiv" : "fehlt"));

    // Es gibt kein Tick- oder Welt-betreten-Event; das Overlay wird aber nur im Spiel
    // gezeichnet. Beim ersten Mal im Spiel nur melden, falls ein Eingriff fehlt.
    getApi().getEventManager().register(new RenderIngameOverlayEvent() {
      @Override
      public void onRender(float partialTicks) {
        if (statusChecked || !getApi().isIngame()) {
          return;
        }
        statusChecked = true;
        if (!TooltipHook.isInstalled() || !SlotHook.isInstalled()) {
          getApi().displayMessageInChat(PREFIX + "§cNicht alles konnte eingebaut werden"
              + " (Tooltip: " + (TooltipHook.isInstalled() ? "ja" : "nein")
              + ", Rahmen: " + (SlotHook.isInstalled() ? "ja" : "nein") + ").");
        }
      }
    });
  }

  @Override
  public void loadConfig() {
    JsonObject config = getConfig();
    this.settings.enabled = readBoolean(config, KEY_ENABLED, true);
    this.settings.whitelist = readString(config, KEY_WHITELIST, AddonSettings.DEFAULT_WHITELIST);
    this.settings.threshold = AddonSettings.clampThreshold(
        readInt(config, KEY_THRESHOLD, AddonSettings.DEFAULT_THRESHOLD));
    this.settings.icedOutCheck = readBoolean(config, KEY_ICED_OUT, true);
    this.settings.slotFrame = readBoolean(config, KEY_SLOT_FRAME, true);
    this.applySettings();
  }

  @Override
  protected void fillSettings(List<SettingsElement> list) {
    list.add(new HeaderElement("AH-Fake-Warner"));

    list.add(new BooleanElement("Aktiviert", new IconData(Material.LEVER),
        new Consumer<Boolean>() {
          @Override
          public void accept(Boolean value) {
            settings.enabled = value;
            save(KEY_ENABLED, value);
          }
        }, this.settings.enabled));

    StringElement whitelist = new StringElement("Vertrauenswürdige Signierer",
        new IconData(Material.NAME_TAG), this.settings.whitelist, new Consumer<String>() {
          @Override
          public void accept(String value) {
            settings.whitelist = value;
            save(KEY_WHITELIST, value);
          }
        });
    whitelist.maxLength(256);
    whitelist.bindDescription("Namen echter Signierer, durch Komma getrennt. Standard ist "
        + AddonSettings.DEFAULT_WHITELIST + " (offizielle Admin-Items). Solange die Liste leer"
        + " ist, wird keine Signatur geprüft.");
    list.add(whitelist);

    SliderElement threshold = new SliderElement("Ähnlichkeits-Schwelle",
        new IconData(Material.REDSTONE_COMPARATOR), this.settings.threshold);
    threshold.setRange(0, AddonSettings.MAX_THRESHOLD);
    threshold.setSteps(1);
    threshold.addCallback(new Consumer<Integer>() {
      @Override
      public void accept(Integer value) {
        settings.threshold = AddonSettings.clampThreshold(value);
        save(KEY_THRESHOLD, settings.threshold);
      }
    });
    threshold.bindDescription("Wie viele Buchstaben ein Name abweichen darf, um als"
        + " \"Mögliche Fälschung\" zu gelten (0 = nur verwechselbare Zeichen wie I/l oder 0/O).");
    list.add(threshold);

    BooleanElement icedOut = new BooleanElement("Iced-Out-Fälschungen prüfen",
        new IconData(Material.DIAMOND), new Consumer<Boolean>() {
          @Override
          public void accept(Boolean value) {
            settings.icedOutCheck = value;
            save(KEY_ICED_OUT, value);
          }
        }, this.settings.icedOutCheck);
    icedOut.bindDescription("Warnt bei \"Iced Out\"-Items ohne Verzauberung, denn echte tragen"
        + " immer eine.");
    list.add(icedOut);

    list.add(new BooleanElement("Roter Rahmen um Slots", new IconData(Material.ITEM_FRAME),
        new Consumer<Boolean>() {
          @Override
          public void accept(Boolean value) {
            settings.slotFrame = value;
            save(KEY_SLOT_FRAME, value);
          }
        }, this.settings.slotFrame));
  }

  // Wert in die Addon-Konfiguration schreiben, speichern und sofort übernehmen
  private void save(String key, Object value) {
    JsonObject config = getConfig();
    if (value instanceof Boolean) {
      config.addProperty(key, (Boolean) value);
    } else if (value instanceof Number) {
      config.addProperty(key, (Number) value);
    } else {
      config.addProperty(key, String.valueOf(value));
    }
    saveConfig();
    this.applySettings();
  }

  private void applySettings() {
    TooltipHook.setWarner(this.settings.createWarner());
    SlotHook.setEnabled(this.settings.enabled && this.settings.slotFrame);
  }

  private static boolean readBoolean(JsonObject config, String key, boolean fallback) {
    JsonElement element = config.get(key);
    try {
      return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
    } catch (RuntimeException invalid) {
      return fallback;
    }
  }

  private static String readString(JsonObject config, String key, String fallback) {
    JsonElement element = config.get(key);
    return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
  }

  private static int readInt(JsonObject config, String key, int fallback) {
    JsonElement element = config.get(key);
    try {
      return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
    } catch (RuntimeException invalid) {
      return fallback;
    }
  }
}
