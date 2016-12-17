package ru.mail.gradle.plugin.kali

class CallDescription {
    String owner
    String methodName
    String desc

    public String toString() {
        "$owner#$methodName $desc"
    }
}
