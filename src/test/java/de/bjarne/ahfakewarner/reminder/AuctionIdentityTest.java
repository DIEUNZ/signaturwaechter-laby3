package de.bjarne.ahfakewarner.reminder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuctionIdentityTest {

  private static final long MINUTE = 60_000;
  private static final long NOW = 1_000_000_000L;

  @Test
  void gleichNamigeAuktionenMitAndererEndzeitSindVerschieden() {
    // Viele Köpfe mit demselben Namen, aber unterschiedlich lange Restzeit
    assertFalse(AuctionIdentity.sameAuction("Kopf", NOW + 27 * MINUTE, "Kopf", NOW + 52 * MINUTE));
  }

  @Test
  void dieselbeAuktionBleibtTrotzMinutenrundungGleich() {
    // GG zeigt ganze Minuten: Schätzungen derselben Auktion weichen bis ~1 Minute ab
    assertTrue(AuctionIdentity.sameAuction("Kopf", NOW + 27 * MINUTE, "Kopf", NOW + 27 * MINUTE + 50_000));
    assertTrue(AuctionIdentity.sameAuction("Kopf", NOW + 27 * MINUTE, "Kopf", NOW + 26 * MINUTE + 5_000));
  }

  @Test
  void anderesItemIstNieGleich() {
    assertFalse(AuctionIdentity.sameAuction("Kopf", NOW, "Kiste", NOW));
  }
}
