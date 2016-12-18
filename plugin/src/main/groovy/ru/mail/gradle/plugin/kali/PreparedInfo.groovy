package ru.mail.gradle.plugin.kali

final class PreparedInfo {

    private final classes

    private PreparedInfo(Builder builder) {
        classes = builder.classes.clone()
    }

    ClassInfo getClass(String className) {
        classes[className]
    }

    AccessorInfo getAccessor(String className, String methodName, String methodDesc) {
        getClass(className)?.getAccessor(methodName, methodDesc)
    }

    boolean hasAccessor(String className, String fieldName, String fieldDesc) {
        getClass(className)?.hasAccessor(fieldName, fieldDesc)
    }

    static class Builder {

        final classes = [:]

        Builder addClass(ClassInfo classInfo) {
            classes[classInfo.name] = classInfo
            this
        }

        PreparedInfo build() {
            return new PreparedInfo(this)
        }

    }

}
