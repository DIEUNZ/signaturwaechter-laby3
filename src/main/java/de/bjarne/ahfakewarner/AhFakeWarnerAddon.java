package de.bjarne.ahfakewarner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.bjarne.ahfakewarner.hook.KeyHook;
import de.bjarne.ahfakewarner.hook.SlotHook;
import de.bjarne.ahfakewarner.hook.TooltipHook;
import de.bjarne.ahfakewarner.reminder.ReminderService;
import java.lang.reflect.Method;
import java.util.List;
import net.labymod.api.LabyModAddon;
import net.labymod.api.events.MessageSendEvent;
import net.labymod.api.events.RenderIngameOverlayEvent;
import net.labymod.core.LabyModCore;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement.IconData;
import net.labymod.settings.elements.HeaderElement;
import net.labymod.settings.elements.KeyElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.SliderElement;
import net.labymod.settings.elements.StringElement;
import net.labymod.utils.Consumer;
import net.labymod.utils.Material;
import org.lwjgl.input.Keyboard;

/**
 * Einstiegspunkt des LabyMod-3-Addons.
 *
 * Die Warnungen und die Timer-Taste laufen über die ASM-Eingriffe in {@code ItemStack} und
 * {@code GuiContainer} (siehe {@code AhFakeWarnerTransformer}); hier werden Einstellungen
 * geladen, gespeichert und an die Hooks übergeben sowie fällige Erinnerungen gemeldet.
 */
public class AhFakeWarnerAddon extends LabyModAddon {

  static final String PREFIX = "§c[AH-Fake-Warner] §f";
  private static final String YELLOW = "§e";

  private static final String KEY_ENABLED = "enabled";
  private static final String KEY_WHITELIST = "whitelist";
  private static final String KEY_THRESHOLD = "threshold";
  private static final String KEY_ICED_OUT = "icedOutCheck";
  private static final String KEY_SLOT_FRAME = "slotFrame";
  private static final String KEY_REMINDER_ENABLED = "reminderEnabled";
  private static final String KEY_REMINDER_KEY = "reminderKey";
  private static final String KEY_REMINDER_LEAD = "reminderLeadMinutes";
  private static final String KEY_REMINDER_HINT = "reminderHint";

  // Notenblock-Ton von 1.8.9 (ab 1.9 heißt er anders)
  private static final String REMINDER_SOUND = "note.pling";
  private static final long DUE_CHECK_MILLIS = 1000;

  private final AddonSettings settings = new AddonSettings();
  private final ReminderService reminders = new ReminderService();

  /** Merkt sich, ob der Eingriffs-Status in dieser Sitzung schon geprüft wurde. */
  private boolean statusChecked;
  private long lastDueCheck;
  private boolean soundErrorLogged;

  @Override
  public void onEnable() {
    System.out.println("[AH-Fake-Warner] Addon geladen, Tooltip-Eingriff "
        + (TooltipHook.isInstalled() ? "aktiv" : "fehlt") + ", Slot-Eingriff "
        + (SlotHook.isInstalled() ? "aktiv" : "fehlt") + ", Tasten-Eingriff "
        + (KeyHook.isInstalled() ? "aktiv" : "fehlt"));

    TooltipHook.setReminders(this.reminders);
    KeyHook.setup(this.reminders, new KeyHook.ChatOutput() {
      @Override
      public void send(String text) {
        getApi().displayMessageInChat(PREFIX + text);
      }
    });

    // Es gibt kein Tick- oder Welt-betreten-Event; das Overlay wird aber in jedem Frame im
    // Spiel gezeichnet (auch unter offenen Inventaren)
    getApi().getEventManager().register(new RenderIngameOverlayEvent() {
      @Override
      public void onRender(float partialTicks) {
        if (!getApi().isIngame()) {
          return;
        }
        checkStatusOnce();
        checkDueReminders();
      }
    });

    // Clientseitiger Befehl /aht list|clear; true = nicht an den Server senden
    getApi().getEventManager().register(new MessageSendEvent() {
      @Override
      public boolean onSend(String message) {
        List<String> answer = reminders.onCommand(message, settings.enabled);
        if (answer == null) {
          return false;
        }
        for (String line : answer) {
          getApi().displayMessageInChat(PREFIX + line);
        }
        return true;
      }
    });
  }

  // Beim ersten Mal im Spiel nur melden, falls ein Eingriff fehlt
  private void checkStatusOnce() {
    if (this.statusChecked) {
      return;
    }
    this.statusChecked = true;
    if (!TooltipHook.isInstalled() || !SlotHook.isInstalled() || !KeyHook.isInstalled()) {
      getApi().displayMessageInChat(PREFIX + "§cNicht alles konnte eingebaut werden"
          + " (Tooltip: " + yesNo(TooltipHook.isInstalled())
          + ", Rahmen: " + yesNo(SlotHook.isInstalled())
          + ", Timer-Taste: " + yesNo(KeyHook.isInstalled()) + ").");
    }
  }

  private void checkDueReminders() {
    long now = System.currentTimeMillis();
    if (now - this.lastDueCheck < DUE_CHECK_MILLIS) {
      return;
    }
    this.lastDueCheck = now;
    for (String text : this.reminders.pollDue(now)) {
      getApi().displayMessageInChat(PREFIX + YELLOW + text);
      LabyMod.getInstance().notifyMessageRaw("Auktions-Erinnerung", text);
      this.playReminderSound();
    }
  }

  // LabyMods playSound erwartet eine ResourceLocation, deren Klassenname im Spiel verschleiert
  // ist; deshalb per Reflexion über die Parametertypen statt über den Namen
  private void playReminderSound() {
    try {
      Object minecraft = LabyModCore.getMinecraft();
      for (Method method : minecraft.getClass().getMethods()) {
        Class<?>[] parameters = method.getParameterTypes();
        if (method.getName().equals("playSound") && parameters.length == 2
            && parameters[1] == float.class) {
          Object sound = parameters[0].getConstructor(String.class).newInstance(REMINDER_SOUND);
          method.invoke(minecraft, sound, 1F);
          return;
        }
      }
      throw new NoSuchMethodException("playSound");
    } catch (Throwable throwable) {
      if (!this.soundErrorLogged) {
        this.soundErrorLogged = true;
        System.err.println("[AH-Fake-Warner] Erinnerungston konnte nicht abgespielt werden");
        throwable.printStackTrace();
      }
    }
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
    this.settings.reminderEnabled = readBoolean(config, KEY_REMINDER_ENABLED, true);
    this.settings.reminderKey = readInt(config, KEY_REMINDER_KEY,
        AddonSettings.DEFAULT_REMINDER_KEY);
    this.settings.reminderLeadMinutes = AddonSettings.clampLeadMinutes(
        readInt(config, KEY_REMINDER_LEAD, 2));
    this.settings.reminderHint = readBoolean(config, KEY_REMINDER_HINT, true);
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

    this.fillReminderSettings(list);
  }

  private void fillReminderSettings(List<SettingsElement> list) {
    list.add(new HeaderElement("Auktions-Erinnerung"));

    list.add(new BooleanElement("Erinnerungen aktiv", new IconData(Material.WATCH),
        new Consumer<Boolean>() {
          @Override
          public void accept(Boolean value) {
            settings.reminderEnabled = value;
            save(KEY_REMINDER_ENABLED, value);
          }
        }, this.settings.reminderEnabled));

    KeyElement key = new KeyElement("Taste (beim Hovern)", new IconData(Material.PAPER),
        this.settings.reminderKey, new Consumer<Integer>() {
          @Override
          public void accept(Integer value) {
            settings.reminderKey = value;
            save(KEY_REMINDER_KEY, value);
          }
        });
    key.bindDescription("Über einer Auktion drücken, um eine Erinnerung zu setzen oder zu"
        + " entfernen. Liste mit /aht, löschen mit /aht clear.");
    list.add(key);

    SliderElement lead = new SliderElement("Minuten vor Ablauf erinnern",
        new IconData(Material.COMPASS), this.settings.reminderLeadMinutes);
    lead.setRange(0, AddonSettings.MAX_LEAD_MINUTES);
    lead.setSteps(1);
    lead.addCallback(new Consumer<Integer>() {
      @Override
      public void accept(Integer value) {
        settings.reminderLeadMinutes = AddonSettings.clampLeadMinutes(value);
        save(KEY_REMINDER_LEAD, settings.reminderLeadMinutes);
      }
    });
    list.add(lead);

    BooleanElement hint = new BooleanElement("Timer im Tooltip vorschlagen",
        new IconData(Material.BOOK_AND_QUILL), new Consumer<Boolean>() {
          @Override
          public void accept(Boolean value) {
            settings.reminderHint = value;
            save(KEY_REMINDER_HINT, value);
          }
        }, this.settings.reminderHint);
    hint.bindDescription("Zeigt bei Auktionen an, mit welcher Taste du eine Erinnerung setzt.");
    list.add(hint);
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

    int key = this.settings.reminderKey;
    boolean hasKey = key > Keyboard.KEY_NONE;
    this.reminders.configure(this.settings.enabled && this.settings.reminderEnabled,
        hasKey ? key : ReminderService.NO_KEY, hasKey ? keyName(key) : "",
        this.settings.reminderLeadMinutes, this.settings.reminderHint);
  }

  private static String keyName(int key) {
    String name = Keyboard.getKeyName(key);
    return name != null ? name : String.valueOf(key);
  }

  private static String yesNo(boolean value) {
    return value ? "ja" : "nein";
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
