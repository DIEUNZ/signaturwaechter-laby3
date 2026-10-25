package de.bjarne.ahfakewarner.detect;

/**
 * Ergebnis einer Prüfung inkl. des ähnlichsten Whitelist-Namens.
 */
public final class DetectionResult {

  private final SignatureVerdict verdict;
  private final String signer;
  private final String matchedName;
  private final int distance;

  /**
   * @param verdict   Urteil
   * @param signer    geprüfter Signatur-Name
   * @param matchedName ähnlichster Whitelist-Name oder {@code null}
   * @param distance  Editierdistanz der normalisierten Namen, -1 wenn nicht berechnet
   */
  public DetectionResult(SignatureVerdict verdict, String signer, String matchedName, int distance) {
    this.verdict = verdict;
    this.signer = signer;
    this.matchedName = matchedName;
    this.distance = distance;
  }

  public static DetectionResult unknown(String signer) {
    return new DetectionResult(SignatureVerdict.UNBEKANNT, signer, null, -1);
  }

  public SignatureVerdict verdict() {
    return this.verdict;
  }

  public String signer() {
    return this.signer;
  }

  public String matchedName() {
    return this.matchedName;
  }

  public int distance() {
    return this.distance;
  }
}
