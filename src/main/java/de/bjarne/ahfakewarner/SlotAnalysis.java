package de.bjarne.ahfakewarner;

import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import de.bjarne.ahfakewarner.reminder.AuctionTooltipParser;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Was ein Slot im Inventar über sein Item wissen muss: Fälschung (roter Rahmen) und
 * Auktionsdaten für die Farbmarkierung. Ohne Minecraft-Code, damit testbar.
 */
public final class SlotAnalysis {

  private final boolean warning;
  private final boolean fake;
  private final String auctionKey;
  private final boolean ownBid;
  private final long endsAtEstimate;

  private SlotAnalysis(boolean warning, boolean fake, String auctionKey, boolean ownBid,
      long endsAtEstimate) {
    this.warning = warning;
    this.fake = fake;
    this.auctionKey = auctionKey;
    this.ownBid = ownBid;
    this.endsAtEstimate = endsAtEstimate;
  }

  /**
   * @param lines           Tooltip-Zeilen mit Farbcodes (ohne Warnzeilen des Addons)
   * @param hasEnchantments ob die Item-Daten eine Verzauberung enthalten
   * @param eggMob          Mob im Spawn-Ei oder {@code null}
   * @param warner          Prüflogik oder {@code null}, wenn das Addon aus ist
   * @param playerName      eigener Spielername oder {@code null}
   * @param now             aktuelle Zeit (Epoch-Millis)
   */
  public static SlotAnalysis of(List<String> lines, boolean hasEnchantments, String eggMob,
      TooltipWarner warner, String playerName, long now) {
    boolean warning = false;
    boolean fake = false;
    if (warner != null) {
      for (TooltipWarner.Finding finding : warner.findings(lines, hasEnchantments, eggMob)) {
        warning = true;
        fake |= finding.fake;
      }
    }

    List<String> plain = new ArrayList<String>(lines.size());
    for (String line : lines) {
      plain.add(SignatureExtractor.stripFormatting(line));
    }
    if (!AuctionTooltipParser.isAuction(plain)) {
      return new SlotAnalysis(warning, fake, null, false, -1);
    }

    Optional<Duration> remaining = AuctionTooltipParser.parseRemaining(plain);
    return new SlotAnalysis(warning, fake,
        AuctionTooltipParser.stableKey(plain),
        AuctionTooltipParser.hasOwnBid(plain, playerName),
        remaining.isPresent() ? now + remaining.get().toMillis() : -1);
  }

  /** Item auffällig (Rahmen). */
  public boolean isWarning() {
    return this.warning;
  }

  /** Rote Fälschung (Signatur, Iced Out); sonst nur oranges Spawn-Ei. Rot hat Vorrang. */
  public boolean isFake() {
    return this.fake;
  }

  /** Kennung der Auktion oder {@code null}, wenn das Item keine Auktion ist. */
  public String auctionKey() {
    return this.auctionKey;
  }

  /** Ob du geboten hast (Höchstbietender oder Gebotslimit). */
  public boolean ownBid() {
    return this.ownBid;
  }

  /** Geschätzte Endzeit (Epoch-Millis) oder -1. */
  public long endsAtEstimate() {
    return this.endsAtEstimate;
  }
}
