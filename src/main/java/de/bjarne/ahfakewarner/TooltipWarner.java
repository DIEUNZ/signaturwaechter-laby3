package de.bjarne.ahfakewarner;

import de.bjarne.ahfakewarner.detect.DetectionResult;
import de.bjarne.ahfakewarner.detect.IcedOutDetector;
import de.bjarne.ahfakewarner.detect.SignatureDetector;
import de.bjarne.ahfakewarner.detect.SignatureVerdict;
import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import de.bjarne.ahfakewarner.extract.SignatureExtractor.Signature;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Entscheidet anhand der Tooltip-Zeilen, welche Warnzeilen angehängt werden.
 * Enthält keinen Minecraft-Code und ist dadurch testbar.
 */
public final class TooltipWarner {

  // Die Schrift von 1.8.9 kann ⚠ nicht sicher darstellen
  private static final String WARNING_SYMBOL = "!";
  private static final String RED = "§c";

  private final SignatureDetector detector;
  private final SignatureExtractor extractor;
  private final boolean icedOutCheck;

  public TooltipWarner(List<String> whitelist, int threshold, String signaturePattern,
      boolean icedOutCheck) {
    this.detector = new SignatureDetector(whitelist, threshold);
    this.extractor = new SignatureExtractor(signaturePattern);
    this.icedOutCheck = icedOutCheck;
  }

  /**
   * @param lines           Tooltip-Zeilen mit Farbcodes, Zeile 0 ist der Itemname
   * @param hasEnchantments ob die Item-Daten eine Verzauberung enthalten
   * @return anzuhängende Zeilen (leer, wenn nichts auffällt)
   */
  public List<String> warningsFor(List<String> lines, boolean hasEnchantments) {
    List<String> plain = new ArrayList<String>(lines.size());
    for (String line : lines) {
      plain.add(SignatureExtractor.stripFormatting(line));
    }

    List<String> warnings = new ArrayList<String>(4);
    Optional<Signature> signature = this.extractor.extract(plain);
    if (signature.isPresent()) {
      DetectionResult result = this.detector.detect(signature.get().name());
      if (result.verdict().isWarning()) {
        String text = result.verdict() == SignatureVerdict.FAKE
            ? "FÄLSCHUNG - imitiert " + result.matchedName()
            : "Mögliche Fälschung - ähnlich zu " + result.matchedName();
        warnings.add("");
        warnings.add(RED + WARNING_SYMBOL + " " + text);
      }
    }

    if (this.icedOutCheck && IcedOutDetector.isFake(plain, hasEnchantments)) {
      warnings.add("");
      warnings.add(RED + WARNING_SYMBOL + " FÄLSCHUNG - Iced Out ohne Verzauberung");
    }
    return warnings;
  }

  /** Ob das Item auffällt (für den roten Slot-Rahmen). */
  public boolean isWarning(List<String> lines, boolean hasEnchantments) {
    return !this.warningsFor(lines, hasEnchantments).isEmpty();
  }
}
