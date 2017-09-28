package ru.mail.gradle.plugin.kali

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode

final class ClassInfo {

    final name
    final superclassName
    private final fields
    private final accessors
    private final Map<String, Integer> fieldsAccess
    private final Map<String, List<AbstractInsnNode>> fieldInitializers

    private ClassInfo(Builder builder) {
        name = builder.name
        superclassName = builder.superclassName
        fields = builder.fields.clone()
        accessors = builder.accessors.clone()
        fieldsAccess = builder.fieldsAccess.clone() as Map<String, Integer>
        fieldInitializers = builder.fieldInitializers.clone() as Map<String, List<AbstractInsnNode>>
    }

    AccessorInfo getAccessor(String methodName, String methodDesc) {
        accessors[combine(methodName, methodDesc)]
    }

    Set<AccessorInfo> getAllAccessors() {
        accessors.values() as Set<AccessorInfo>
    }

    boolean hasField(String fieldName, String fieldDesc) {
        combine(fieldName, fieldDesc) in fields
    }

    private static combine(String methodName, String methodDesc) {
        "$methodName: $methodDesc"
    }

    List<List<AbstractInsnNode>> getFieldInitializers(boolean forStaticFields) {
        fieldInitializers.findAll { fieldName, initializer ->
            def staticAccess = (fieldsAccess[fieldName] & Opcodes.ACC_STATIC) != 0
            staticAccess && forStaticFields || (!staticAccess && !forStaticFields)
        }.collect { fieldName, initializer ->
            initializer
        }
    }

    static class Builder {

        private name
        private superclassName
        private final fields = [] as Set
        private final accessors = [:]
        private final Map<String, Integer> fieldsAccess = [:]
        private final Map<String, List<AbstractInsnNode>> fieldInitializers = [:]

        Builder addAccessor(String methodName, String methodDesc, AccessorInfo info) {
            accessors[combine(methodName, methodDesc)] = info
            this
        }

        Builder setName(String className) {
            name = className
            this
        }

        Builder setSuperclass(String superclass) {
            superclassName = superclass
            this
        }

        Builder addField(String name, String desc) {
            fields << combine(name, desc)
            this
        }

        Builder addFieldAccess(String fieldName, int access) {
            fieldsAccess[fieldName] = access
            this
        }

        Builder addFieldInitializers(Map<String, List<AbstractInsnNode>> initializers) {
            fieldInitializers.putAll(initializers)
            this
        }

        List<AbstractInsnNode> getFieldInitializer(String fieldName) {
            fieldInitializers[fieldName]
        }

        ClassInfo build() {
            new ClassInfo(this)
        }

    }

}
