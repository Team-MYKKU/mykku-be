package com.example.mykku.docs

import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.snippet.Snippet

class RestDocumentationResponse {

    private val snippets = mutableListOf<Snippet>()

    fun responseBodyField(vararg descriptors: FieldDescriptor): RestDocumentationResponse {
        snippets.add(responseFields(*descriptors))
        return this
    }

    fun getSnippets(): List<Snippet> = snippets.toList()

    companion object {
        val ERROR_RESPONSE: RestDocumentationResponse
            get() = RestDocumentationResponse().responseBodyField(
                fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
            )
    }
}
