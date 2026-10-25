package de.bjarne.ahfakewarner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatWarnerTest {

  private final TooltipWarner warner = new TooltipWarner(
      Collections.singletonList("AbgegrieftHD"), 2, SignatureExtractor.DEFAULT_PATTERN, true);

  @Test
  void signaturFakeNurEinmal() {
    ChatWarner chat = new ChatWarner();
    List<String> lines = Arrays.asList("§cHauptgewinn-Ticket (#0403)",
        "§5§o§7Signiert von §aAbgegriefHD §7am §e24.10.2026");
    assertEquals(Arrays.asList("Hauptgewinn-Ticket: signiert von AbgegriefHD"),
        chat.newMessages(this.warner.findings(lines, false)));
    // Erneutes Hovern (auch ohne F3+H) meldet nichts mehr
    assertTrue(chat.newMessages(this.warner.findings(Arrays.asList("Hauptgewinn-Ticket",
        "Signiert von AbgegriefHD am 24.10.2026"), false)).isEmpty());
  }

  @Test
  void anderesDatumIstNeuesItem() {
    ChatWarner chat = new ChatWarner();
    chat.newMessages(this.warner.findings(Arrays.asList("Perk",
        "Signiert von AbgegriefHD am 24.10.2026"), false));
    assertEquals(1, chat.newMessages(this.warner.findings(Arrays.asList("Perk",
        "Signiert von AbgegriefHD am 25.10.2026"), false)).size());
  }

  @Test
  void icedOutMeldung() {
    ChatWarner chat = new ChatWarner();
    assertEquals(Arrays.asList("*Iced Out* Schaf: Iced Out ohne Verzauberung, vermutlich gefälscht"),
        chat.newMessages(this.warner.findings(Arrays.asList("*Iced Out* Schaf"), false)));
  }

  @Test
  void echteSignaturMeldetNichts() {
    assertTrue(new ChatWarner().newMessages(this.warner.findings(Arrays.asList("Perk",
        "Signiert von AbgegrieftHD am 24.10.2026"), false)).isEmpty());
  }
}
