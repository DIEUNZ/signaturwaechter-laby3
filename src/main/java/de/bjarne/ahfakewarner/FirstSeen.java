package de.bjarne.ahfakewarner;

import de.bjarne.ahfakewarner.TooltipWarner.Finding;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Merkt sich, welche gefälschten Items (Name + Signatur + Datum) schon einmal gehovert
 * wurden, damit Chat-Warnung und Warnton pro Item nur einmal kommen.
 * Ohne Minecraft-Code, damit testbar.
 */
public final class FirstSeen {

  private static final int MAX_REMEMBERED = 500;

  private final Set<String> seen = new HashSet<String>();

  /** @return die Auffälligkeiten, die bisher noch nicht gemeldet wurden */
  public synchronized List<Finding> newFindings(List<Finding> findings) {
    List<Finding> fresh = new ArrayList<Finding>(findings.size());
    for (Finding finding : findings) {
      if (this.seen.size() > MAX_REMEMBERED) {
        this.seen.clear();
      }
      if (this.seen.add(finding.chatKey)) {
        fresh.add(finding);
      }
    }
    return fresh;
  }
}
