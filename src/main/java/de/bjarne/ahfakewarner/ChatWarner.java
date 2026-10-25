package de.bjarne.ahfakewarner;

import de.bjarne.ahfakewarner.TooltipWarner.Finding;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Einmalige Chat-Warnung: pro Item (Name + Signatur + Datum) nur beim ersten Hovern.
 * Ohne Minecraft-Code, damit testbar.
 */
public final class ChatWarner {

  private static final int MAX_WARNED = 500;

  private final Set<String> warned = new HashSet<String>();

  /** @return Chat-Texte für Auffälligkeiten, die noch nicht gemeldet wurden */
  public synchronized List<String> newMessages(List<Finding> findings) {
    List<String> messages = new ArrayList<String>(findings.size());
    for (Finding finding : findings) {
      if (this.warned.size() > MAX_WARNED) {
        this.warned.clear();
      }
      if (this.warned.add(finding.chatKey)) {
        messages.add(finding.chatText);
      }
    }
    return messages;
  }
}
