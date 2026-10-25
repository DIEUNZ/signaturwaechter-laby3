package de.bjarne.ahfakewarner.reminder;

/**
 * Entscheidet, ob zwei Tooltips dieselbe Auktion zeigen. Gleich benannte Items (z. B. viele
 * Köpfe) unterscheiden sich nur in der Endzeit.
 */
public final class AuctionIdentity {

  /** GG zeigt die Restzeit in ganzen Minuten, daher ist die Endzeit nur auf etwa eine Minute genau. */
  public static final long END_TOLERANCE_MILLIS = 90_000;

  private AuctionIdentity() {
  }

  public static boolean sameAuction(String keyA, long endsAtA, String keyB, long endsAtB) {
    return keyA.equals(keyB) && Math.abs(endsAtA - endsAtB) <= END_TOLERANCE_MILLIS;
  }
}
