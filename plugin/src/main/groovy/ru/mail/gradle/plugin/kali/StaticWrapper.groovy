package ru.mail.gradle.plugin.kali

import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class StaticWrapper extends BaseClassProcessor {

    boolean ignore
    Set<String> ignoreClasses
    List<Replacement> replacements
    List<Replacement> replacementsRegex
    String name
    final PreparedInfo preparedInfo

    public StaticWrapper(Set<String> ignoreClasses,
                         List<Replacement> replacements,
                         List<Replacement> replacementsRegex,
                         PreparedInfo info) {
        this.ignoreClasses = ignoreClasses
        this.replacements = replacements
        this.replacementsRegex = replacementsRegex
        this.preparedInfo = info
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
                boolean replaced = false
                replacements.each { replacement ->
                    if (replacement.from.equals(insnOwner, insnName, insnDesc)) {
                        opcode = Opcodes.INVOKESTATIC
                        insnOwner = replacement.to.owner
                        insnName = replacement.to.methodName
                        insnDesc = replacement.to.descriptor
                        if (!insnDesc) {
                            insnDesc = "(L$replacement.from.owner;${replacement.from.descriptor[1..-1]}"
                        }
                        replaced = true
                        return
                    }
                }
                if (!replaced) {
                    replacementsRegex.each { replacement ->
                        if (replacement.from.matches(insnOwner, insnName, insnDesc)) {
                            opcode = Opcodes.INVOKESTATIC
                            if (replacement.to.descriptor) {
                                insnDesc = replacement.to.descriptor
                            } else {
                                insnDesc = "(L$insnOwner;${insnDesc[1..-1]}"
                            }
                            insnOwner = replacement.to.owner
                            insnName = replacement.to.methodName
                            return
                        }
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
