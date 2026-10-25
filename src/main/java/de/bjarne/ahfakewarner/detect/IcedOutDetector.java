package de.bjarne.ahfakewarner.detect;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Erkennt gefälschte "Iced Out"-Items: Echte tragen eine Verzauberung (z. B. "Luck of the Sea I"),
 * Fälschungen mit dem Titel "*Iced Out*" haben keine. Enthält keinen Minecraft-Code.
 */
public final class IcedOutDetector {

  // Zeile wie "Luck of the Sea I" oder "Glück des Meeres III": Name + römische/arabische Stufe
  private static final Pattern ENCHANT_LINE =
      Pattern.compile("^[\\p{L}' -]+ (?:[IVX]{1,4}|\\d{1,3})$");

  private IcedOutDetector() {
  }

  /**
   * @param lines            Tooltip als reiner Text, Zeile 0 ist der Itemname
   * @param hasEnchantments  ob die Item-Daten selbst eine Verzauberung enthalten
   * @return {@code true}, wenn der Titel "Iced Out" sagt, aber keine Verzauberung erkennbar ist
   */
  public static boolean isFake(List<String> lines, boolean hasEnchantments) {
    return isIcedOut(lines) && !hasEnchantments && !hasEnchantmentLine(lines);
  }

  /** Der Itemname enthält "Iced Out" (auch mit Sternchen, Groß-/Kleinschreibung egal). */
  public static boolean isIcedOut(List<String> lines) {
    if (lines.isEmpty()) {
      return false;
    }
    String name = lines.get(0).toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ');
    return name.replaceAll("\\s+", " ").contains("iced out");
  }

  /** Fallback für Versionen, in denen die Verzauberungen nicht aus den Item-Daten lesbar sind. */
  static boolean hasEnchantmentLine(List<String> lines) {
    for (int i = 1; i < lines.size(); i++) {
      if (ENCHANT_LINE.matcher(lines.get(i).trim()).matches()) {
        return true;
      }
    }
    return false;
  }
}
