package ru.mail.gradle.plugin.transformer

class Replacement {
    String owner
    String methodName

    public String toString() {
        "$owner#$methodName"
    }
}
