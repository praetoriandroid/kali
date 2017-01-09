package ru.mail.gradle.plugin.kali

final class PreparedInfo {

    private final classes
    private final accessedFields

    private PreparedInfo(Builder builder) {
        classes = builder.classes.clone()
        accessedFields = [:]
        classes.values().each { ClassInfo classInfo ->
            classInfo.allAccessors.each {
                def field = resolveField(it.field)
                if (field) {
                    accessedFields[field.toString()] = field
                }
            }
        }
    }

    private FieldInfo resolveField(FieldInfo field) {
        def clazz = getClass([field.owner])
        while (!clazz.hasField(field.name, field.desc)) {
            clazz = getClass(clazz.superclassName)
            if (!clazz) {
                return null
            }
            field = field.forAnotherClass(clazz.name)
        }
        return field
    }

    ClassInfo getClass(String className) {
        classes[className]
    }

    AccessorInfo getAccessor(String className, String methodName, String methodDesc) {
        getClass(className)?.getAccessor(methodName, methodDesc)
    }

    boolean hasAccessor(String className, String fieldName, String fieldDesc) {
        accessedFields[FieldInfo.toString(className, fieldName, fieldDesc)]
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
