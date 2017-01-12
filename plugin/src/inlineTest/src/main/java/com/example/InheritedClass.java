package com.example;

import com.example.another_package.BaseClass;

public class InheritedClass extends BaseClass {
    private boolean untouched;
    private boolean outerClassField;

    static class InnerClass {
        void accessOuterBaseClassField(InheritedClass inherited) {
            inherited.baseClassField = true;
        }

        void accessOuterClassField(InheritedClass inherited) {
            inherited.outerClassField = true;
        }
    }
}
