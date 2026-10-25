package de.bjarne.ahfakewarner.hook;

/** Gibt Meldungen im Chat aus (vom Addon gesetzt, damit die Hooks keinen LabyMod-Code brauchen). */
public interface ChatOutput {

  void send(String text);
}
