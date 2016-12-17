package ru.mail.gradle.plugin.kali

import org.objectweb.asm.FieldVisitor

import static org.objectweb.asm.Opcodes.ACC_PRIVATE
import static org.objectweb.asm.Opcodes.ACC_PROTECTED
import static org.objectweb.asm.Opcodes.ACC_PUBLIC

public class FieldExposer extends BaseClassProcessor {
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        access &= ~(ACC_PRIVATE | ACC_PROTECTED)
        access |= ACC_PUBLIC
        return super.visitField(access, name, desc, signature, value)
    }
}
