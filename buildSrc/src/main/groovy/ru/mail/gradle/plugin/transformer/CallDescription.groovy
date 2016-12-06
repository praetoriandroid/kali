package ru.mail.gradle.plugin.transformer

class CallDescription {
    String owner
    String methodName
    String desc

    public String toString() {
        "$owner#$methodName $desc"
    }
}
