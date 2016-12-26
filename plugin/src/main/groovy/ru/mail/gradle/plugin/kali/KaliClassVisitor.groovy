package ru.mail.gradle.plugin.kali

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class KaliClassVisitor extends ClassVisitor {

    boolean ignore
    Set<String> ignoreClasses
    List<Replacement> replacements
    List<Replacement> replacementsRegex
    String name
    final PreparedInfo preparedInfo

    public KaliClassVisitor(Set<String> ignoreClasses,
                            List<Replacement> replacements,
                            List<Replacement> replacementsRegex,
                            PreparedInfo info) {
        super(Opcodes.ASM5, new ClassWriter(ClassWriter.COMPUTE_MAXS))
        this.ignoreClasses = ignoreClasses
        this.replacements = replacements
        this.replacementsRegex = replacementsRegex
        this.preparedInfo = info
    }

    byte[] toByteArray() {
        cv.toByteArray()
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
        ignore = ignoreClasses.contains(name)
        this.name = name
    }

    @Override
    FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (preparedInfo.hasAccessor(this.name, name, desc)) {
            access = (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC
        }
        return super.visitField(access, name, desc, signature, value)
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
                Replacement matchedReplacement = replacements.find { replacement ->
                    return replacement.from.equals(insnOwner, insnName, insnDesc)
                }
                if (matchedReplacement) {
                    opcode = Opcodes.INVOKESTATIC
                    insnOwner = matchedReplacement.to.owner
                    insnName = matchedReplacement.to.methodName
                    insnDesc = matchedReplacement.to.descriptor
                    if (!insnDesc) {
                        insnDesc = "(L$matchedReplacement.from.owner;${matchedReplacement.from.descriptor[1..-1]}"
                    }
                } else {
                    matchedReplacement = replacementsRegex.find { replacement ->
                        return replacement.from.matches(insnOwner, insnName, insnDesc)
                    }
                    if (matchedReplacement) {
                        opcode = Opcodes.INVOKESTATIC
                        if (matchedReplacement.to.descriptor) {
                            insnDesc = matchedReplacement.to.descriptor
                        } else {
                            insnDesc = "(L$insnOwner;${insnDesc[1..-1]}"
                        }
                        insnOwner = matchedReplacement.to.owner
                        insnName = matchedReplacement.to.methodName
                    }
                }

                def accessor = preparedInfo.getAccessor(insnOwner, insnName, insnDesc)
                if (accessor) {
                    accessor.accept(this)
                    return
                }

                super.visitMethodInsn(opcode, insnOwner, insnName, insnDesc, itf)
            }

        }
    }

}
