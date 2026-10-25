package de.bjarne.ahfakewarner.detect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prüft Signatur-Namen gegen eine Whitelist. Ohne Minecraft-Abhängigkeiten.
 * Instanzen sind unveränderlich; bei Settings-Änderung neu erzeugen.
 */
public final class SignatureDetector {

  private final List<String> whitelist;
  private final List<String> normalizedWhitelist;
  private final int threshold;
  private final Map<String, DetectionResult> cache = new ConcurrentHashMap<String, DetectionResult>();

  public SignatureDetector(List<String> whitelist, int threshold) {
    List<String> names = new ArrayList<String>(whitelist.size());
    List<String> normalized = new ArrayList<String>(whitelist.size());
    for (String name : whitelist) {
      String trimmed = name.trim();
      if (!trimmed.isEmpty()) {
        names.add(trimmed);
        normalized.add(NameNormalizer.normalize(trimmed));
      }
    }
    this.whitelist = Collections.unmodifiableList(names);
    this.normalizedWhitelist = Collections.unmodifiableList(normalized);
    this.threshold = Math.max(0, threshold);
  }

  public DetectionResult detect(String signer) {
    DetectionResult cached = this.cache.get(signer);
    if (cached == null) {
      cached = this.evaluate(signer);
      this.cache.put(signer, cached);
    }
    return cached;
  }

  private DetectionResult evaluate(String signer) {
    String trimmed = signer.trim();
    if (trimmed.isEmpty()) {
      return DetectionResult.unknown(signer);
    }

    // 1. Exakter Treffer
    if (this.whitelist.contains(trimmed)) {
      return new DetectionResult(SignatureVerdict.ECHT, trimmed, trimmed, 0);
    }

    // 2. + 3. Normalisiert gleich bzw. ähnlichster Name
    String normalized = NameNormalizer.normalize(trimmed);
    String bestName = null;
    int bestDistance = Integer.MAX_VALUE;
    for (int i = 0; i < this.whitelist.size(); i++) {
      int distance = damerauLevenshtein(normalized, this.normalizedWhitelist.get(i));
      if (distance < bestDistance) {
        bestDistance = distance;
        bestName = this.whitelist.get(i);
      }
    }

    if (bestName == null) {
      return DetectionResult.unknown(trimmed);
    }
    if (bestDistance == 0) {
      return new DetectionResult(SignatureVerdict.FAKE, trimmed, bestName, 0);
    }
    if (bestDistance <= this.threshold) {
      return new DetectionResult(SignatureVerdict.FAKE_VERDACHT, trimmed, bestName, bestDistance);
    }
    return new DetectionResult(SignatureVerdict.UNBEKANNT, trimmed, bestName, bestDistance);
  }

  /**
   * Damerau-Levenshtein-Distanz (Variante "optimal string alignment"):
   * Einfügen, Löschen, Ersetzen und Vertauschen benachbarter Zeichen kosten je 1.
   */
  static int damerauLevenshtein(String a, String b) {
    int[][] d = new int[a.length() + 1][b.length() + 1];
    for (int i = 0; i <= a.length(); i++) {
      d[i][0] = i;
    }
    for (int j = 0; j <= b.length(); j++) {
      d[0][j] = j;
    }

    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
        if (i > 1 && j > 1
            && a.charAt(i - 1) == b.charAt(j - 2)
            && a.charAt(i - 2) == b.charAt(j - 1)) {
          d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
        }
      }
    }
    return d[a.length()][b.length()];
  }
}
