package com.example.mykku.contract.schema

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.SerializationConfig

object PropertyInclusions {

    fun effective(config: SerializationConfig, owner: JavaType, property: BeanProperty): JsonInclude.Value {
        val perType = JsonInclude.Value.merge(
            config.introspect(owner).findPropertyInclusion(JsonInclude.Value.empty()),
            config.getDefaultPropertyInclusion(owner.rawClass, JsonInclude.Value.empty())
        )
        val defaults = JsonInclude.Value.merge(config.defaultPropertyInclusion, perType)
        val rawType = property.type.rawClass
        val annotated = property.member?.let { config.annotationIntrospector.findPropertyInclusion(it) }
            ?: JsonInclude.Value.empty()
        return config.getDefaultInclusion(rawType, rawType, defaults).withOverrides(annotated)
    }
}
