package ru.mail.gradle.plugin.transformer

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class StaticWrapper extends ClassVisitor {

    boolean skip

    public StaticWrapper() {
        super(Opcodes.ASM5, new ClassWriter(ClassWriter.COMPUTE_MAXS))
    }

    byte[] toByteArray() {
        cv.toByteArray()
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
        skip = name == 'com/example/gte/FakeLock'
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        def defaultMethodVisitor = super.visitMethod(access, name, desc, signature, exceptions)
        if (skip) {
            return defaultMethodVisitor
        }
        return new MethodVisitor(Opcodes.ASM5, defaultMethodVisitor) {
            @Override
            void visitMethodInsn(int opcode,
                                 String owner,
                                 String insnName,
                                 String insnDesc,
                                 boolean itf) {
                if (insnName == 'acquire' && owner == 'android/os/PowerManager$WakeLock') {
                    owner = 'com/example/gte/FakeLock'
                    if (insnDesc == '()V') {
                        opcode = Opcodes.INVOKESTATIC
                        insnDesc = '(Landroid/os/PowerManager$WakeLock)V'
                    } else if (insnDesc == '(J)V') {
                        opcode = Opcodes.INVOKESTATIC
                        insnDesc = '(Landroid/os/PowerManager$WakeLock;J)V'
                    }
                } else if (insnName == 'release' && owner == 'android/os/PowerManager$WakeLock') {
                    opcode = Opcodes.INVOKESTATIC
                    owner = 'com/example/gte/FakeLock'
                    insnDesc = '(Landroid/os/PowerManager$WakeLock;)V'
                }
                super.visitMethodInsn(opcode, owner, insnName, insnDesc, itf)
            }

        }
    }

}
