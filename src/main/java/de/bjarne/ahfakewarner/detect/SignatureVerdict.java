package de.bjarne.ahfakewarner.detect;

/**
 * Ergebnis der Signaturprüfung.
 */
public enum SignatureVerdict {

  /** Exakter Treffer in der Whitelist. */
  ECHT,

  /** Normalisiert identisch mit einem Whitelist-Namen, aber anders geschrieben. */
  FAKE,

  /** Sehr ähnlich (geringe Editierdistanz) zu einem Whitelist-Namen. */
  FAKE_VERDACHT,

  /** Kein Bezug zur Whitelist, keine Warnung. */
  UNBEKANNT;

  /** Ob für dieses Ergebnis gewarnt werden soll. */
  public boolean isWarning() {
    return this == FAKE || this == FAKE_VERDACHT;
  }
}
