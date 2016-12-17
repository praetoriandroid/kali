package ru.mail.gradle.plugin.kali

class Replacement {
    String owner
    String methodName

    public String toString() {
        "$owner#$methodName"
    }
}
