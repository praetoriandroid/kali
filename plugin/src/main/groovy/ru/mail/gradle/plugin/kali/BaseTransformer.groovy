package ru.mail.gradle.plugin.kali

import jdk.internal.org.objectweb.asm.Opcodes
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

class BaseTransformer extends ClassVisitor {

    public BaseTransformer() {
        super(Opcodes.ASM5, new ClassWriter(ClassWriter.COMPUTE_MAXS))
    }

    byte[] toByteArray() {
        cv.toByteArray()
    }
}
