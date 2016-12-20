package ru.mail.gradle.plugin.kali

class Replacement {
    String owner
    String methodName
    String descriptor

    public String toString() {
        "$owner#$methodName${descriptor == null ? '(...)' : descriptor}"
    }
}
