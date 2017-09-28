package com.example;

public class OuterClass {
    private String field = "non-static";
    private static String staticField = "static";

    public static class InnerClass {
        private String field = "inner non-static";
        private static String staticField = "inner static";
        private int multiInited;

        InnerClass() {
            multiInited = 42;
        }

        InnerClass(int unused) {
            multiInited = 42;
        }
    }
}