package ru.mail.gradle.plugin.kali

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KaliClassVisitor extends ClassVisitor {

    final Logger logger
    boolean ignore
    Set<String> ignoreClasses
    List<Replacement> replacements
    List<Replacement> replacementsRegex
    String className
    final PreparedInfo preparedInfo
    boolean classNameWasShown

    public KaliClassVisitor(Set<String> ignoreClasses,
                            List<Replacement> replacements,
                            List<Replacement> replacementsRegex,
                            PreparedInfo info) {
        super(Opcodes.ASM5, new ClassWriter(ClassWriter.COMPUTE_MAXS))
        this.logger = LoggerFactory.getLogger(KaliClassVisitor)
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
        className = name
    }

    @Override
    FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (preparedInfo.hasAccessor(className, name, desc)) {
            access = (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC
            showClassNameOnce()
            logger.info "  Making public: $name"
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
                    showClassNameOnce()
                    logger.info "  Replace invocation: ${insnOwner.replace('/', '.')}.$insnName$insnDesc -> $matchedReplacement.to"
                    opcode = Opcodes.INVOKESTATIC
                    (insnOwner, insnName, insnDesc) = matchedReplacement.to.toStaticInvocation(insnOwner, insnDesc)
                } else {
                    matchedReplacement = replacementsRegex.find { replacement ->
                        return replacement.from.matches(insnOwner, insnName, insnDesc)
                    }
                    if (matchedReplacement) {
                        showClassNameOnce()
                        logger.info "  Replace invocation (by regex): ${insnOwner.replace('/', '.')}.$insnName$insnDesc -> $matchedReplacement.to"
                        opcode = Opcodes.INVOKESTATIC
                        (insnOwner, insnName, insnDesc) = matchedReplacement.to.toStaticInvocation(insnOwner, insnDesc)
                    }
                }

                def accessor = preparedInfo.getAccessor(insnOwner, insnName, insnDesc)
                if (accessor) {
                    showClassNameOnce()
                    logger.info "  Inline accessor invocation for: ${accessor.field.owner.replace('/', '.')}.$accessor.field.name"
                    accessor.accept(this)
                    return
                }

                super.visitMethodInsn(opcode, insnOwner, insnName, insnDesc, itf)
            }

        }
    }

    def showClassNameOnce() {
        if (!classNameWasShown) {
            logger.info "Processing class ${className.replace('/', '.')}:"
            classNameWasShown = true
        }
    }

}
