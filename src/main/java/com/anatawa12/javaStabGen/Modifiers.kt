package com.anatawa12.javaStabGen

import org.objectweb.asm.Opcodes
import java.lang.reflect.Modifier

object Modifiers {
    fun isPublic(access: Int): Boolean = Modifier.isPublic(access)
    fun isPrivate(access: Int): Boolean = Modifier.isPrivate(access)
    fun isProtected(access: Int): Boolean = Modifier.isProtected(access)
    fun isStatic(access: Int): Boolean = Modifier.isStatic(access)
    fun isFinal(access: Int): Boolean = Modifier.isFinal(access)
    fun isSynchronized(access: Int): Boolean = Modifier.isSynchronized(access)
    fun isVolatile(access: Int): Boolean = Modifier.isVolatile(access)
    fun isTransient(access: Int): Boolean = Modifier.isTransient(access)
    fun isNative(access: Int): Boolean = Modifier.isNative(access)
    fun isInterface(access: Int): Boolean = Modifier.isInterface(access)
    fun isAbstract(access: Int): Boolean = Modifier.isAbstract(access)
    fun isStrict(access: Int): Boolean = Modifier.isStrict(access)

    fun isBridge(access: Int): Boolean = access.and(Opcodes.ACC_BRIDGE) != 0
    fun isSynthetic(access: Int): Boolean = access.and(Opcodes.ACC_SYNTHETIC) != 0
    fun isVarargs(access: Int): Boolean = access.and(Opcodes.ACC_VARARGS) != 0
    fun isEnum(access: Int): Boolean = access.and(Opcodes.ACC_ENUM) != 0
    fun isAnnotation(access: Int): Boolean = access.and(Opcodes.ACC_ANNOTATION) != 0
    fun isModule(access: Int): Boolean = access.and(Opcodes.ACC_MODULE) != 0
}
