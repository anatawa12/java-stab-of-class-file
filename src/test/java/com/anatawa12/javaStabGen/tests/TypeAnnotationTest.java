package com.anatawa12.javaStabGen.tests;

import java.io.OutputStream;

@SuppressWarnings("ConstantConditions")
public class TypeAnnotationTest {
    Test.@Indexing(0) Inner1.@Indexing(1) Inner2<@Indexing(2) String>
            .@Indexing(3) Inner3<@Indexing(4) OutputStream> @Indexing(5) [] @Indexing(6) [] test(
            @Indexing(7) int @Indexing(8) ... some) {
        throw null;
    }
}
