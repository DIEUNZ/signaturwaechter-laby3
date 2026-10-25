package de.bjarne.ahfakewarner.hook;

import de.bjarne.ahfakewarner.TooltipWarner;
import java.util.Map;
import java.util.WeakHashMap;
import org.lwjgl.opengl.GL11;

/**
 * Wird am Ende von {@code GuiContainer.drawSlot} aufgerufen und zeichnet einen roten
 * Rahmen um Slots mit gefälschtem Item. Läuft nur im Render-Thread.
 */
public final class SlotHook {

  private static final int SLOT_SIZE = 16;
  // Kurzer Cache, damit nicht jeder Frame den Tooltip neu baut; läuft ab, weil
  // GG die Lore offener Auktionen laufend ändert
  private static final long CACHE_MILLIS = 5000;

  private static final Map<Object, Entry> CACHE = new WeakHashMap<Object, Entry>();
  private static volatile boolean enabled = true;
  private static boolean errorLogged;

  private SlotHook() {
  }

  /**
   * @param stack Item im Slot ({@code ItemStack}) oder {@code null}
   * @param x     Slot-Position links (relativ zur GUI)
   * @param y     Slot-Position oben (relativ zur GUI)
   */
  public static void afterDrawSlot(Object stack, int x, int y) {
    if (!enabled || !(stack instanceof ItemSource)) {
      return;
    }
    try {
      if (isWarning((ItemSource) stack)) {
        drawFrame(x, y);
      }
    } catch (Throwable throwable) {
      if (!errorLogged) {
        errorLogged = true;
        System.err.println("[AH-Fake-Warner] Fehler beim Slot-Rahmen");
        throwable.printStackTrace();
      }
    }
  }

  private static boolean isWarning(ItemSource source) {
    TooltipWarner warner = TooltipHook.warner();
    if (warner == null) {
      return false;
    }

    long now = System.currentTimeMillis();
    Entry cached = CACHE.get(source);
    if (cached != null && cached.warner == warner && now - cached.computedAt < CACHE_MILLIS) {
      return cached.warning;
    }

    boolean warning = warner.isWarning(TooltipHook.linesWithoutWarnings(source),
        !source.ahfw$enchantments().isEmpty());
    CACHE.put(source, new Entry(warner, warning, now));
    return warning;
  }

  // 1 Pixel breiter Rahmen innerhalb des 16x16-Slots; GL-Zustand wird komplett zurückgesetzt,
  // damit der Zustands-Cache von Minecraft stimmt
  private static void drawFrame(int x, int y) {
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
    try {
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_BLEND);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glColor4f(1F, 0.125F, 0.125F, 1F);
      GL11.glBegin(GL11.GL_QUADS);
      quad(x, y, x + SLOT_SIZE, y + 1);
      quad(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE);
      quad(x, y + 1, x + 1, y + SLOT_SIZE - 1);
      quad(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE - 1);
      GL11.glEnd();
    } finally {
      GL11.glPopAttrib();
    }
  }

  private static void quad(int left, int top, int right, int bottom) {
    GL11.glVertex2f(left, top);
    GL11.glVertex2f(left, bottom);
    GL11.glVertex2f(right, bottom);
    GL11.glVertex2f(right, top);
  }

  public static void setEnabled(boolean value) {
    enabled = value;
  }

  private static volatile boolean installed;

  /** Vom Transformer gesetzt, sobald der Eingriff in {@code GuiContainer} geklappt hat. */
  public static void markInstalled() {
    installed = true;
  }

  public static boolean isInstalled() {
    return installed;
  }

  private static final class Entry {

    final TooltipWarner warner;
    final boolean warning;
    final long computedAt;

    Entry(TooltipWarner warner, boolean warning, long computedAt) {
      this.warner = warner;
      this.warning = warning;
      this.computedAt = computedAt;
    }
  }
}
