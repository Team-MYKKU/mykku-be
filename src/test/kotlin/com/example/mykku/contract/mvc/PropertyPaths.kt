package com.example.mykku.contract.mvc

import com.example.mykku.contract.dump.AttributeValues
import org.springframework.beans.BeanWrapperImpl

object PropertyPaths {

    fun render(prefix: String, target: Any, paths: List<String>): List<String> {
        val wrapper = BeanWrapperImpl(target)
        return paths.map { "$prefix.$it=${AttributeValues.value(wrapper.getPropertyValue(camelCase(it)))}" }
    }

    private fun camelCase(path: String): String = path.split('.').joinToString(".") { camelSegment(it) }

    private fun camelSegment(segment: String): String = segment.split('-')
        .mapIndexed { index, word -> if (index == 0) word else word.replaceFirstChar(Char::uppercase) }
        .joinToString("")
}
