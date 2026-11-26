package com.example.mykku.contract.schema

import com.example.mykku.contract.dump.KotlinTypes
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.kotlinFunction

object KotlinProperties {

    fun ownerOf(declared: KType?, type: JavaType): KType? {
        if (declared != null) {
            return declared
        }
        val klass = type.rawClass.kotlin
        return if (KotlinTypes.isKotlin(klass) && klass.typeParameters.isEmpty()) klass.createType() else null
    }

    fun typeOf(owner: KType?, member: AnnotatedMember?): KType? {
        val klass = owner?.classifier as? KClass<*> ?: return null
        if (!KotlinTypes.isKotlin(klass)) {
            return null
        }
        val declared = declaredType(klass, member?.member) ?: return null
        return KotlinTypes.substitute(declared, KotlinTypes.bindingsOf(owner))
    }

    private fun declaredType(klass: KClass<*>, javaMember: Any?): KType? {
        if (javaMember == null) {
            return null
        }
        val property = klass.memberProperties.firstOrNull { it.javaGetter == javaMember || it.javaField == javaMember }
        return property?.returnType ?: (javaMember as? Method)?.kotlinFunction?.returnType
    }
}
