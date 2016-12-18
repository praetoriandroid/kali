package ru.mail.gradle.plugin.kali

import org.codehaus.groovy.runtime.StackTraceUtils
import org.objectweb.asm.*
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode

class PrepareVisitor extends ClassVisitor {

    ClassInfo.Builder builder

    String className

    PrepareVisitor() {
        super(Opcodes.ASM5)
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        builder = new ClassInfo.Builder().setName(name)
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (isSynthetic(access) && !isBridge(access)) {
            return new MethodAnalyzeVisitor(access, signature) {
                @Override
                void visitEnd() {
                    if (step == Step.done) {
                        builder.addAccessor(name, desc, new AccessorInfo(inlineInsn, field))
                    }
                }
            }
        }
        return null
    }

    ClassInfo build() {
        builder.build()
    }

    private static boolean isSynthetic(int access) {
        masks(access, Opcodes.ACC_SYNTHETIC)
    }

    private static boolean isStatic(int access) {
        masks(access, Opcodes.ACC_STATIC)
    }

    private static boolean isBridge(int access) {
        masks(access, Opcodes.ACC_BRIDGE)
    }

    private static boolean masks(int value, int mask) {
        (value & mask) == mask
    }

    private class MethodAnalyzeVisitor extends MethodVisitor {

        private final acccess
        private final signature

        final inlineInsn = new InsnList()
        FieldInfo field

        enum Step {
            awaitForLoad0,
            awaitForLoad1OrGetField,
            awaitForDupX1,
            awaitForPutField,
            awaitForReturn,
            done,
            skip
        }

        Step step = Step.awaitForLoad0

        MethodAnalyzeVisitor(int access, String signature) {
            super(Opcodes.ASM5)
            this.acccess = access
            this.signature = signature
            if (!isStatic(access)) {
                skip()
            }
        }

        @Override
        void visitInsn(int opcode) {
            if (step == Step.skip) {
                return
            }
            switch (opcode) {
                case Opcodes.DUP_X1:
                case Opcodes.DUP2_X1:
                    if (step == Step.awaitForDupX1) {
                        step = Step.awaitForPutField
                        inlineInsn.add(new InsnNode(opcode))
                        break
                    }
                    skip()
                    break
                case Opcodes.ARETURN:
                case Opcodes.DRETURN:
                case Opcodes.FRETURN:
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                    if (step == Step.awaitForReturn) {
                        step = Step.done
                        break
                    }
                    skip()
                    break
                default:
                    skip()
                    break
            }
        }

        @Override
        void visitVarInsn(int opcode, int var) {
            if (step == Step.skip) {
                return
            }
            switch (opcode) {
                case Opcodes.ALOAD:
                case Opcodes.DLOAD:
                case Opcodes.FLOAD:
                case Opcodes.ILOAD:
                case Opcodes.LLOAD:
                    if (step == Step.awaitForLoad0) {
                        if (var == 0) {
                            step = Step.awaitForLoad1OrGetField
                            break
                        }
                    } else if (step == Step.awaitForLoad1OrGetField) {
                        if (var == 1) {
                            step = Step.awaitForDupX1
                            break
                        }
                    }
                    skip()
                    break
                default:
                    skip()
                    break
            }
        }

        @Override
        void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (step == Step.skip) {
                return
            }
            switch (opcode) {
                case Opcodes.GETFIELD:
                    if (step == Step.awaitForLoad1OrGetField) {
                        awaitForReturn(opcode, new FieldInfo(owner, name, desc))
                        break
                    }
                    skip()
                    break
                case Opcodes.PUTFIELD:
                    if (step == Step.awaitForPutField) {
                        awaitForReturn(opcode, new FieldInfo(owner, name, desc))
                        break
                    }
                    skip()
                    break
                default:
                    skip()
                    break
            }
        }

        private void awaitForReturn(int opcode, FieldInfo info) {
            step = Step.awaitForReturn
            field = info
            inlineInsn.add(new FieldInsnNode(opcode, info.owner, info.name, info.desc))
        }

        @Override
        void visitIntInsn(int opcode, int operand) {
            skip()
        }

        @Override
        void visitTypeInsn(int opcode, String type) {
            skip()
        }

        @Override
        void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            skip()
        }

        @Override
        void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            skip()
        }

        @Override
        void visitJumpInsn(int opcode, Label label) {
            skip()
        }

        @Override
        void visitLdcInsn(Object cst) {
            skip()
        }

        @Override
        void visitIincInsn(int var, int increment) {
            skip()
        }

        @Override
        void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            skip()
        }

        @Override
        void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            skip()
        }

        @Override
        void visitMultiANewArrayInsn(String desc, int dims) {
            skip()
        }

        @Override
        void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            skip()
        }

        private void skip() {
            step = Step.skip
            inlineInsn.clear()
        }

    }

}
