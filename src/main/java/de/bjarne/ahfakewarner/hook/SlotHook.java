package de.bjarne.ahfakewarner.hook;

import de.bjarne.ahfakewarner.SlotAnalysis;
import de.bjarne.ahfakewarner.TooltipWarner;
import de.bjarne.ahfakewarner.reminder.ReminderService;
import java.util.Map;
import java.util.WeakHashMap;
import org.lwjgl.opengl.GL11;

/**
 * Wird aus {@code GuiContainer.drawSlot} aufgerufen: am Anfang färbt es Auktions-Slots hinter
 * dem Item ein (Timer gesetzt / eigenes Gebot), am Ende zeichnet es einen Rahmen um Slots mit
 * gefälschtem Item (rot) bzw. falsch benanntem Spawn-Ei (orange). Läuft nur im Render-Thread.
 */
public final class SlotHook {

  private static final int SLOT_SIZE = 16;
  private static final int STRIPE_WIDTH = 4;
  // Farben wie im LabyMod-4-Addon (RGBA)
  private static final int[] REMINDER_COLOR = {30, 120, 255, 180};
  private static final int[] BID_COLOR = {40, 200, 60, 150};
  // Rahmen: rot (Fälschung, 1 Pixel) bzw. orange (falsch benanntes Spawn-Ei, 2 Pixel)
  private static final int[] FAKE_FRAME = {255, 32, 32, 255};
  private static final int[] EGG_FRAME = {255, 144, 16, 255};
  // Kurzer Cache, damit nicht jeder Frame den Tooltip neu baut; läuft ab, weil
  // GG die Lore offener Auktionen laufend ändert
  private static final long CACHE_MILLIS = 5000;

  private static final Map<Object, Entry> CACHE = new WeakHashMap<Object, Entry>();
  private static volatile boolean frameEnabled = true;
  private static volatile boolean highlightReminder = true;
  private static volatile boolean highlightBid = true;
  private static volatile ReminderService reminders;
  private static volatile String playerName;
  private static volatile boolean installed;
  private static boolean errorLogged;

  private SlotHook() {
  }

  /**
   * Vor dem Item: Hintergrundfarbe für Auktionen mit Timer bzw. eigenem Gebot.
   *
   * @param stack Item im Slot ({@code ItemStack}) oder {@code null}
   * @param x     Slot-Position links (relativ zur GUI)
   * @param y     Slot-Position oben (relativ zur GUI)
   */
  public static void beforeDrawSlot(Object stack, int x, int y) {
    if ((!highlightReminder && !highlightBid) || !(stack instanceof ItemSource)) {
      return;
    }
    try {
      SlotAnalysis analysis = analyze((ItemSource) stack);
      if (analysis.auctionKey() == null) {
        return;
      }
      ReminderService service = reminders;
      boolean bid = highlightBid && analysis.ownBid();
      boolean reminder = highlightReminder && service != null
          && service.hasReminder(analysis.auctionKey(), analysis.endsAtEstimate());
      if (bid && reminder) {
        drawStripes(x, y, BID_COLOR, REMINDER_COLOR);
      } else if (bid || reminder) {
        drawFill(x, y, bid ? BID_COLOR : REMINDER_COLOR);
      }
    } catch (Throwable throwable) {
      logOnce(throwable);
    }
  }

  /**
   * Nach dem Item: roter Rahmen bei Fälschungen.
   *
   * @param stack Item im Slot ({@code ItemStack}) oder {@code null}
   * @param x     Slot-Position links (relativ zur GUI)
   * @param y     Slot-Position oben (relativ zur GUI)
   */
  public static void afterDrawSlot(Object stack, int x, int y) {
    if (!frameEnabled || !(stack instanceof ItemSource)) {
      return;
    }
    try {
      SlotAnalysis analysis = analyze((ItemSource) stack);
      if (analysis.isFake()) {
        // Rot hat Vorrang vor Orange
        drawFrame(x, y, FAKE_FRAME, 1);
      } else if (analysis.isWarning()) {
        drawFrame(x, y, EGG_FRAME, 2);
      }
    } catch (Throwable throwable) {
      logOnce(throwable);
    }
  }

  private static SlotAnalysis analyze(ItemSource source) {
    TooltipWarner warner = TooltipHook.warner();
    long now = System.currentTimeMillis();
    Entry cached = CACHE.get(source);
    if (cached != null && cached.warner == warner && now - cached.computedAt < CACHE_MILLIS) {
      return cached.analysis;
    }

    SlotAnalysis analysis = SlotAnalysis.of(TooltipHook.linesWithoutWarnings(source),
        !source.ahfw$enchantments().isEmpty(), TooltipHook.eggMob(source), warner, playerName,
        now);
    CACHE.put(source, new Entry(warner, analysis, now));
    return analysis;
  }

  private static void logOnce(Throwable throwable) {
    if (!errorLogged) {
      errorLogged = true;
      System.err.println("[AH-Fake-Warner] Fehler beim Zeichnen eines Slots");
      throwable.printStackTrace();
    }
  }

  // GL-Zustand wird jeweils komplett zurückgesetzt, damit der Zustands-Cache von Minecraft stimmt
  private static void begin(boolean blend) {
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_LIGHTING);
    GL11.glDisable(GL11.GL_DEPTH_TEST);
    GL11.glDisable(GL11.GL_CULL_FACE);
    GL11.glDisable(GL11.GL_ALPHA_TEST);
    if (blend) {
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    } else {
      GL11.glDisable(GL11.GL_BLEND);
    }
    GL11.glBegin(GL11.GL_QUADS);
  }

  private static void end() {
    GL11.glEnd();
    GL11.glPopAttrib();
  }

  // Rahmen innerhalb des 16x16-Slots in der angegebenen Breite
  private static void drawFrame(int x, int y, int[] rgba, int width) {
    begin(false);
    try {
      color(rgba);
      quad(x, y, x + SLOT_SIZE, y + width);
      quad(x, y + SLOT_SIZE - width, x + SLOT_SIZE, y + SLOT_SIZE);
      quad(x, y + width, x + width, y + SLOT_SIZE - width);
      quad(x + SLOT_SIZE - width, y + width, x + SLOT_SIZE, y + SLOT_SIZE - width);
    } finally {
      end();
    }
  }

  private static void drawFill(int x, int y, int[] rgba) {
    begin(true);
    try {
      color(rgba);
      quad(x, y, x + SLOT_SIZE, y + SLOT_SIZE);
    } finally {
      end();
    }
  }

  // Diagonale Streifen im Wechsel (Gebot / Timer), zeilenweise in Abschnitten gezeichnet
  private static void drawStripes(int x, int y, int[] first, int[] second) {
    begin(true);
    try {
      for (int row = 0; row < SLOT_SIZE; row++) {
        int column = 0;
        while (column < SLOT_SIZE) {
          int band = (column + row) / STRIPE_WIDTH;
          int end = Math.min(SLOT_SIZE, (band + 1) * STRIPE_WIDTH - row);
          color(band % 2 == 0 ? first : second);
          quad(x + column, y + row, x + end, y + row + 1);
          column = end;
        }
      }
    } finally {
      end();
    }
  }

  private static void color(int[] rgba) {
    GL11.glColor4f(rgba[0] / 255F, rgba[1] / 255F, rgba[2] / 255F, rgba[3] / 255F);
  }

  private static void quad(int left, int top, int right, int bottom) {
    GL11.glVertex2f(left, top);
    GL11.glVertex2f(left, bottom);
    GL11.glVertex2f(right, bottom);
    GL11.glVertex2f(right, top);
  }

  /**
   * @param frame         roter Rahmen um Fälschungen
   * @param reminderColor Slots mit Timer einfärben
   * @param bidColor      Slots mit eigenem Gebot einfärben
   */
  public static void configure(boolean frame, boolean reminderColor, boolean bidColor) {
    frameEnabled = frame;
    highlightReminder = reminderColor;
    highlightBid = bidColor;
  }

  public static void setReminders(ReminderService service) {
    reminders = service;
  }

  public static void setPlayerName(String name) {
    playerName = name;
  }

  /** Vom Transformer gesetzt, sobald der Eingriff in {@code GuiContainer.drawSlot} geklappt hat. */
  public static void markInstalled() {
    installed = true;
  }

  public static boolean isInstalled() {
    return installed;
  }

  private static final class Entry {

    final TooltipWarner warner;
    final SlotAnalysis analysis;
    final long computedAt;

    Entry(TooltipWarner warner, SlotAnalysis analysis, long computedAt) {
      this.warner = warner;
      this.analysis = analysis;
      this.computedAt = computedAt;
    }
  }
}
