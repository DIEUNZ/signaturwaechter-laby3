package de.bjarne.ahfakewarner.hook;

import java.util.List;
import java.util.Map;

/**
 * Wird per ASM an {@code ItemStack} angehängt, damit das Addon Tooltip-Zeilen und
 * Verzauberungen lesen kann, ohne selbst Minecraft-Klassen zu kennen.
 */
public interface ItemSource {

  /** Tooltip-Zeilen mit Farbcodes, wie sie auch beim Hovern entstehen. */
  List<String> ahfw$lines();

  /** Verzauberungen (ID → Stufe) aus {@code EnchantmentHelper.getEnchantments}. */
  Map<?, ?> ahfw$enchantments();

  /** Numerische Item-ID ({@code Item.getIdFromItem}), z. B. 383 für Spawn-Eier in 1.8.9. */
  int ahfw$itemId();

  /** Schadenswert ({@code getItemDamage}); bei Spawn-Eiern in 1.8.9 die Entity-ID. */
  int ahfw$damage();
}
