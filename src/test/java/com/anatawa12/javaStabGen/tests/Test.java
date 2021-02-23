package com.anatawa12.javaStabGen.tests;

public class Test {
    static class Inner1 {
        @SuppressWarnings("InnerClassMayBeStatic")
        class Inner2<C> {
            class Inner3<C1> {

            }
        }
    }
}
