package com.example.mykku.docs

import org.springframework.restdocs.cookies.CookieDescriptor
import org.springframework.restdocs.cookies.CookieDocumentation.requestCookies
import org.springframework.restdocs.headers.HeaderDescriptor
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.restdocs.request.RequestPartDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation.requestPartFields
import org.springframework.restdocs.request.RequestDocumentation.requestParts
import org.springframework.restdocs.snippet.Snippet

class RestDocumentationRequest {

    private val snippets = mutableListOf<Snippet>()

    fun pathParameter(vararg descriptors: ParameterDescriptor): RestDocumentationRequest {
        snippets.add(pathParameters(*descriptors))
        return this
    }

    fun queryParameter(vararg descriptors: ParameterDescriptor): RestDocumentationRequest {
        snippets.add(queryParameters(*descriptors))
        return this
    }

    fun requestHeader(vararg descriptors: HeaderDescriptor): RestDocumentationRequest {
        snippets.add(requestHeaders(*descriptors))
        return this
    }

    fun requestCookie(vararg descriptors: CookieDescriptor): RestDocumentationRequest {
        snippets.add(requestCookies(*descriptors))
        return this
    }

    fun requestBodyField(vararg descriptors: FieldDescriptor): RestDocumentationRequest {
        snippets.add(requestFields(*descriptors))
        return this
    }

    fun requestPart(vararg descriptors: RequestPartDescriptor): RestDocumentationRequest {
        snippets.add(requestParts(*descriptors))
        return this
    }

    fun requestPartField(partName: String, vararg descriptors: FieldDescriptor): RestDocumentationRequest {
        snippets.add(requestPartFields(partName, *descriptors))
        return this
    }

    fun getSnippets(): List<Snippet> = snippets.toList()
}
