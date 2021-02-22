package com.anatawa12.javaStabGen

import com.squareup.javapoet.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import javax.lang.model.element.Modifier

// TODO: annotations
object ClassStabGenerator {
    fun generate(classNode: ClassNode): Pair<ClassName, TypeSpec>? {
        val access = classNode.innerClasses.find { it.name == classNode.name }?.access
            ?: classNode.access
        if (Modifiers.isSynthetic(access)) return null
        if (Modifiers.isModule(access)) return null
        val name = classNameFromInternalName(classNode.name, classNode) ?: return null
        val builder = when {
            Modifiers.isEnum(access) -> TypeSpec.enumBuilder(name)
            Modifiers.isAnnotation(access) -> TypeSpec.annotationBuilder(name)
            Modifiers.isInterface(access) -> TypeSpec.interfaceBuilder(name)
            else -> TypeSpec.classBuilder(name)
        }

        if (!Modifiers.isInterface(access) && Modifiers.isAbstract(access))
            builder.addModifiers(Modifier.ABSTRACT)

        builder.addModifiers(*createGeneralModifiers(access).toTypedArray())

        for (method in classNode.methods) {
            builder.addMethod(generateMethod(classNode, method) ?: continue)
        }

        for (field in classNode.fields) {
            builder.addField(generateField(classNode, field) ?: continue)
        }

        return name to builder.build()
    }

    // TODO: annotation default
    fun generateMethod(classNode: ClassNode, methodNode: MethodNode): MethodSpec? {
        if (Modifiers.isBridge(methodNode.access)) return null
        if (Modifiers.isSynthetic(methodNode.access)) return null
        if (isSyntheticVisibleMethod(classNode, methodNode)) return null
        if (methodNode.name != "<init>"
            && methodNode.name != "<clinit>"
            && !methodNode.name.isJavaIdentifierName()) return null
        val name = if (methodNode.name == "<clinit>") "<init>" else methodNode.name

        val typeAnnotations = methodNode.invisibleTypeAnnotations.orEmpty() +
                methodNode.visibleTypeAnnotations.orEmpty()

        return MethodSpec.methodBuilder(name).apply {
            // modifiers
            addModifiers(createGeneralModifiers(methodNode.access))
            if (Modifiers.isSynchronized(methodNode.access)) addModifiers(Modifier.SYNCHRONIZED)
            val varargs = Modifiers.isVarargs(methodNode.access)
            var hasBody = true
            if (Modifiers.isNative(methodNode.access)) {
                hasBody = false
                addModifiers(Modifier.NATIVE)
            }
            if (Modifiers.isAbstract(methodNode.access)) {
                hasBody = false
                addModifiers(Modifier.ABSTRACT)
            }
            if (Modifiers.isStrict(methodNode.access)) addModifiers(Modifier.STRICTFP)
            if (hasBody) addCode("throw null;")

            var returns: TypeName
            var parameters: List<TypeName>
            var throws: List<TypeName>

            // parameters
            if (methodNode.signature != null) {
                MethodSignatureVisitor(classNode).visit(methodNode.signature).also { visitor ->
                    returns = visitor.returns
                        ?: return null
                    parameters = visitor.parameters.nullIfSomeAreNull()
                        ?: return null
                    throws = visitor.throws.nullIfSomeAreNull()
                        ?: return null
                }
            } else {
                val methodType = Type.getMethodType(methodNode.desc)

                //returnsAnnotations.filter { it.typePath }
                returns = methodType.returnType.asTypeName(classNode)
                    ?: return null
                parameters = methodType.argumentTypes.map { it.asTypeName(classNode) }
                    .drop(syntheticParametersCount(classNode, methodNode))
                    .nullIfSomeAreNull()
                    ?: return null
                throws = methodNode.exceptions.map { classNameFromInternalName(it, classNode) }
                    .nullIfSomeAreNull()
                    ?: return null
            }


            val parametersSlotOffset = (if (Modifiers.isStatic(methodNode.access)) 0 else 1) +
                    syntheticParametersCount(classNode, methodNode)

            if (methodNode.name != "<init>") returns(returns)
            for ((i, param) in parameters.withIndex()) {
                val variable = methodNode.localVariables?.find { it.index == i + parametersSlotOffset }
                addParameter(ParameterSpec.builder(param, variable?.name ?: "param_$i").build())
            }
            if (varargs) varargs()
            for (throwsException in throws) {
                addException(throwsException)
            }
        }.build()
    }

    fun generateField(classNode: ClassNode, fieldNode: FieldNode): FieldSpec? {
        if (Modifiers.isSynthetic(fieldNode.access)) return null
        val type = typeNameFromDescriptor(fieldNode.desc, classNode) ?: return null
        val name = classNode.name.asJavaIdentifierOrNull() ?: return null
        return FieldSpec.builder(type, name).apply {
            addModifiers(*createGeneralModifiers(fieldNode.access).toTypedArray())
            if (Modifiers.isTransient(fieldNode.access)) addModifiers(Modifier.TRANSIENT)
            if (Modifiers.isVolatile(fieldNode.access)) addModifiers(Modifier.VOLATILE)
        }.build()
    }

    private fun syntheticParametersCount(classNode: ClassNode, methodNode: MethodNode): Int {
        if (Modifiers.isEnum(classNode.access)) {
            // name and ordinal
            if (methodNode.name == "<init>")
                return 2
        }
        return innerNestCount(classNode)
    }

    private fun innerNestCount(classNode: ClassNode): Int {
        var innerNestCount = 0
        var name = classNode.name
        while (true) {
            val innerClass = classNode.innerClasses.find { it.name == name }
                ?: return innerNestCount
            if (Modifiers.isStatic(innerClass.access)) return innerNestCount
            innerNestCount++
            name = innerClass.outerName!!
        }
    }

    private fun isSyntheticVisibleMethod(classNode: ClassNode, methodNode: MethodNode): Boolean {
        if (Modifiers.isEnum(classNode.access)) {
            if (methodNode.name == "valueOf" && methodNode.desc.startsWith("(L${"java/lang/String"};)"))
                return true
            if (methodNode.name == "values" && methodNode.desc.startsWith("()["))
                return true
        }
        return false
    }

    /**
     * general modifiers: access modifier and static/final
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun createGeneralModifiers(access: Int) = buildList {
        if (Modifiers.isPublic(access)) add(Modifier.PUBLIC)
        if (Modifiers.isPrivate(access)) add(Modifier.PRIVATE)
        if (Modifiers.isProtected(access)) add(Modifier.PROTECTED)
        if (Modifiers.isStatic(access)) add(Modifier.STATIC)
        if (Modifiers.isFinal(access)) add(Modifier.FINAL)
    }
}
