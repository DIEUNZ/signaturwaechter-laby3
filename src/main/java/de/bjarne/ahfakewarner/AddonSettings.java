package de.bjarne.ahfakewarner;

import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import java.util.ArrayList;
import java.util.List;

/**
 * Einstellungen des Addons mit den Standardwerten des LabyMod-4-Addons.
 * Ohne LabyMod- und Minecraft-Code, damit testbar.
 */
public final class AddonSettings {

  public static final String DEFAULT_WHITELIST = "AbgegrieftHD";
  public static final int DEFAULT_THRESHOLD = 2;
  public static final int MAX_THRESHOLD = 4;

  public boolean enabled = true;
  public String whitelist = DEFAULT_WHITELIST;
  public int threshold = DEFAULT_THRESHOLD;
  public boolean icedOutCheck = true;
  public boolean slotFrame = true;
  public boolean chatWarning = false;
  public boolean warningSound = true;

  // Auktions-Erinnerung
  /** LWJGL-Tastencode von R. */
  public static final int DEFAULT_REMINDER_KEY = 19;
  public static final int MAX_LEAD_MINUTES = 30;

  public boolean reminderEnabled = true;
  public int reminderKey = DEFAULT_REMINDER_KEY;
  public int reminderLeadMinutes = 2;
  public boolean reminderHint = true;

  // Slot-Farben im Auktionshaus
  public boolean highlightReminder = true;
  public boolean highlightBid = true;

  public static int clampLeadMinutes(int value) {
    return Math.max(0, Math.min(MAX_LEAD_MINUTES, value));
  }

  /** Whitelist-Namen, getrennt durch Komma, Semikolon oder Leerzeichen. */
  public List<String> whitelistNames() {
    List<String> names = new ArrayList<String>();
    for (String name : this.whitelist.split("[,;\\s]+")) {
      if (!name.trim().isEmpty()) {
        names.add(name);
      }
    }
    return names;
  }

  public static int clampThreshold(int value) {
    return Math.max(0, Math.min(MAX_THRESHOLD, value));
  }

  /** Prüflogik passend zu den Einstellungen oder {@code null}, wenn das Addon aus ist. */
  public TooltipWarner createWarner() {
    if (!this.enabled) {
      return null;
    }
    return new TooltipWarner(this.whitelistNames(), clampThreshold(this.threshold),
        SignatureExtractor.DEFAULT_PATTERN, this.icedOutCheck);
  }
}
