package com.example.mykku.docs

import org.springframework.http.HttpHeaders
import org.springframework.restdocs.operation.preprocess.OperationRequestPreprocessor
import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.restdocs.restassured.RestAssuredRestDocumentation.document
import org.springframework.restdocs.restassured.RestDocumentationFilter
import org.springframework.restdocs.snippet.Snippet

class RestDocumentationFilterBuilder(
    identifierPrefix: String,
    identifier: String
) {
    private val identifier: String = "$identifierPrefix/$identifier"
    private val snippets = mutableListOf<Snippet>()

    fun request(request: RestDocumentationRequest): RestDocumentationFilterBuilder {
        snippets.addAll(request.getSnippets())
        return this
    }

    fun response(response: RestDocumentationResponse): RestDocumentationFilterBuilder {
        snippets.addAll(response.getSnippets())
        return this
    }

    fun build(): RestDocumentationFilter {
        return document(
            identifier,
            REQUEST_PREPROCESSOR,
            RESPONSE_PREPROCESSOR,
            *snippets.toTypedArray()
        )
    }

    companion object {
        private val REQUEST_PREPROCESSOR: OperationRequestPreprocessor = Preprocessors.preprocessRequest(
            Preprocessors.prettyPrint(),
            Preprocessors.modifyHeaders()
                .remove(HttpHeaders.HOST)
                .remove(HttpHeaders.CONTENT_LENGTH)
        )

        private val RESPONSE_PREPROCESSOR: OperationResponsePreprocessor = Preprocessors.preprocessResponse(
            Preprocessors.prettyPrint(),
            Preprocessors.modifyHeaders()
                .remove(HttpHeaders.TRANSFER_ENCODING)
                .remove(HttpHeaders.DATE)
                .remove(HttpHeaders.CONNECTION)
                .remove(HttpHeaders.CONTENT_LENGTH)
                .remove("X-Content-Type-Options")
                .remove("X-XSS-Protection")
                .remove("Cache-Control")
                .remove("Pragma")
                .remove("Expires")
                .remove("X-Frame-Options")
                .remove("X-Request-ID")
                .remove("Keep-Alive")
        )
    }
}
