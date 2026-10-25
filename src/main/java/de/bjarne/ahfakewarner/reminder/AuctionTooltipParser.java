package de.bjarne.ahfakewarner.reminder;

import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liest Restzeit und Gebot aus den Tooltip-Zeilen des GG-Auktionshauses.
 * Beispiel: "Verbleibende Zeit:" / "4 Minuten".
 */
public final class AuctionTooltipParser {

  private static final String TIME_LABEL = "verbleibende zeit";
  private static final String BID_LABEL = "aktuelles gebot";
  // GG nennt den Spieler nicht, sondern schreibt "Du bist aktuell Höchstbietender!" bzw.
  // zeigt "Dein Gebotslimit", sobald man geboten hat
  private static final List<String> OWN_BID_MARKERS =
      Arrays.asList("du bist aktuell höchstbietender", "dein gebotslimit");
  private static final Pattern TIME_PART = Pattern.compile(
      "(\\d+)\\s*(tage?|d|stunden?|std|h|minuten?|min|m|sekunden?|sek|s)\\b");

  private AuctionTooltipParser() {
  }

  public static Optional<Duration> parseRemaining(List<String> lines) {
    return valueAfterLabel(lines, TIME_LABEL).flatMap(AuctionTooltipParser::parseDuration);
  }

  public static Optional<String> parseBid(List<String> lines) {
    return valueAfterLabel(lines, BID_LABEL);
  }

  /** Ob die Zeilen zu einem Auktionshaus-Angebot gehören. */
  public static boolean isAuction(List<String> lines) {
    return parseBid(lines).isPresent();
  }

  /** Ob du auf diese Auktion geboten hast (Höchstbietender oder eigenes Gebotslimit). */
  public static boolean hasOwnBid(List<String> lines, String playerName) {
    for (String line : lines) {
      String lower = SignatureExtractor.stripFormatting(line).toLowerCase(Locale.ROOT);
      for (String marker : OWN_BID_MARKERS) {
        if (lower.contains(marker)) {
          return true;
        }
      }
    }
    return mentionsPlayerInAuction(lines, playerName);
  }

  /**
   * Ob der Spielername im Auktionsteil (ab "Aktuelles Gebot"/"Verbleibende Zeit") vorkommt,
   * z. B. als Höchstbietender. Die Signaturzeile davor zählt nicht.
   */
  public static boolean mentionsPlayerInAuction(List<String> lines, String playerName) {
    if (playerName == null || playerName.trim().isEmpty()) {
      return false;
    }
    Pattern name = Pattern.compile("(?i)(?<![A-Za-z0-9_])" + Pattern.quote(playerName)
        + "(?![A-Za-z0-9_])");
    boolean inAuction = false;
    for (String line : lines) {
      String plain = SignatureExtractor.stripFormatting(line);
      String lower = plain.toLowerCase(Locale.ROOT);
      if (lower.contains(BID_LABEL) || lower.contains(TIME_LABEL)) {
        inAuction = true;
      }
      if (inAuction && name.matcher(plain).find()) {
        return true;
      }
    }
    return false;
  }

  /** Kennung einer Auktion: alles vor dem Auktionsteil (Name, Lore, Signatur, Kennung). */
  public static String stableKey(List<String> lines) {
    StringBuilder key = new StringBuilder();
    for (String line : lines) {
      String lower = SignatureExtractor.stripFormatting(line).toLowerCase(Locale.ROOT);
      if (lower.contains(BID_LABEL) || lower.contains(TIME_LABEL)) {
        break;
      }
      key.append(line).append('\n');
    }
    return key.toString();
  }

  // Wert steht entweder hinter dem Doppelpunkt oder in der nächsten Zeile
  private static Optional<String> valueAfterLabel(List<String> lines, String label) {
    for (int i = 0; i < lines.size(); i++) {
      String line = SignatureExtractor.stripFormatting(lines.get(i));
      int index = line.toLowerCase(Locale.ROOT).indexOf(label);
      if (index < 0) {
        continue;
      }

      String rest = line.substring(index + label.length()).replaceFirst("^\\s*:", "").trim();
      if (!rest.isEmpty()) {
        return Optional.of(rest);
      }
      if (i + 1 < lines.size()) {
        String next = SignatureExtractor.stripFormatting(lines.get(i + 1));
        if (!next.isEmpty()) {
          return Optional.of(next);
        }
      }
    }
    return Optional.empty();
  }

  public static Optional<Duration> parseDuration(String text) {
    Matcher matcher = TIME_PART.matcher(text.toLowerCase(Locale.ROOT));
    Duration total = Duration.ZERO;
    boolean found = false;
    while (matcher.find()) {
      long value = Long.parseLong(matcher.group(1));
      String unit = matcher.group(2);
      found = true;
      if (unit.startsWith("t") || unit.equals("d")) {
        total = total.plusDays(value);
      } else if (unit.startsWith("st") || unit.equals("h")) {
        total = total.plusHours(value);
      } else if (unit.startsWith("m")) {
        total = total.plusMinutes(value);
      } else {
        total = total.plusSeconds(value);
      }
    }
    return found ? Optional.of(total) : Optional.empty();
  }
}
