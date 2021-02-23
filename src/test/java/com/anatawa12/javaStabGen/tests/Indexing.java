package com.anatawa12.javaStabGen.tests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
public @interface Indexing {
    int value();
}
