package de.bjarne.ahfakewarner.extract;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Findet die Signaturzeile in reinem Text (ohne Minecraft-Abhängigkeiten).
 * Beispiel: "Signiert von AbgegrieftHD am 01.06.2026".
 */
public final class SignatureExtractor {

  public static final String DEFAULT_PATTERN =
      "(?i)signiert von\\s+(\\S+)\\s+am\\s+(\\d{1,2}\\.\\d{1,2}\\.\\d{4})";

  // Legacy-Farbcodes wie §a oder &a
  private static final Pattern COLOR_CODES = Pattern.compile("(?i)[§&][0-9a-fk-orx]");

  private final Pattern pattern;

  public SignatureExtractor(String regex) {
    this.pattern = compileOrDefault(regex);
  }

  public Optional<Signature> extract(List<String> lines) {
    for (String line : lines) {
      Matcher matcher = this.pattern.matcher(stripFormatting(line));
      if (matcher.find() && matcher.groupCount() >= 1) {
        String date = matcher.groupCount() >= 2 ? matcher.group(2) : null;
        return Optional.of(new Signature(matcher.group(1), date));
      }
    }
    return Optional.empty();
  }

  public static String stripFormatting(String text) {
    return COLOR_CODES.matcher(text).replaceAll("").trim();
  }

  // Ungültige Regex aus den Settings fällt auf das Standardmuster zurück
  private static Pattern compileOrDefault(String regex) {
    if (regex != null && !regex.trim().isEmpty()) {
      try {
        return Pattern.compile(regex);
      } catch (PatternSyntaxException ignored) {
      }
    }
    return Pattern.compile(DEFAULT_PATTERN);
  }

  /** Gefundene Signatur (in Java 21 ein record). */
  public static final class Signature {

    private final String name;
    private final String date;

    /**
     * @param name Name des Signierers
     * @param date Datum laut Signatur oder {@code null}
     */
    public Signature(String name, String date) {
      this.name = name;
      this.date = date;
    }

    public String name() {
      return this.name;
    }

    public String date() {
      return this.date;
    }
  }
}
