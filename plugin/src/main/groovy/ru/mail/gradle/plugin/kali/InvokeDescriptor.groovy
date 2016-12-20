package ru.mail.gradle.plugin.kali

class InvokeDescriptor {
    String owner
    String methodName
    String descriptor

    public String toString() {
        "$owner#$methodName${descriptor == null ? '(...)' : descriptor}"
    }

    boolean equals(String owner, String methodName, String descriptor) {
        return owner == this.owner && methodName == this.methodName && descriptor == this.descriptor
    }

    boolean matches(String owner, String methodName, String descriptor) {
        return owner =~ this.owner && methodName =~ this.methodName && descriptor =~ this.descriptor
    }
}
