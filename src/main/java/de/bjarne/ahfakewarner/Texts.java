package de.bjarne.ahfakewarner;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Texte der Addon-Meldungen auf Deutsch und Englisch (wie im LabyMod-4-Addon).
 * Die Sprache ist in den Einstellungen wählbar, Standard Deutsch. Platzhalter: %s.
 * Ohne Minecraft-Code, damit testbar.
 */
public final class Texts {

  /** Sprache der Addon-Meldungen. */
  public enum Language {
    GERMAN("Deutsch"),
    ENGLISH("Englisch"),
    GAME("Wie Spielsprache");

    private final String displayName;

    Language(String displayName) {
      this.displayName = displayName;
    }

    /** Anzeige im Auswahlmenü der Einstellungen. */
    @Override
    public String toString() {
      return this.displayName;
    }
  }

  private static final Map<String, String> GERMAN = new HashMap<String, String>();
  private static final Map<String, String> ENGLISH = new HashMap<String, String>();

  private static volatile Language language = Language.GERMAN;
  private static volatile String gameLocale = "";

  static {
    // Die Schrift von 1.8.9 kann "–" und „“ nicht sicher darstellen, daher "-" und "\""
    put("disabled", "Das Addon ist deaktiviert.", "The addon is disabled.");
    put("notInstalled", "Nicht alles konnte eingebaut werden (Tooltip: %s, Rahmen: %s,"
        + " Timer-Taste: %s).", "Not everything could be installed (tooltip: %s, frame: %s,"
        + " timer key: %s).");
    put("yes", "ja", "yes");
    put("no", "nein", "no");

    put("warning.fake", "FÄLSCHUNG - imitiert %s", "FAKE - imitates %s");
    put("warning.suspect", "Mögliche Fälschung - ähnlich zu %s", "Possible fake - similar to %s");
    put("warning.icedOut", "FÄLSCHUNG - Iced Out ohne Verzauberung",
        "FAKE - Iced Out without enchantment");
    put("warning.chatIcedOut", "%s: Iced Out ohne Verzauberung, vermutlich gefälscht",
        "%s: Iced Out without enchantment, probably fake");
    put("warning.chat", "%s: signiert von %s", "%s: signed by %s");
    put("warning.spawnEgg", "ACHTUNG - das Ei enthält ein anderes Mob (%s)",
        "WARNING - this egg contains a different mob (%s)");
    put("warning.chatSpawnEgg", "%s: Das Ei enthält ein anderes Mob (%s)",
        "%s: This egg contains a different mob (%s)");

    put("hint.set", "Erinnerung gesetzt für %s - [%s] zum Entfernen",
        "Reminder set for %s - [%s] to remove");
    put("hint.endingSoon", "Endet gleich (ca. %s)", "Ending soon (approx. %s)");
    put("hint.offerAtEnd", "[%s] Erinnern bei Ablauf (endet ca. %s)",
        "[%s] Remind at end (ends approx. %s)");
    put("hint.offer", "[%s] Erinnern %s Min. vor Ende (endet ca. %s)",
        "[%s] Remind %s min before end (ends approx. %s)");

    put("reminder.title", "Auktions-Erinnerung", "Auction reminder");
    put("reminder.noTime", "Keine Restzeit im Tooltip von \"%s\" gefunden.",
        "No remaining time found in the tooltip of \"%s\".");
    put("reminder.removed", "Erinnerung für \"%s\" entfernt.", "Reminder for \"%s\" removed.");
    put("reminder.set", "Erinnerung gesetzt: \"%s\" endet um %s, Erinnerung um %s.",
        "Reminder set: \"%s\" ends at %s, reminder at %s.");
    put("reminder.due", "Auktion \"%s\" endet in %s Min.", "Auction \"%s\" ends in %s min.");
    put("reminder.dueWithBid", "Auktion \"%s\" endet in %s Min. (Gebot %s)",
        "Auction \"%s\" ends in %s min. (bid %s)");
    put("reminder.cleared", "%s Erinnerung(en) gelöscht.", "%s reminder(s) cleared.");
    put("reminder.none", "Keine Erinnerungen aktiv.", "No active reminders.");
    put("reminder.entry", "- %s - endet %s, Erinnerung %s", "- %s - ends %s, reminder %s");
    put("reminder.usage", "Benutzung: /aht list | /aht clear", "Usage: /aht list | /aht clear");
  }

  private Texts() {
  }

  private static void put(String key, String german, String english) {
    GERMAN.put(key, german);
    ENGLISH.put(key, english);
  }

  public static void setLanguage(Language selected) {
    language = selected != null ? selected : Language.GERMAN;
  }

  /** @param locale Sprachcode des Spiels, z. B. "de_DE" oder "en_US" */
  public static void setGameLocale(String locale) {
    gameLocale = locale != null ? locale : "";
  }

  /** Ob gerade Deutsch verwendet wird (bei "Wie Spielsprache" nach dem Sprachcode des Spiels). */
  public static boolean isGerman() {
    Language selected = language;
    if (selected == Language.GAME) {
      return gameLocale.toLowerCase(Locale.ROOT).startsWith("de");
    }
    return selected == Language.GERMAN;
  }

  /** Text in der gewählten Sprache; Platzhalter %s werden der Reihe nach ersetzt. */
  public static String get(String key, Object... arguments) {
    String template = (isGerman() ? GERMAN : ENGLISH).get(key);
    if (template == null) {
      return key;
    }
    StringBuilder result = new StringBuilder(template.length() + 16);
    int start = 0;
    for (Object argument : arguments) {
      int index = template.indexOf("%s", start);
      if (index < 0) {
        break;
      }
      result.append(template, start, index).append(argument);
      start = index + 2;
    }
    return result.append(template.substring(start)).toString();
  }
}
