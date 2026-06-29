package site.gtnhserverlocalization.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class ProspectingPacketTransformer implements IClassTransformer {

    private static final String TARGET_CLASS = "detrav.net.ProspectingPacket";
    private static final String ITEM_STACK = "net/minecraft/item/ItemStack";
    private static final String HELPER = "site/gtnhserverlocalization/ProspectionNameHelper";
    private static final String ADD_BLOCK_DESC = "(IIILnet/minecraft/block/Block;I)V";

    @Override
    public byte[] transform(final String name, final String transformedName, final byte[] basicClass) {
        final String className = transformedName == null ? name : transformedName;
        if (basicClass == null || !TARGET_CLASS.equals(className)) {
            return basicClass;
        }

        try {
            return transformProspectingPacket(basicClass);
        } catch (final ThreadDeath e) {
            throw e;
        } catch (final Throwable e) {
            System.err.println("[GTNHServerLocalization] Failed to patch " + TARGET_CLASS);
            e.printStackTrace();
            return basicClass;
        }
    }

    private static byte[] transformProspectingPacket(final byte[] basicClass) {
        final ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        int patchedCalls = 0;
        for (final MethodNode method : classNode.methods) {
            if ("addBlock".equals(method.name) && ADD_BLOCK_DESC.equals(method.desc)) {
                patchedCalls += patchDisplayNameCalls(method);
            }
        }

        if (patchedCalls == 0) {
            System.err
                .println("[GTNHServerLocalization] No ProspectingPacket.addBlock display-name calls were patched.");
            return basicClass;
        }

        final ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        System.out.println(
            "[GTNHServerLocalization] Patched " + patchedCalls + " ProspectingPacket.addBlock display-name call(s).");
        return writer.toByteArray();
    }

    private static int patchDisplayNameCalls(final MethodNode method) {
        int patchedCalls = 0;
        AbstractInsnNode instruction = method.instructions.getFirst();
        while (instruction != null) {
            final AbstractInsnNode next = instruction.getNext();
            if (isItemStackDisplayNameCall(instruction)) {
                final AbstractInsnNode previous = getPreviousRealInstruction(instruction);
                if (previous != null && previous.getOpcode() == Opcodes.ALOAD) {
                    final InsnList replacement = new InsnList();
                    replacement.add(new VarInsnNode(Opcodes.ALOAD, 4));
                    replacement.add(new VarInsnNode(Opcodes.ILOAD, 5));
                    replacement.add(
                        new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            HELPER,
                            "getDisplayName",
                            "(Lnet/minecraft/block/Block;I)Ljava/lang/String;",
                            false));

                    method.instructions.insertBefore(previous, replacement);
                    method.instructions.remove(previous);
                    method.instructions.remove(instruction);
                    patchedCalls++;
                }
            }
            instruction = next;
        }
        return patchedCalls;
    }

    private static boolean isItemStackDisplayNameCall(final AbstractInsnNode instruction) {
        if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) {
            return false;
        }

        final MethodInsnNode methodInsn = (MethodInsnNode) instruction;
        return ITEM_STACK.equals(methodInsn.owner) && "func_82833_r".equals(methodInsn.name)
            && "()Ljava/lang/String;".equals(methodInsn.desc);
    }

    private static AbstractInsnNode getPreviousRealInstruction(final AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null
            && (previous.getType() == AbstractInsnNode.LABEL || previous.getType() == AbstractInsnNode.LINE
                || previous.getType() == AbstractInsnNode.FRAME)) {
            previous = previous.getPrevious();
        }
        return previous;
    }
}
