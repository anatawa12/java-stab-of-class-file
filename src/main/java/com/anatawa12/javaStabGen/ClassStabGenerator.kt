package com.anatawa12.javaStabGen

import com.squareup.javapoet.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import javax.lang.model.element.Modifier

// TODO: annotations
object ClassStabGenerator {
    fun generate(byteArray: ByteArray): Pair<ClassName, TypeSpec>? {
        val reader = ClassReader(byteArray)
        val node = ClassNode()
        reader.accept(node, 0)
        return generate(node)
    }

    fun generate(classNode: ClassNode): Pair<ClassName, TypeSpec>? {
        if (classNode.name == "java/lang/Object") return null // no support for java.lang.Object
        val access = classNode.innerClasses.find { it.name == classNode.name }?.access
            ?: classNode.access
        if (Modifiers.isSynthetic(access)) return null
        if (Modifiers.isModule(access)) return null
        val name = classNameFromInternalName(classNode.name, classNode) ?: return null
        val kind = when {
            Modifiers.isEnum(access) -> TypeSpec.Kind.ENUM
            Modifiers.isAnnotation(access) -> TypeSpec.Kind.ANNOTATION
            Modifiers.isInterface(access) -> TypeSpec.Kind.INTERFACE
            else -> TypeSpec.Kind.CLASS
        }
        val builder = when (kind) {
            TypeSpec.Kind.CLASS -> TypeSpec.classBuilder(name)
            TypeSpec.Kind.INTERFACE -> TypeSpec.interfaceBuilder(name)
            TypeSpec.Kind.ENUM -> TypeSpec.enumBuilder(name)
            TypeSpec.Kind.ANNOTATION -> TypeSpec.annotationBuilder(name)
        }

        val signature = classNode.signature ?: buildSignature(classNode)
        val visitor = ClassSignatureVisitor(classNode,
            classNode.invisibleTypeAnnotations.orEmpty() + classNode.visibleTypeAnnotations.orEmpty())
            .visit(signature)
        val typeParameters = visitor.typeParameters.nullIfSomeAreNull()
        val superClass = visitor.superClass ?: return null
        val superInterfaces = visitor.superInterfaces.nullIfSomeAreNull() ?: return null

        builder.addTypeVariables(typeParameters)

        if (kind == TypeSpec.Kind.CLASS) builder.superclass(superClass)
        builder.addSuperinterfaces(superInterfaces)

        if (!Modifiers.isInterface(access) && Modifiers.isAbstract(access))
            builder.addModifiers(Modifier.ABSTRACT)

        builder.addModifiers(*createGeneralModifiers(access).toTypedArray())

        for (method in classNode.methods) {
            builder.addMethod(generateMethod(classNode, method) ?: continue)
        }

        for (field in classNode.fields) {
            builder.addField(generateField(classNode, field) ?: continue)
        }
        if (Modifiers.isEnum(classNode.access)) {
            if (!builder.methodSpecs.any { it.name == "<ctor>" && it.parameters.isEmpty() })
                builder.addMethod(MethodSpec.constructorBuilder().addCode("throw null;").build())
            val abstracts = builder.methodSpecs.filter { it.modifiers.contains(Modifier.ABSTRACT) }

            for (field in classNode.fields) {
                if (Modifiers.isEnum(field.access))
                    builder.addEnumConstant(field.name, TypeSpec.anonymousClassBuilder("")
                        .apply {
                            for (abstractMethod in abstracts) {
                                addMethod(MethodSpec.methodBuilder(abstractMethod.name)
                                    .apply {
                                        addModifiers(abstractMethod.modifiers.toMutableSet()
                                            .also { it.remove(Modifier.ABSTRACT) })
                                        for (parameter in abstractMethod.parameters)
                                            addParameter(parameter)
                                        returns(abstractMethod.returnType)
                                        addCode("throw null;")
                                    }.build())
                            }
                        }.build())
            }
        }

        builder.addAnnotations(classNode.invisibleAnnotations?.toSpecs(classNode).orEmpty())
        builder.addAnnotations(classNode.visibleAnnotations?.toSpecs(classNode).orEmpty())

        return name to builder.build()
    }

    private fun buildSignature(classNode: ClassNode): String {
        return buildString {
            append('L').append(classNode.superName).append(';')
            for (interfaceName in classNode.interfaces) {
                append('L').append(interfaceName).append(';')
            }
        }
    }

    // TODO: annotation default
    private fun generateMethod(classNode: ClassNode, methodNode: MethodNode): MethodSpec? {
        if (Modifiers.isBridge(methodNode.access)) return null
        if (Modifiers.isSynthetic(methodNode.access)) return null
        if (isSyntheticVisibleMethod(classNode, methodNode)) return null
        if (methodNode.name != "<init>"
            && methodNode.name != "<clinit>"
            && !methodNode.name.isJavaIdentifierName()
        ) return null
        if (methodNode.name == "<clinit>") return null
        val name = methodNode.name

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

            var typeParameters: List<TypeVariableName>
            var returns: TypeName
            var parameters: List<TypeName>
            var throws: List<TypeName>

            // parameters
            val signature = methodNode.signature ?: buildSignature(classNode, methodNode)
            MethodSignatureVisitor(classNode, typeAnnotations).visit(signature).also { visitor ->
                typeParameters = visitor.typeParameters.nullIfSomeAreNull()
                    ?: return null
                returns = visitor.returns
                    ?: return null
                parameters = visitor.parameters.nullIfSomeAreNull()
                    ?: return null
                throws = visitor.throws.nullIfSomeAreNull()
                    ?: return null
            }

            addTypeVariables(typeParameters)

            val parametersSlotOffset = (if (Modifiers.isStatic(methodNode.access)) 0 else 1) +
                    syntheticParametersCount(classNode, methodNode)

            if (methodNode.name != "<init>") returns(returns)
            for ((i, param) in parameters.withIndex()) {
                val variable = methodNode.localVariables?.find { it.index == i + parametersSlotOffset }
                addParameter(ParameterSpec
                    .builder(param, variable?.name?.asJavaIdentifierOrNull() ?: "param_$i")
                    .build())
            }
            if (varargs) varargs()
            for (throwsException in throws) {
                addException(throwsException)
            }

            methodNode.annotationDefault?.let { default ->
                defaultValue(default.asAnnotationCode(classNode))
            }

            addAnnotations(methodNode.invisibleAnnotations?.toSpecs(classNode).orEmpty())
            addAnnotations(methodNode.visibleAnnotations?.toSpecs(classNode).orEmpty())
        }.build()
    }

    private fun buildSignature(classNode: ClassNode, methodNode: MethodNode): String {
        val methodType = Type.getMethodType(methodNode.desc)

        return buildString {
            append('(')
            for (type in methodType.argumentTypes.asSequence().drop(syntheticParametersCount(classNode, methodNode))) {
                append(type)
            }
            append(')')
            append(methodType.returnType)
            for (exception in methodNode.exceptions) {
                append('^')
                append(exception)
            }
        }
    }

    private fun generateField(classNode: ClassNode, fieldNode: FieldNode): FieldSpec? {
        if (Modifiers.isSynthetic(fieldNode.access)) return null
        if (Modifiers.isEnum(fieldNode.access)) return null

        val typeAnnotations = fieldNode.invisibleTypeAnnotations.orEmpty() +
                fieldNode.visibleTypeAnnotations.orEmpty()

        val signature = fieldNode.signature ?: fieldNode.desc
        val type = FieldSignatureVisitor(classNode, typeAnnotations).visit(signature).type ?: return null
        val name = fieldNode.name.asJavaIdentifierOrNull() ?: return null
        return FieldSpec.builder(type, name).apply {
            addModifiers(*createGeneralModifiers(fieldNode.access).toTypedArray())
            if (Modifiers.isTransient(fieldNode.access)) addModifiers(Modifier.TRANSIENT)
            if (Modifiers.isVolatile(fieldNode.access)) addModifiers(Modifier.VOLATILE)

            addAnnotations(fieldNode.invisibleAnnotations?.toSpecs(classNode).orEmpty())
            addAnnotations(fieldNode.visibleAnnotations?.toSpecs(classNode).orEmpty())
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
    private fun createGeneralModifiers(access: Int) = mutableListOf<Modifier>().apply {
        if (Modifiers.isPublic(access)) add(Modifier.PUBLIC)
        if (Modifiers.isPrivate(access)) add(Modifier.PRIVATE)
        if (Modifiers.isProtected(access)) add(Modifier.PROTECTED)
        if (Modifiers.isStatic(access)) add(Modifier.STATIC)
        if (Modifiers.isFinal(access)) add(Modifier.FINAL)
    }
}
