package de.bjarne.ahfakewarner.asm;

import de.bjarne.ahfakewarner.hook.SlotHook;
import de.bjarne.ahfakewarner.hook.TooltipHook;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Eingriffe in Minecraft 1.8.9, für die die LabyMod-3-API keine Events hat:
 * <ul>
 *   <li>{@code ItemStack.getTooltip}: Warnzeilen anhängen ({@link TooltipHook})</li>
 *   <li>{@code ItemStack}: Zugriff auf Tooltip und Verzauberungen für das Addon</li>
 *   <li>{@code GuiContainer.drawSlot}: roter Rahmen um gefälschte Items ({@link SlotHook})</li>
 * </ul>
 *
 * LabyMod 3 lässt Addon-Transformer vor der Forge-Deobfuskierung laufen, daher kommen
 * die Klassen hier immer mit Notch-Namen an (geprüft gegen joined.srg von MCP 1.8.9).
 * Eingefügt wird nur geradliniger Code ohne Sprünge, damit die vorhandenen Frames gültig
 * bleiben und keine Klassen zum Neuberechnen geladen werden müssen.
 */
public class AhFakeWarnerTransformer implements IClassTransformer {

  // net.minecraft.item.ItemStack
  private static final String ITEM_STACK = "zx";
  // ItemStack.getTooltip(EntityPlayer, boolean), SRG func_82840_a
  private static final String GET_TOOLTIP = "a";
  private static final String GET_TOOLTIP_DESC = "(Lwn;Z)Ljava/util/List;";
  // net.minecraft.entity.player.EntityPlayer
  private static final String ENTITY_PLAYER = "wn";
  // EnchantmentHelper.getEnchantments(ItemStack), SRG func_82781_a
  private static final String ENCHANTMENT_HELPER = "ack";
  private static final String GET_ENCHANTMENTS = "a";
  private static final String GET_ENCHANTMENTS_DESC = "(Lzx;)Ljava/util/Map;";
  // Minecraft.getMinecraft() (func_71410_x) und Minecraft.thePlayer (field_71439_g)
  private static final String MINECRAFT = "ave";
  private static final String GET_MINECRAFT = "A";
  private static final String THE_PLAYER = "h";
  private static final String THE_PLAYER_DESC = "Lbew;";

  // net.minecraft.client.gui.inventory.GuiContainer.drawSlot(Slot), SRG func_146977_a
  private static final String GUI_CONTAINER = "ayl";
  private static final String DRAW_SLOT = "a";
  private static final String DRAW_SLOT_DESC = "(Lyg;)V";
  // net.minecraft.inventory.Slot: getStack() (func_75211_c), xDisplayPosition, yDisplayPosition
  private static final String SLOT = "yg";
  private static final String SLOT_GET_STACK = "d";
  private static final String SLOT_X = "f";
  private static final String SLOT_Y = "g";

  private static final String TOOLTIP_HOOK = "de/bjarne/ahfakewarner/hook/TooltipHook";
  private static final String SLOT_HOOK = "de/bjarne/ahfakewarner/hook/SlotHook";
  private static final String ITEM_SOURCE = "de/bjarne/ahfakewarner/hook/ItemSource";

  @Override
  public byte[] transform(String name, String transformedName, byte[] bytes) {
    if (bytes == null) {
      return bytes;
    }
    if (ITEM_STACK.equals(name)) {
      return patch(name, bytes, true);
    }
    if (GUI_CONTAINER.equals(name)) {
      return patch(name, bytes, false);
    }
    return bytes;
  }

  private static byte[] patch(String name, byte[] bytes, boolean itemStack) {
    try {
      ClassNode node = new ClassNode();
      new ClassReader(bytes).accept(node, 0);

      boolean ok = itemStack ? patchItemStack(node) : patchGuiContainer(node);
      if (!ok) {
        System.err.println("[AH-Fake-Warner] Methode in " + name + " nicht gefunden, Eingriff fehlt");
        return bytes;
      }

      ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      node.accept(writer);
      if (itemStack) {
        TooltipHook.markInstalled();
      } else {
        SlotHook.markInstalled();
      }
      System.out.println("[AH-Fake-Warner] Eingriff in " + name + " eingebaut");
      return writer.toByteArray();
    } catch (Throwable throwable) {
      System.err.println("[AH-Fake-Warner] Eingriff in " + name + " fehlgeschlagen, Original bleibt");
      throwable.printStackTrace();
      return bytes;
    }
  }

  private static boolean patchItemStack(ClassNode node) {
    int patched = 0;
    for (MethodNode method : node.methods) {
      if (GET_TOOLTIP.equals(method.name) && GET_TOOLTIP_DESC.equals(method.desc)) {
        patched += patchTooltipReturns(method);
      }
    }
    if (patched == 0) {
      return false;
    }

    node.interfaces.add(ITEM_SOURCE);
    node.methods.add(linesMethod());
    node.methods.add(enchantmentsMethod());
    return true;
  }

  // Vor jedem "areturn": Liste duplizieren, Verzauberungen holen, Hook aufrufen
  private static int patchTooltipReturns(MethodNode method) {
    int count = 0;
    for (AbstractInsnNode insn : method.instructions.toArray()) {
      if (insn.getOpcode() != Opcodes.ARETURN) {
        continue;
      }
      InsnList hook = new InsnList();
      hook.add(new InsnNode(Opcodes.DUP));
      hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
      hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ENCHANTMENT_HELPER, GET_ENCHANTMENTS,
          GET_ENCHANTMENTS_DESC, false));
      hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TOOLTIP_HOOK, "onTooltip",
          "(Ljava/util/List;Ljava/util/Map;)V", false));
      method.instructions.insertBefore(insn, hook);
      count++;
    }
    return count;
  }

  // public List ahfw$lines() { return this.getTooltip(Minecraft.getMinecraft().thePlayer, false); }
  private static MethodNode linesMethod() {
    MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "ahfw$lines", "()Ljava/util/List;",
        null, null);
    InsnList code = method.instructions;
    code.add(new VarInsnNode(Opcodes.ALOAD, 0));
    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MINECRAFT, GET_MINECRAFT,
        "()L" + MINECRAFT + ";", false));
    code.add(new FieldInsnNode(Opcodes.GETFIELD, MINECRAFT, THE_PLAYER, THE_PLAYER_DESC));
    // Cast statt Subtyp-Prüfung, damit der Verifier keine Spielerklassen vorzeitig lädt
    code.add(new TypeInsnNode(Opcodes.CHECKCAST, ENTITY_PLAYER));
    code.add(new InsnNode(Opcodes.ICONST_0));
    code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, ITEM_STACK, GET_TOOLTIP, GET_TOOLTIP_DESC,
        false));
    code.add(new InsnNode(Opcodes.ARETURN));
    return method;
  }

  // public Map ahfw$enchantments() { return EnchantmentHelper.getEnchantments(this); }
  private static MethodNode enchantmentsMethod() {
    MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "ahfw$enchantments",
        "()Ljava/util/Map;", null, null);
    InsnList code = method.instructions;
    code.add(new VarInsnNode(Opcodes.ALOAD, 0));
    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ENCHANTMENT_HELPER, GET_ENCHANTMENTS,
        GET_ENCHANTMENTS_DESC, false));
    code.add(new InsnNode(Opcodes.ARETURN));
    return method;
  }

  // Vor jedem "return" von drawSlot: SlotHook.afterDrawSlot(slot.getStack(), slot.x, slot.y)
  private static boolean patchGuiContainer(ClassNode node) {
    int patched = 0;
    for (MethodNode method : node.methods) {
      if (!DRAW_SLOT.equals(method.name) || !DRAW_SLOT_DESC.equals(method.desc)) {
        continue;
      }
      for (AbstractInsnNode insn : method.instructions.toArray()) {
        if (insn.getOpcode() != Opcodes.RETURN) {
          continue;
        }
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SLOT, SLOT_GET_STACK,
            "()L" + ITEM_STACK + ";", false));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new FieldInsnNode(Opcodes.GETFIELD, SLOT, SLOT_X, "I"));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new FieldInsnNode(Opcodes.GETFIELD, SLOT, SLOT_Y, "I"));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SLOT_HOOK, "afterDrawSlot",
            "(Ljava/lang/Object;II)V", false));
        method.instructions.insertBefore(insn, hook);
        patched++;
      }
    }
    return patched > 0;
  }
}
