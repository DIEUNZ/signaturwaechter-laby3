package de.bjarne.ahfakewarner.reminder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Merkt sich die Tooltip-Zeilen des zuletzt gehoverten Items, damit die Timer-Taste weiß,
 * worauf sie sich bezieht.
 */
public final class HoverTracker {

  // Tooltip wird jeden Frame neu gebaut; älter als das gilt als "nicht mehr gehovert"
  private static final long MAX_AGE_MILLIS = 250;

  private List<String> lines = Collections.emptyList();
  private long lastSeen;

  public synchronized void update(List<String> plainLines, long now) {
    this.lines = Collections.unmodifiableList(new ArrayList<String>(plainLines));
    this.lastSeen = now;
  }

  /** @return Zeilen des gerade gehoverten Items oder {@code null} */
  public synchronized List<String> current(long now) {
    if (this.lines.isEmpty() || now - this.lastSeen > MAX_AGE_MILLIS) {
      return null;
    }
    return this.lines;
  }
}
