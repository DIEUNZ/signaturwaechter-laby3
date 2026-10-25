package de.bjarne.ahfakewarner.hook;

import de.bjarne.ahfakewarner.FirstSeen;
import de.bjarne.ahfakewarner.TooltipWarner;
import de.bjarne.ahfakewarner.TooltipWarner.Finding;
import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import de.bjarne.ahfakewarner.reminder.ReminderService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wird aus dem per ASM veränderten {@code ItemStack.getTooltip} aufgerufen.
 * Darf keine Minecraft-Klassen verwenden und nie eine Exception nach außen werfen,
 * sonst bricht das Zeichnen des Tooltips ab.
 */
public final class TooltipHook {

  private static volatile TooltipWarner warner;
  private static volatile ReminderService reminders;
  private static volatile FirstSeen firstSeen;
  private static volatile boolean chatWarning;
  private static volatile Runnable warningSound;
  private static volatile ChatOutput chat;
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
    TooltipWarner currentWarner = warner;
    ReminderService currentReminders = reminders;
    if ((currentWarner == null && currentReminders == null) || computingLines || lines == null
        || lines.isEmpty()) {
      return;
    }
    try {
      // Erst alles berechnen, dann anhängen, damit die Prüfungen den Original-Tooltip sehen
      List<String> added = new ArrayList<String>(4);
      if (currentWarner != null) {
        List<Finding> findings = currentWarner.findings(lines,
            enchantments != null && !enchantments.isEmpty());
        added.addAll(TooltipWarner.tooltipLines(findings));
        notifyFirstHover(findings);
      }
      if (currentReminders != null) {
        List<String> plain = new ArrayList<String>(lines.size());
        for (String line : lines) {
          plain.add(SignatureExtractor.stripFormatting(line));
        }
        added.addAll(currentReminders.onTooltip(plain, System.currentTimeMillis()));
      }
      lines.addAll(added);
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

  // Beim ersten Hovern über ein gefälschtes Item: Chat-Warnung und/oder Warnton
  private static void notifyFirstHover(List<Finding> findings) {
    FirstSeen seen = firstSeen;
    ChatOutput output = chat;
    Runnable sound = warningSound;
    boolean chatOn = chatWarning && output != null;
    if (findings.isEmpty() || seen == null || (!chatOn && sound == null)) {
      return;
    }
    List<Finding> fresh = seen.newFindings(findings);
    if (fresh.isEmpty()) {
      return;
    }
    if (chatOn) {
      for (Finding finding : fresh) {
        output.send(finding.chatText);
      }
    }
    if (sound != null) {
      // Nur ein Ton, auch wenn ein Item zwei Auffälligkeiten hat
      sound.run();
    }
  }

  /**
   * @param seen        merkt sich bereits gemeldete Items
   * @param chatOn      einmalige Chat-Warnung eingeschaltet
   * @param soundPlayer spielt den Warnton oder {@code null}, wenn ausgeschaltet
   */
  public static void configureFirstHover(FirstSeen seen, boolean chatOn, Runnable soundPlayer) {
    firstSeen = seen;
    chatWarning = chatOn;
    warningSound = soundPlayer;
  }

  public static void setChat(ChatOutput output) {
    chat = output;
  }

  /** Auktions-Erinnerung: merkt sich das gehoverte Item und liefert die Hinweiszeile. */
  public static void setReminders(ReminderService service) {
    reminders = service;
  }

  /** Vom Transformer gesetzt, sobald der Eingriff in {@code ItemStack} geklappt hat. */
  public static void markInstalled() {
    installed = true;
  }

  public static boolean isInstalled() {
    return installed;
  }
}
