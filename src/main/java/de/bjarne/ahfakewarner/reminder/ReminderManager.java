package de.bjarne.ahfakewarner.reminder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Hält die Auktions-Erinnerungen. Nur im Speicher; nichts wird an den Server gesendet.
 * Ohne Minecraft-Code, die Zeit wird von außen übergeben (testbar).
 */
public final class ReminderManager {

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

  private final List<Reminder> reminders = new ArrayList<Reminder>();

  /**
   * Legt eine Erinnerung an oder entfernt sie, falls es für die Auktion schon eine gibt.
   *
   * @return die neue Erinnerung oder {@code null}, wenn eine bestehende entfernt wurde
   */
  public synchronized Reminder toggle(String key, String itemName, String bid,
      long remainingMillis, int leadMinutes, long now) {
    long endsAt = now + remainingMillis;
    Iterator<Reminder> iterator = this.reminders.iterator();
    while (iterator.hasNext()) {
      Reminder reminder = iterator.next();
      if (AuctionIdentity.sameAuction(reminder.key(), reminder.endsAt(), key, endsAt)) {
        iterator.remove();
        return null;
      }
    }

    long remindAt = Math.max(now, endsAt - TimeUnit.MINUTES.toMillis(leadMinutes));
    Reminder reminder = new Reminder(key, itemName, bid, endsAt, remindAt);
    this.reminders.add(reminder);
    return reminder;
  }

  /** Sucht die Erinnerung zu dieser Auktion (gleicher Name und etwa gleiche Endzeit). */
  public synchronized Reminder find(String key, long endsAt) {
    for (Reminder reminder : this.reminders) {
      if (AuctionIdentity.sameAuction(reminder.key(), reminder.endsAt(), key, endsAt)) {
        return reminder;
      }
    }
    return null;
  }

  public synchronized List<Reminder> reminders() {
    return Collections.unmodifiableList(new ArrayList<Reminder>(this.reminders));
  }

  public synchronized int clear() {
    int count = this.reminders.size();
    this.reminders.clear();
    return count;
  }

  /** Entfernt und liefert alle Erinnerungen, deren Zeitpunkt erreicht ist. */
  public synchronized List<Reminder> pollDue(long now) {
    List<Reminder> due = new ArrayList<Reminder>();
    Iterator<Reminder> iterator = this.reminders.iterator();
    while (iterator.hasNext()) {
      Reminder reminder = iterator.next();
      if (reminder.remindAt() <= now) {
        due.add(reminder);
        iterator.remove();
      }
    }
    return due;
  }

  public static String format(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(TIME);
  }

  /** Eine gesetzte Erinnerung (in Java 21 ein record). */
  public static final class Reminder {

    private final String key;
    private final String itemName;
    private final String bid;
    private final long endsAt;
    private final long remindAt;

    /**
     * @param key      Erkennung desselben Items (Name + Signatur + Kennung)
     * @param itemName Anzeigename
     * @param bid      aktuelles Gebot oder {@code null}
     * @param endsAt   Ablaufzeitpunkt (Epoch-Millis)
     * @param remindAt Erinnerungszeitpunkt (Epoch-Millis)
     */
    public Reminder(String key, String itemName, String bid, long endsAt, long remindAt) {
      this.key = key;
      this.itemName = itemName;
      this.bid = bid;
      this.endsAt = endsAt;
      this.remindAt = remindAt;
    }

    public String key() {
      return this.key;
    }

    public String itemName() {
      return this.itemName;
    }

    public String bid() {
      return this.bid;
    }

    public long endsAt() {
      return this.endsAt;
    }

    public long remindAt() {
      return this.remindAt;
    }
  }
}
