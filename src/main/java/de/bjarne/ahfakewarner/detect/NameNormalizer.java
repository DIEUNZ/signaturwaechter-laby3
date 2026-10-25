package de.bjarne.ahfakewarner.detect;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Bringt Namen in eine Vergleichsform, in der verwechselbare Zeichen gleich sind.
 */
public final class NameNormalizer {

  // Einzelne Zeichen, die optisch verwechselt werden (nach Kleinschreibung)
  private static final Map<Character, Character> CHAR_MAP = new HashMap<Character, Character>();

  static {
    CHAR_MAP.put('i', 'l');
    CHAR_MAP.put('1', 'l');
    CHAR_MAP.put('|', 'l');
    CHAR_MAP.put('!', 'l');
    CHAR_MAP.put('0', 'o');
    CHAR_MAP.put('5', 's');
    CHAR_MAP.put('3', 'e');
    // Kyrillische Doppelgänger
    CHAR_MAP.put('а', 'a');
    CHAR_MAP.put('в', 'b');
    CHAR_MAP.put('е', 'e');
    CHAR_MAP.put('һ', 'h');
    CHAR_MAP.put('і', 'l');
    CHAR_MAP.put('ӏ', 'l');
    CHAR_MAP.put('о', 'o');
    CHAR_MAP.put('р', 'p');
    CHAR_MAP.put('с', 'c');
    CHAR_MAP.put('т', 't');
    CHAR_MAP.put('х', 'x');
    CHAR_MAP.put('у', 'y');
    CHAR_MAP.put('ԁ', 'd');
  }

  // Zeichenfolgen, die zusammen wie ein anderer Buchstabe aussehen
  private static final String[][] SEQUENCES = {
      {"rn", "m"},
      {"vv", "w"},
      {"cl", "d"}
  };

  private NameNormalizer() {
  }

  public static String normalize(String name) {
    String lower = name.trim().toLowerCase(Locale.ROOT);
    StringBuilder builder = new StringBuilder(lower.length());
    for (int i = 0; i < lower.length(); i++) {
      char c = lower.charAt(i);
      Character mapped = CHAR_MAP.get(c);
      builder.append(mapped != null ? mapped : c);
    }

    String result = builder.toString();
    for (String[] sequence : SEQUENCES) {
      result = result.replace(sequence[0], sequence[1]);
    }
    return result;
  }
}
