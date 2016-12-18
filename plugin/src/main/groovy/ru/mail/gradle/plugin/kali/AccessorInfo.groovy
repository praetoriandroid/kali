package ru.mail.gradle.plugin.kali

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.InsnList

class AccessorInfo {

    private final InsnList inline
    final FieldInfo field

    AccessorInfo(InsnList inline, FieldInfo field) {
        this.inline = inline
        this.field = field
    }

    void accept(MethodVisitor mv) {
        inline.accept(mv)
    }

}
