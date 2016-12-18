package ru.mail.gradle.plugin.kali

final class ClassInfo {

    final name
    private final accessors
    private final fieldsWithAccessors

    private ClassInfo(Builder builder) {
        name = builder.name
        accessors = builder.accessors.clone()
        fieldsWithAccessors = builder.fieldsWithAcessors.clone()
    }

    AccessorInfo getAccessor(String methodName, String methodDesc) {
        accessors[combine(methodName, methodDesc)]
    }

    boolean hasAccessor(String fieldName, String fieldDesc) {
        fieldsWithAccessors.contains(combine(fieldName, fieldDesc))
    }

    private static combine(String methodName, String methodDesc) {
        "$methodName: $methodDesc"
    }

    static class Builder {

        private name
        private final accessors = [:]
        private final fieldsWithAcessors = [] as Set

        Builder addAccessor(String methodName, String methodDesc, AccessorInfo info) {
            accessors[combine(methodName, methodDesc)] = info
            fieldsWithAcessors << combine(info.field.name, info.field.desc)
            this
        }

        Builder setName(String className) {
            name = className
            this
        }

        ClassInfo build() {
            new ClassInfo(this)
        }

    }

}
