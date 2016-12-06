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
        println("***  [M] $name: $desc")
        return new MethodVisitor(Opcodes.ASM5, defaultMethodVisitor) {
            @Override
            void visitMethodInsn(int opcode,
                                 String owner,
                                 String insnName,
                                 String insnDesc,
                                 boolean itf) {
                println("*** visitMethodInsn(${opName(opcode)}, $owner, $insnName, $insnDesc, $itf)")
                if (insnName == 'acquire' && owner == 'android/os/PowerManager$WakeLock') {
                    owner = 'com/example/gte/FakeLock'
                    if (insnDesc == '()V') {
                        opcode = Opcodes.INVOKESTATIC
                        insnDesc = '(Landroid/os/PowerManager$WakeLock)V'
                        println("--- REPLACE owner to FakeLock and call to static acquire()")
                    } else if (insnDesc == '(J)V') {
                        opcode = Opcodes.INVOKESTATIC
                        insnDesc = '(Landroid/os/PowerManager$WakeLock;J)V'
                        println("--- REPLACE owner to FakeLock and call to static acquire(timeout)")
                    } else {
                        println("------ NOT REPLACED: bad aqcuire()")
                    }
                } else if (insnName == 'release' && owner == 'android/os/PowerManager$WakeLock') {
                    opcode = Opcodes.INVOKESTATIC
                    owner = 'com/example/gte/FakeLock'
                    insnDesc = '(Landroid/os/PowerManager$WakeLock;)V'
                    println("--- REPLACE owner to FakeLock call to static release()")
                }
                super.visitMethodInsn(opcode, owner, insnName, insnDesc, itf)
            }

            String opName(int opcode) {
                switch (opcode) {
                    case Opcodes.INVOKEVIRTUAL:
                        return 'INVOKEVIRTUAL'
                    case Opcodes.INVOKESPECIAL:
                        return 'INVOKESPECIAL'
                    case Opcodes.INVOKESTATIC:
                        return 'INVOKESTATIC'
                    case Opcodes.INVOKEINTERFACE:
                        return 'INVOKEINTERFACE'

                    case Opcodes.ILOAD:
                        return 'ILOAD'
                    case Opcodes.LLOAD:
                        return 'LLOAD'
                    case Opcodes.FLOAD:
                        return 'FLOAD'
                    case Opcodes.DLOAD:
                        return 'DLOAD'
                    case Opcodes.ALOAD:
                        return 'ALOAD'
                    case Opcodes.ISTORE:
                        return 'ISTORE'
                    case Opcodes.LSTORE:
                        return 'LSTORE'
                    case Opcodes.FSTORE:
                        return 'FSTORE'
                    case Opcodes.DSTORE:
                        return 'DSTORE'
                    case Opcodes.ASTORE:
                        return 'ASTORE'
                    case Opcodes.RET:
                        return 'RET'

                    case Opcodes.RETURN:
                        return 'RETURN'
                    default:
                        return "UNKNOWN($opcode)"
                }
            }


        }
    }

}
