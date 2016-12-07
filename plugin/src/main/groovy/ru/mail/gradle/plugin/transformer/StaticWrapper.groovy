package ru.mail.gradle.plugin.transformer

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class StaticWrapper extends ClassVisitor {

    boolean ignore
    String ignoreClass
    Map<CallDescription, Replacement> replacements

    public StaticWrapper(String ignoreClass, Map<CallDescription, Replacement> replacements) {
        super(Opcodes.ASM5, new ClassWriter(ClassWriter.COMPUTE_MAXS))
        this.ignoreClass = ignoreClass.replace('.', '/')
        this.replacements = replacements
    }

    byte[] toByteArray() {
        cv.toByteArray()
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
        ignore = name == ignoreClass
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        def defaultMethodVisitor = super.visitMethod(access, name, desc, signature, exceptions)
        if (ignore) {
            return defaultMethodVisitor
        }
        return new MethodVisitor(Opcodes.ASM5, defaultMethodVisitor) {
            @Override
            void visitMethodInsn(int opcode,
                                 String insnOwner,
                                 String insnName,
                                 String insnDesc,
                                 boolean itf) {
                replacements.each { replaceable, replacement ->
                    if (insnName == replaceable.methodName &&
                            insnOwner == replaceable.owner &&
                            insnDesc == replaceable.desc) {
                        opcode = Opcodes.INVOKESTATIC
                        insnOwner = replacement.owner
                        insnName = replacement.methodName
                        insnDesc = "(L$replaceable.owner;${replaceable.desc[1..-1]}"
                        return false
                    }
                }
                super.visitMethodInsn(opcode, insnOwner, insnName, insnDesc, itf)
            }

        }
    }

}
