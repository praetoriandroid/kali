package ru.mail.gradle.plugin.kali

final class ClassInfo {

    final name
    final superclassName
    private final fields
    private final accessors

    private ClassInfo(Builder builder) {
        name = builder.name
        superclassName = builder.superclassName
        fields = builder.fields.clone()
        accessors = builder.accessors.clone()
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

    static class Builder {

        private name
        private superclassName
        private final fields = [] as Set
        private final accessors = [:]

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

        ClassInfo build() {
            new ClassInfo(this)
        }

    }

}
