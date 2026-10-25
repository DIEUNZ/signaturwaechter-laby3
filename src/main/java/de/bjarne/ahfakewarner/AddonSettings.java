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
