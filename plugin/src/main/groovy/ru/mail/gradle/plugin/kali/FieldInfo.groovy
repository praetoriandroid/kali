package ru.mail.gradle.plugin.kali

import groovy.transform.Immutable

@Immutable
class FieldInfo {

    String owner, name, desc

    FieldInfo forAnotherClass(String owner) {
        return new FieldInfo(owner, name, desc)
    }

    @Override
    String toString() {
        toString(owner, name, desc)
    }

    static String toString(owner, name, desc) {
        "$owner: $name: $desc"
    }

}
