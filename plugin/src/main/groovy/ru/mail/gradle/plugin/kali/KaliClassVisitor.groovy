package ru.mail.gradle.plugin.kali

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static ru.mail.gradle.plugin.kali.VisitorUtils.decodeModifiers
import static ru.mail.gradle.plugin.kali.VisitorUtils.fieldOwner

class KaliClassVisitor extends ClassVisitor {

    final Logger logger
    boolean ignore
    Set<String> ignoreClasses
    List<Replacement> replacements
    List<Replacement> replacementsRegex
    Map<String, String> setFieldModifiers
    Map<String, Boolean> setFieldStaticAccess = [:]
    Set<String> fieldModClasses
    String className
    String humanReadableClassName
    final PreparedInfo preparedInfo
    boolean classNameWasShown

    public KaliClassVisitor(Set<String> ignoreClasses,
                            List<Replacement> replacements,
                            List<Replacement> replacementsRegex,
                            Map<String, String> setFieldModifiers,
                            PreparedInfo info) {
        super(Opcodes.ASM5, new ClassWriter(ClassWriter.COMPUTE_MAXS))
        this.logger = LoggerFactory.getLogger(KaliClassVisitor)
        this.ignoreClasses = ignoreClasses
        this.replacements = replacements
        this.replacementsRegex = replacementsRegex
        this.setFieldModifiers = setFieldModifiers
        this.preparedInfo = info
        fieldModClasses = setFieldModifiers.keySet().collect { String field ->
            return fieldOwner(field)
        }
    }

    byte[] toByteArray() {
        cv.toByteArray()
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
        ignore = ignoreClasses.contains(name)
        className = name
        humanReadableClassName = name.replace('/', '.')
    }

    @Override
    FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        def fullFieldId = "$humanReadableClassName.$name"
        String modifiers = setFieldModifiers[fullFieldId]
        if (modifiers != null) {
            logger.info "  Set modifiers for $fullFieldId to $modifiers"
            int setAccess = decodeModifiers(access, modifiers)
            if ((access & Opcodes.ACC_STATIC) != (setAccess & Opcodes.ACC_STATIC)) {
                setFieldStaticAccess[fullFieldId] = (setAccess & Opcodes.ACC_STATIC) != 0
            }
            access = setAccess
        }
        if (preparedInfo.hasAccessor(className, name, desc)) {
            access = (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC
            showClassNameOnce()
            logger.info "  Making public: $name"
        }
        return super.visitField(access, name, desc, signature, value)
    }

    @Override
    MethodVisitor visitMethod(int methodAccess, String methodName, String methodDesc, String methodSignature, String[] exceptions) {
        def defaultMethodVisitor = super.visitMethod(methodAccess, methodName, methodDesc, methodSignature, exceptions)
        if (ignore) {
            return defaultMethodVisitor
        }
        MethodVisitor kaliMethodVisitor = new MethodVisitor(Opcodes.ASM5, defaultMethodVisitor) {
            @Override
            void visitMethodInsn(int opcode,
                                 String owner,
                                 String name,
                                 String desc,
                                 boolean itf) {
                Replacement matchedReplacement = replacements.find { replacement ->
                    //noinspection ChangeToOperator
                    return replacement.from.equals(owner, name, desc)
                }
                if (matchedReplacement) {
                    showClassNameOnce()
                    logger.info "  Replace invocation: ${owner.replace('/', '.')}.$name$desc -> $matchedReplacement.to"
                    opcode = Opcodes.INVOKESTATIC
                    (owner, name, desc) = matchedReplacement.to.toStaticInvocation(owner, desc, opcode)
                } else {
                    matchedReplacement = replacementsRegex.find { replacement ->
                        return replacement.from.matches(owner, name, desc)
                    }
                    if (matchedReplacement) {
                        showClassNameOnce()
                        logger.info "  Replace invocation (by regex): ${owner.replace('/', '.')}.$name$desc -> $matchedReplacement.to"
                        opcode = Opcodes.INVOKESTATIC
                        (owner, name, desc) = matchedReplacement.to.toStaticInvocation(owner, desc, opcode)
                    }
                }

                def accessor = preparedInfo.getAccessor(owner, name, desc)
                if (accessor) {
                    showClassNameOnce()
                    logger.info "  Inline accessor invocation for: ${accessor.field.owner.replace('/', '.')}.$accessor.field.name"
                    accessor.accept(this)
                    return
                }

                super.visitMethodInsn(opcode, owner, name, desc, itf)
            }
        }

        if (fieldModClasses.contains(humanReadableClassName) && (methodName == '<init>' || methodName == '<clinit>')) {
            def node = new MethodNode(Opcodes.ASM5, methodAccess, methodName, methodDesc, methodSignature, exceptions) {
                @Override
                void visitEnd() {
                    super.visitEnd()

                    ListIterator<AbstractInsnNode> editor = instructions.iterator()
                    while (editor.hasNext()) {
                        AbstractInsnNode instruction = editor.next()
                        if (shouldRemoveAssignment(instruction)) {
                            editor.remove()
                            while (editor.hasPrevious()) {
                                instruction = editor.previous()
                                editor.remove()
                                if (instruction instanceof LineNumberNode) {
                                    break
                                }
                            }
                        }
                        if (instruction.opcode == Opcodes.RETURN) {
                            if (editor.hasPrevious()) {
                                editor.previous()
                            }

                            def classInfo = preparedInfo.getClass(className)
                            if (classInfo == null) {
                                throw new IllegalStateException("Missing class info for $humanReadableClassName in ${preparedInfo.allClasses()}")
                            }
                            boolean needNonStaticInitializers = methodName != '<clinit>'
                            List<List<AbstractInsnNode>> initializers = classInfo.getFieldInitializers(needNonStaticInitializers)
                            initializers.each { initializer ->
                                initializer.each { insn ->
                                    editor.add insn
                                }
                            }
                            break
                        }
                    }
                    accept(kaliMethodVisitor)
                }

                private boolean shouldRemoveAssignment(AbstractInsnNode instruction) {
                    if (instruction.opcode != Opcodes.PUTFIELD && instruction.opcode != Opcodes.PUTSTATIC) {
                        return false
                    }
                    FieldInsnNode fieldInsn = instruction as FieldInsnNode
                    def fullFieldId = VisitorUtils.fieldId(fieldInsn.owner, fieldInsn.name)
                    return setFieldStaticAccess.containsKey(fullFieldId)
                }
            }
            return node
        } else {
            return kaliMethodVisitor
        }
    }

    def showClassNameOnce() {
        if (!classNameWasShown) {
            logger.info "Processing class $humanReadableClassName:"
            classNameWasShown = true
        }
    }

}
