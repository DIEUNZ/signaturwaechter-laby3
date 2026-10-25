package de.bjarne.ahfakewarner.asm;

import de.bjarne.ahfakewarner.hook.TooltipHook;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Hängt einen Aufruf von {@link TooltipHook#onTooltip} an jedes {@code return} von
 * {@code ItemStack.getTooltip(EntityPlayer, boolean)}.
 *
 * LabyMod 3 lässt Addon-Transformer vor der Forge-Deobfuskierung laufen, daher kommen
 * die Klassen hier immer mit Notch-Namen an (geprüft gegen joined.srg von MCP 1.8.9).
 */
public class AhFakeWarnerTransformer implements IClassTransformer {

  // net.minecraft.item.ItemStack
  private static final String ITEM_STACK = "zx";
  // ItemStack.getTooltip(EntityPlayer, boolean), SRG func_82840_a
  private static final String GET_TOOLTIP = "a";
  private static final String GET_TOOLTIP_DESC = "(Lwn;Z)Ljava/util/List;";
  // EnchantmentHelper.getEnchantments(ItemStack), SRG func_82781_a
  private static final String ENCHANTMENT_HELPER = "ack";
  private static final String GET_ENCHANTMENTS = "a";
  private static final String GET_ENCHANTMENTS_DESC = "(Lzx;)Ljava/util/Map;";

  private static final String HOOK = "de/bjarne/ahfakewarner/hook/TooltipHook";

  @Override
  public byte[] transform(String name, String transformedName, byte[] bytes) {
    if (bytes == null || !ITEM_STACK.equals(name)) {
      return bytes;
    }

    try {
      ClassNode node = new ClassNode();
      new ClassReader(bytes).accept(node, 0);

      int patched = 0;
      for (MethodNode method : node.methods) {
        if (GET_TOOLTIP.equals(method.name) && GET_TOOLTIP_DESC.equals(method.desc)) {
          patched += patchReturns(method);
        }
      }
      if (patched == 0) {
        System.err.println("[AH-Fake-Warner] getTooltip nicht gefunden, Tooltip-Warnung inaktiv");
        return bytes;
      }

      // Frames bleiben gültig, weil nur geradliniger Code vor dem return eingefügt wird
      ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      node.accept(writer);
      TooltipHook.markInstalled();
      System.out.println("[AH-Fake-Warner] Tooltip-Eingriff eingebaut (" + patched + " return)");
      return writer.toByteArray();
    } catch (Throwable throwable) {
      System.err.println("[AH-Fake-Warner] Tooltip-Eingriff fehlgeschlagen, Original bleibt");
      throwable.printStackTrace();
      return bytes;
    }
  }

  // Vor jedem "areturn": Liste duplizieren, Verzauberungen holen, Hook aufrufen
  private static int patchReturns(MethodNode method) {
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
      hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "onTooltip",
          "(Ljava/util/List;Ljava/util/Map;)V", false));
      method.instructions.insertBefore(insn, hook);
      count++;
    }
    return count;
  }
}
