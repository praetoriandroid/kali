package ru.mail.gradle.plugin.kali

import org.objectweb.asm.Opcodes

class VisitorUtils {

    private static final Map<String, Integer> FIELD_MODIFIERS = [
            public: Opcodes.ACC_PUBLIC,
            private: Opcodes.ACC_PRIVATE,
            protected: Opcodes.ACC_PROTECTED,
            static: Opcodes.ACC_STATIC,
            final: Opcodes.ACC_FINAL,
            volatile: Opcodes.ACC_VOLATILE,
            transient: Opcodes.ACC_TRANSIENT
    ]

    private VisitorUtils() {
        throw new UnsupportedOperationException("No instances!")
    }

    static int decodeModifiers(int originalAccess, String modifiers) {
        int code = originalAccess & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE |
                Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL |
                Opcodes.ACC_VOLATILE | Opcodes.ACC_TRANSIENT)
        for (String modifier : modifiers.trim().split('\\s+')) {
            int modifierCode = FIELD_MODIFIERS[modifier]
            if (modifier && !modifierCode) {
                throw new IllegalArgumentException("Bad modifier: $modifier")
            }
            code |= modifierCode
        }
        return code
    }

    static boolean flagsChanged(int oldValue, int newValue, int flags) {
        return (oldValue & flags) != (newValue & flags)
    }

    static String fieldOwner(String fieldId) {
        def index = fieldId.lastIndexOf('.')
        if (index == -1) {
            throw new IllegalArgumentException("Bad field identifier: $fieldId")
        }
        return fieldId[0..index - 1]
    }

    static String fieldName(String fieldId) {
        return fieldId[fieldId.lastIndexOf('.') + 1..-1]
    }

    static String humanReadableClassName(String className) {
        className.replace('/', '.')
    }

    static String fieldId(String owner, String fieldName) {
        "${humanReadableClassName(owner)}.$fieldName" as String
    }

}
