package ru.mail.gradle.plugin.kali

import jdk.internal.org.objectweb.asm.Opcodes
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

class BaseClassProcessor extends ClassVisitor {

    public BaseClassProcessor() {
        super(Opcodes.ASM5, new ClassWriter(ClassWriter.COMPUTE_MAXS))
    }

    byte[] toByteArray() {
        cv.toByteArray()
    }
}
