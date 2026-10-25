package de.bjarne.ahfakewarner.hook;

import de.bjarne.ahfakewarner.reminder.ReminderService;

/**
 * Wird am Anfang von {@code GuiContainer.keyTyped} aufgerufen. Die Taste wird nur
 * mitgelesen, Minecraft verarbeitet sie danach ganz normal weiter.
 */
public final class KeyHook {

  private static volatile ReminderService reminders;
  private static volatile ChatOutput output;
  private static volatile boolean installed;
  private static boolean errorLogged;

  private KeyHook() {
  }

  /** @param keyCode LWJGL-Tastencode */
  public static void onContainerKey(int keyCode) {
    ReminderService service = reminders;
    ChatOutput chat = output;
    if (service == null || chat == null) {
      return;
    }
    try {
      String message = service.onKey(keyCode, System.currentTimeMillis());
      if (message != null) {
        chat.send(message);
      }
    } catch (Throwable throwable) {
      if (!errorLogged) {
        errorLogged = true;
        System.err.println("[AH-Fake-Warner] Fehler bei der Timer-Taste");
        throwable.printStackTrace();
      }
    }
  }

  public static void setup(ReminderService service, ChatOutput chat) {
    reminders = service;
    output = chat;
  }

  /** Vom Transformer gesetzt, sobald der Eingriff in {@code GuiContainer.keyTyped} geklappt hat. */
  public static void markInstalled() {
    installed = true;
  }

  public static boolean isInstalled() {
    return installed;
  }
}
