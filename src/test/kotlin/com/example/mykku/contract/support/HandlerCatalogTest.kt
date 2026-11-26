package com.example.mykku.contract.support

import com.example.mykku.BaseControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.Random

@DisplayName("핸들러 분석")
class HandlerCatalogTest : BaseControllerTest() {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    private val catalog: HandlerCatalog by lazy { HandlerCatalog(handlerMapping, handlerAdapter) }

    @Test
    fun `multipart 피드 작성은 인증 필수이고 인자를 선언 순서대로 판정한다`() {
        val endpoint = catalog.find("POST", "/api/v1/feeds")

        assertThat(endpoint.area).isEqualTo(EndpointArea.APP)
        assertThat(endpoint.consumes).containsExactly("multipart/form-data")
        assertThat(endpoint.auth).isEqualTo(AuthLevel.REQUIRED)
        assertThat(summaries(endpoint)).containsExactly(
            listOf(ArgumentKind.PART, "request", true, true, false),
            listOf(ArgumentKind.PART, "images", false, false, true),
            listOf(ArgumentKind.MEMBER, "member", true, false, false)
        )
        assertThat(endpoint.arguments.first().parameter.nestedParameterType.simpleName)
            .isEqualTo("CreateFeedRequestDto")
    }

    @Test
    fun `@Valid 없는 JSON 본문과 본문을 회원보다 먼저 받는 순서를 판정한다`() {
        val endpoint = catalog.find("POST", "/api/v1/feeds/{feedId}/comments")

        assertThat(summaries(endpoint)).containsExactly(
            listOf(ArgumentKind.PATH, "feedId", true, false, false),
            listOf(ArgumentKind.BODY, "request", true, false, false),
            listOf(ArgumentKind.MEMBER, "member", true, false, false)
        )
    }

    @Test
    fun `선택 인증·경로 변수·페이지 파라미터를 판정한다`() {
        val detail = catalog.find("GET", "/api/v1/feeds/{feedId}")
        val comments = catalog.find("GET", "/api/v1/feeds/{feedId}/comments")

        assertThat(detail.auth).isEqualTo(AuthLevel.OPTIONAL)
        assertThat(detail.pathArguments.map { "${it.name}:${it.kotlinType}" }).containsExactly("feedId:kotlin.Long")
        assertThat(detail.paged).isFalse()
        assertThat(comments.paged).isTrue()
        assertThat(queries(comments).map { it.name to it.defaultValue }).containsExactly("page" to "0", "size" to "20")
        assertThat(queries(comments)).noneMatch { it.required }
    }

    @Test
    fun `기본값 없는 필수 쿼리와 기본값 있는 enum 쿼리를 나눈다`() {
        val keyword = queries(catalog.find("GET", "/api/v1/search/feeds")).single()
        val status = queries(catalog.find("GET", "/api/v1/contests")).first()

        assertThat(listOf(keyword.name, keyword.required, keyword.defaultValue)).containsExactly("keyword", true, null)
        assertThat(listOf(status.name, status.required, status.defaultValue)).containsExactly("status", false, "ACTIVE")
        assertThat(status.kotlinType.toString()).endsWith("ContestStatusFilter")
    }

    @Test
    fun `같은 메서드·경로가 consumes로 갈리고 nullable 파트는 선택으로 판정한다`() {
        val profileUpdates = catalog.endpoints.filter { it.method == "PATCH" && it.pattern == "/api/v1/members/me" }
        val multipart = profileUpdates.single { it.consumes == listOf("multipart/form-data") }

        assertThat(profileUpdates.map { it.consumes })
            .containsExactly(listOf("application/json"), listOf("multipart/form-data"))
        assertThatThrownBy { catalog.find("PATCH", "/api/v1/members/me") }
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(summaries(multipart).drop(1)).containsExactly(
            listOf(ArgumentKind.PART, "request", false, true, false),
            listOf(ArgumentKind.PART, "profileImage", false, false, true)
        )
    }

    @Test
    fun `어드민 폼·Pageable·화면 인자와 그 밖의 영역을 나눈다`() {
        val adminCreate = catalog.find("POST", "/admin/api/v1/events")
        val adminFanNotes = catalog.find("GET", "/admin/fannote")

        assertThat(adminCreate.area).isEqualTo(EndpointArea.ADMIN)
        assertThat(adminCreate.arguments.map { it.kind to it.validated }).containsExactly(ArgumentKind.MODEL to true)
        assertThat(adminFanNotes.arguments.map { it.kind })
            .containsExactly(ArgumentKind.PAGEABLE, ArgumentKind.FRAMEWORK)
        assertThat(adminFanNotes.paged).isTrue()
        assertThat(catalog.find("GET", "/api/error-codes").arguments).isEmpty()
        assertThat(catalog.inArea(EndpointArea.OTHER).map { it.method to it.pattern }).containsOnly("*" to "/error")
    }

    @Test
    fun `엔드포인트 정렬 키는 전순서라 입력 순서와 무관하게 같은 목록이 나온다`() {
        val keys = catalog.endpoints.map {
            listOf(it.method, it.pattern, it.consumes, it.produces, it.params, it.headers)
        }
        val resorted = HandlerCatalog.sorted(catalog.endpoints.reversed().shuffled(Random(SEED)))

        assertThat(keys).doesNotHaveDuplicates()
        assertThat(resorted.map { it.handlerName }).containsExactlyElementsOf(catalog.endpoints.map { it.handlerName })
        assertThat(catalog.inArea(EndpointArea.APP)).allMatch { endpoint ->
            endpoint.arguments.count { it.kind == ArgumentKind.MEMBER } <= 1
        }
    }

    @Test
    fun `헤더·쿠키·쿼리 맵·Sort·Pageable 인자를 실제 리졸버 기준으로 판정한다`() {
        val probe = ArgumentProbeController::class.java.methods.single { it.name == "probe" }
        val arguments = HandlerArgumentReader(handlerAdapter.argumentResolvers.orEmpty())
            .read(HandlerMethod(ArgumentProbeController(), probe))

        assertThat(arguments.map { listOf(it.kind, it.name, it.required, it.defaultValue) }).containsExactly(
            listOf(ArgumentKind.HEADER, "X-Probe", true, null),
            listOf(ArgumentKind.HEADER, "headers", false, null),
            listOf(ArgumentKind.COOKIE, "probe", false, "none"),
            listOf(ArgumentKind.QUERY, "params", false, null),
            listOf(ArgumentKind.SORT, "sort", false, null),
            listOf(ArgumentKind.PAGEABLE, "pageable", false, null)
        )
    }

    private fun summaries(endpoint: Endpoint): List<List<Any>> =
        endpoint.arguments.map { listOf(it.kind, it.name, it.required, it.validated, it.file) }

    private fun queries(endpoint: Endpoint): List<HandlerArgument> =
        endpoint.arguments.filter { it.kind == ArgumentKind.QUERY }

    private companion object {
        const val SEED = 20261125L
    }
}

class ArgumentProbeController {
    @Suppress("UNUSED_PARAMETER")
    fun probe(
        @RequestHeader("X-Probe") probeHeader: String,
        @RequestHeader headers: Map<String, String>,
        @CookieValue("probe", defaultValue = "none") cookie: String,
        @RequestParam params: Map<String, String>,
        sort: Sort,
        pageable: Pageable
    ) = Unit
}
