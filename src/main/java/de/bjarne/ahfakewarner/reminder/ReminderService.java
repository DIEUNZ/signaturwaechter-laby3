package de.bjarne.ahfakewarner.reminder;

import de.bjarne.ahfakewarner.Texts;
import de.bjarne.ahfakewarner.reminder.ReminderManager.Reminder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Auktions-Erinnerung: Hinweiszeile im Tooltip, Timer-Taste, fällige Erinnerungen und der
 * Befehl /aht. Liefert nur Texte zurück; Chat, Popup und Ton erledigt das Addon.
 * Ohne Minecraft- und LabyMod-Code, die Zeit wird von außen übergeben (testbar).
 */
public final class ReminderService {

  /** Tastencode für "keine Taste" (wie bei LabyMods Tasten-Einstellung). */
  public static final int NO_KEY = -1;

  // Die Schrift von 1.8.9 kann ⏰ nicht sicher darstellen
  private static final String CLOCK = "»";
  private static final String GREEN = "§a";
  private static final String YELLOW = "§e";
  private static final String AQUA = "§b";

  private final ReminderManager manager = new ReminderManager();
  private final HoverTracker hoverTracker = new HoverTracker();

  private volatile boolean active = true;
  private volatile int keyCode = 19;
  private volatile String keyName = "R";
  private volatile int leadMinutes = 2;
  private volatile boolean hint = true;

  /**
   * @param active      Addon und Erinnerungen eingeschaltet
   * @param keyCode     Tastencode der Timer-Taste oder {@link #NO_KEY}
   * @param keyName     Anzeigename der Taste, z. B. "R"
   * @param leadMinutes Minuten vor Ablauf erinnern
   * @param hint        Hinweiszeile im Tooltip zeigen
   */
  public void configure(boolean active, int keyCode, String keyName, int leadMinutes,
      boolean hint) {
    this.active = active;
    this.keyCode = keyCode;
    this.keyName = keyName;
    this.leadMinutes = Math.max(0, leadMinutes);
    this.hint = hint;
  }

  /**
   * Merkt sich das gehoverte Item und liefert die Hinweiszeilen für den Tooltip.
   *
   * @param plainLines Tooltip ohne Farbcodes, Zeile 0 ist der Itemname
   */
  public List<String> onTooltip(List<String> plainLines, long now) {
    this.hoverTracker.update(plainLines, now);
    if (!this.active || !this.hint || this.keyCode == NO_KEY) {
      return Collections.emptyList();
    }

    Optional<Duration> remaining = AuctionTooltipParser.parseRemaining(plainLines);
    if (!remaining.isPresent()) {
      return Collections.emptyList();
    }

    long endsAt = now + remaining.get().toMillis();
    String endsAtText = ReminderManager.format(endsAt);
    Reminder existing = this.manager.find(AuctionTooltipParser.stableKey(plainLines), endsAt);
    String line;
    if (existing != null) {
      line = GREEN + CLOCK + " " + Texts.get("hint.set",
          ReminderManager.format(existing.remindAt()), this.keyName);
    } else if (remaining.get().compareTo(Duration.ofMinutes(this.leadMinutes)) <= 0) {
      line = YELLOW + CLOCK + " " + Texts.get("hint.endingSoon", endsAtText);
    } else if (this.leadMinutes == 0) {
      line = AQUA + CLOCK + " " + Texts.get("hint.offerAtEnd", this.keyName, endsAtText);
    } else {
      line = AQUA + CLOCK + " " + Texts.get("hint.offer", this.keyName, this.leadMinutes,
          endsAtText);
    }

    List<String> lines = new ArrayList<String>(2);
    lines.add("");
    lines.add(line);
    return lines;
  }

  /** Ob für diese Auktion eine Erinnerung gesetzt ist (für die Slot-Farbe). */
  public boolean hasReminder(String key, long endsAt) {
    return this.active && key != null && endsAt >= 0 && this.manager.find(key, endsAt) != null;
  }

  /**
   * Timer-Taste in einem Inventar gedrückt.
   *
   * @return Chat-Meldung oder {@code null}, wenn die Taste nicht gemeint war
   */
  public String onKey(int pressedKey, long now) {
    if (!this.active || this.keyCode == NO_KEY || pressedKey != this.keyCode) {
      return null;
    }

    List<String> lines = this.hoverTracker.current(now);
    if (lines == null) {
      return null;
    }

    String itemName = AuctionTooltipParser.itemName(lines);
    Optional<Duration> remaining = AuctionTooltipParser.parseRemaining(lines);
    if (!remaining.isPresent()) {
      return Texts.get("reminder.noTime", itemName);
    }

    String bid = AuctionTooltipParser.parseBid(lines).orElse(null);
    Reminder reminder = this.manager.toggle(AuctionTooltipParser.stableKey(lines), itemName,
        bid, remaining.get().toMillis(), this.leadMinutes, now);
    if (reminder == null) {
      return Texts.get("reminder.removed", itemName);
    }
    return Texts.get("reminder.set", itemName, ReminderManager.format(reminder.endsAt()),
        ReminderManager.format(reminder.remindAt()));
  }

  /**
   * Fällige Erinnerungen als Meldungstext. Ist das Addon aus, werden sie still verworfen
   * statt später nachträglich ausgelöst.
   */
  public List<String> pollDue(long now) {
    List<Reminder> due = this.manager.pollDue(now);
    if (due.isEmpty() || !this.active) {
      return Collections.emptyList();
    }

    List<String> texts = new ArrayList<String>(due.size());
    for (Reminder reminder : due) {
      long minutesLeft = Math.max(0,
          Math.round((reminder.endsAt() - now) / (double) TimeUnit.MINUTES.toMillis(1)));
      String text = reminder.bid() == null
          ? Texts.get("reminder.due", reminder.itemName(), minutesLeft)
          : Texts.get("reminder.dueWithBid", reminder.itemName(), minutesLeft, reminder.bid());
      texts.add(CLOCK + " " + text);
    }
    return texts;
  }

  /**
   * Clientseitiger Befehl /aht list|clear – wird nicht an den Server gesendet.
   *
   * @return Chat-Meldungen oder {@code null}, wenn die Nachricht kein /aht-Befehl ist
   */
  public List<String> onCommand(String message, boolean addonEnabled) {
    String trimmed = message.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (!lower.equals("/aht") && !lower.startsWith("/aht ")) {
      return null;
    }
    if (!addonEnabled) {
      return Collections.singletonList(Texts.get("disabled"));
    }

    String[] parts = lower.split("\\s+");
    String action = parts.length < 2 ? "list" : parts[1];
    if (action.equals("clear")) {
      return Collections.singletonList(Texts.get("reminder.cleared", this.manager.clear()));
    }
    if (!action.equals("list")) {
      return Collections.singletonList(Texts.get("reminder.usage"));
    }

    List<Reminder> reminders = this.manager.reminders();
    if (reminders.isEmpty()) {
      return Collections.singletonList(Texts.get("reminder.none"));
    }
    List<String> texts = new ArrayList<String>(reminders.size());
    for (Reminder reminder : reminders) {
      texts.add(Texts.get("reminder.entry", reminder.itemName(),
          ReminderManager.format(reminder.endsAt()), ReminderManager.format(reminder.remindAt())));
    }
    return texts;
  }
}
