package com.example.mykku.docs

import org.springframework.restdocs.headers.HeaderDescriptor
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.restdocs.request.RequestPartDescriptor

data class ApiRequestConfig(
    val pathParameters: List<ParameterDescriptor> = emptyList(),
    val queryParameters: List<ParameterDescriptor> = emptyList(),
    val requestBodyFields: List<FieldDescriptor> = emptyList(),
    val requestParts: List<RequestPartDescriptor> = emptyList(),
    val requestPartFields: Map<String, List<FieldDescriptor>> = emptyMap(),
    val headerDescriptors: List<HeaderDescriptor> = emptyList()
)
