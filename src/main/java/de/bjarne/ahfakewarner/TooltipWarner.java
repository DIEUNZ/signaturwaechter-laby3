package de.bjarne.ahfakewarner;

import de.bjarne.ahfakewarner.detect.DetectionResult;
import de.bjarne.ahfakewarner.detect.IcedOutDetector;
import de.bjarne.ahfakewarner.detect.SignatureDetector;
import de.bjarne.ahfakewarner.detect.SignatureVerdict;
import de.bjarne.ahfakewarner.detect.SpawnEggDetector;
import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import de.bjarne.ahfakewarner.extract.SignatureExtractor.Signature;
import de.bjarne.ahfakewarner.reminder.AuctionTooltipParser;
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
  private static final String ORANGE = "§6";

  private final SignatureDetector detector;
  private final SignatureExtractor extractor;
  private final boolean icedOutCheck;
  private final boolean spawnEggCheck;

  public TooltipWarner(List<String> whitelist, int threshold, String signaturePattern,
      boolean icedOutCheck) {
    this(whitelist, threshold, signaturePattern, icedOutCheck, true);
  }

  public TooltipWarner(List<String> whitelist, int threshold, String signaturePattern,
      boolean icedOutCheck, boolean spawnEggCheck) {
    this.detector = new SignatureDetector(whitelist, threshold);
    this.extractor = new SignatureExtractor(signaturePattern);
    this.icedOutCheck = icedOutCheck;
    this.spawnEggCheck = spawnEggCheck;
  }

  /**
   * @param lines           Tooltip-Zeilen mit Farbcodes, Zeile 0 ist der Itemname
   * @param hasEnchantments ob die Item-Daten eine Verzauberung enthalten
   * @return anzuhängende Zeilen (leer, wenn nichts auffällt)
   */
  public List<String> warningsFor(List<String> lines, boolean hasEnchantments) {
    return tooltipLines(this.findings(lines, hasEnchantments));
  }

  /** Warnzeilen (je eine Leerzeile davor) für den Tooltip. */
  public static List<String> tooltipLines(List<Finding> findings) {
    List<String> warnings = new ArrayList<String>(4);
    for (Finding finding : findings) {
      warnings.add("");
      warnings.add((finding.fake ? RED : ORANGE) + WARNING_SYMBOL + " " + finding.tooltipText);
    }
    return warnings;
  }

  /**
   * Auffälligkeiten mit Text für Tooltip und Chat.
   *
   * @param lines           Tooltip-Zeilen mit Farbcodes, Zeile 0 ist der Itemname
   * @param hasEnchantments ob die Item-Daten eine Verzauberung enthalten
   */
  public List<Finding> findings(List<String> lines, boolean hasEnchantments) {
    return this.findings(lines, hasEnchantments, null);
  }

  /**
   * Wie {@link #findings(List, boolean)}, zusätzlich mit dem Mob im Spawn-Ei.
   *
   * @param eggMob Mob im Spawn-Ei (z. B. "ghast") oder {@code null}, wenn es keins ist
   */
  public List<Finding> findings(List<String> lines, boolean hasEnchantments, String eggMob) {
    List<String> plain = new ArrayList<String>(lines.size());
    for (String line : lines) {
      plain.add(SignatureExtractor.stripFormatting(line));
    }
    String itemName = AuctionTooltipParser.itemName(plain);

    List<Finding> findings = new ArrayList<Finding>(2);
    Optional<Signature> signature = this.extractor.extract(plain);
    if (signature.isPresent()) {
      DetectionResult result = this.detector.detect(signature.get().name());
      if (result.verdict().isWarning()) {
        String text = Texts.get(result.verdict() == SignatureVerdict.FAKE
            ? "warning.fake" : "warning.suspect", result.matchedName());
        findings.add(new Finding(true, text,
            itemName + '|' + signature.get().name() + '|' + signature.get().date(),
            Texts.get("warning.chat", itemName, signature.get().name())));
      }
    }

    if (this.icedOutCheck && IcedOutDetector.isFake(plain, hasEnchantments)) {
      findings.add(new Finding(true, Texts.get("warning.icedOut"), "icedout|" + itemName,
          Texts.get("warning.chatIcedOut", itemName)));
    }

    // Orange statt rot: Name nennt ein anderes Mob als im Ei steckt
    if (this.spawnEggCheck && SpawnEggDetector.isMismatchForMob(eggMob, itemName)) {
      findings.add(new Finding(false, Texts.get("warning.spawnEgg", eggMob),
          "spawnegg|" + itemName, Texts.get("warning.chatSpawnEgg", itemName, eggMob)));
    }
    return findings;
  }

  /** Eine Auffälligkeit eines Items. */
  public static final class Finding {

    /** Rote Fälschung (Signatur, Iced Out); sonst orange (falsch benanntes Spawn-Ei). */
    public final boolean fake;
    /** Warnzeile im Tooltip (ohne Farbe und Symbol). */
    public final String tooltipText;
    /** Kennung, damit pro Item nur einmal im Chat gewarnt wird. */
    public final String chatKey;
    /** Text der Chat-Warnung. */
    public final String chatText;

    Finding(boolean fake, String tooltipText, String chatKey, String chatText) {
      this.fake = fake;
      this.tooltipText = tooltipText;
      this.chatKey = chatKey;
      this.chatText = chatText;
    }
  }

  /** Ob das Item auffällt (für den roten Slot-Rahmen). */
  public boolean isWarning(List<String> lines, boolean hasEnchantments) {
    return !this.warningsFor(lines, hasEnchantments).isEmpty();
  }
}
