package de.bjarne.ahfakewarner.hook;

import de.bjarne.ahfakewarner.TooltipWarner;
import java.util.List;
import java.util.Map;

/**
 * Wird aus dem per ASM veränderten {@code ItemStack.getTooltip} aufgerufen.
 * Darf keine Minecraft-Klassen verwenden und nie eine Exception nach außen werfen,
 * sonst bricht das Zeichnen des Tooltips ab.
 */
public final class TooltipHook {

  private static volatile TooltipWarner warner;
  private static volatile boolean installed;
  private static boolean errorLogged;
  // Verhindert, dass die Slot-Prüfung über ihren eigenen Tooltip-Aufruf Warnzeilen anhängt
  private static boolean computingLines;

  private TooltipHook() {
  }

  /** Tooltip-Zeilen eines Items ohne die Warnzeilen dieses Addons. */
  public static List<String> linesWithoutWarnings(ItemSource source) {
    computingLines = true;
    try {
      return source.ahfw$lines();
    } finally {
      computingLines = false;
    }
  }

  /**
   * @param lines        fertige Tooltip-Zeilen, werden hier ergänzt
   * @param enchantments Verzauberungen des Items (ID → Stufe) aus {@code EnchantmentHelper}
   */
  public static void onTooltip(List<String> lines, Map<?, ?> enchantments) {
    TooltipWarner current = warner;
    if (current == null || computingLines || lines == null || lines.isEmpty()) {
      return;
    }
    try {
      lines.addAll(current.warningsFor(lines, enchantments != null && !enchantments.isEmpty()));
    } catch (Throwable throwable) {
      if (!errorLogged) {
        errorLogged = true;
        System.err.println("[AH-Fake-Warner] Fehler bei der Tooltip-Prüfung");
        throwable.printStackTrace();
      }
    }
  }

  public static void setWarner(TooltipWarner newWarner) {
    warner = newWarner;
  }

  public static TooltipWarner warner() {
    return warner;
  }

  /** Vom Transformer gesetzt, sobald der Eingriff in {@code ItemStack} geklappt hat. */
  public static void markInstalled() {
    installed = true;
  }

  public static boolean isInstalled() {
    return installed;
  }
}
