package com.example.mykku.contract.support

import com.example.mykku.BaseControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

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
        assertThat(endpoint.arguments.map { listOf(it.kind, it.name, it.required, it.validated, it.file) })
            .containsExactly(
                listOf(ArgumentKind.PART, "request", true, true, false),
                listOf(ArgumentKind.PART, "images", false, false, true),
                listOf(ArgumentKind.MEMBER, "member", true, false, false)
            )
        assertThat(endpoint.arguments.first().parameter.nestedParameterType.simpleName)
            .isEqualTo("CreateFeedRequestDto")
    }

    @Test
    fun `선택 인증·경로 변수·페이지 파라미터를 판정한다`() {
        val detail = catalog.find("GET", "/api/v1/feeds/{feedId}")
        val comments = catalog.find("GET", "/api/v1/feeds/{feedId}/comments")

        assertThat(detail.auth).isEqualTo(AuthLevel.OPTIONAL)
        assertThat(detail.pathArguments.map { "${it.name}:${it.kotlinType}" }).containsExactly("feedId:kotlin.Long")
        assertThat(detail.paged).isFalse()
        assertThat(comments.paged).isTrue()
        assertThat(comments.arguments.filter { it.kind == ArgumentKind.QUERY }.map { it.name to it.defaultValue })
            .containsExactly("page" to "0", "size" to "20")
        assertThat(comments.arguments.filter { it.kind == ArgumentKind.QUERY }).noneMatch { it.required }
    }

    @Test
    fun `인자 없는 공개 엔드포인트와 어드민·그 밖의 영역을 나눈다`() {
        val errorCodes = catalog.find("GET", "/api/error-codes")
        val adminCreate = catalog.find("POST", "/admin/api/v1/events")
        val adminEditPage = catalog.find("GET", "/admin/contest/{contestId}/edit")

        assertThat(errorCodes.auth).isEqualTo(AuthLevel.NONE)
        assertThat(errorCodes.arguments).isEmpty()
        assertThat(adminCreate.area).isEqualTo(EndpointArea.ADMIN)
        assertThat(adminCreate.arguments.map { it.kind to it.validated }).containsExactly(ArgumentKind.MODEL to true)
        assertThat(adminEditPage.arguments.map { it.kind }).containsExactly(ArgumentKind.PATH, ArgumentKind.FRAMEWORK)
        assertThat(catalog.inArea(EndpointArea.OTHER).map { it.pattern }).contains("/error")
    }

    @Test
    fun `같은 메서드·경로가 consumes로 갈리면 하나로 찾지 않는다`() {
        val profileUpdates = catalog.endpoints.filter { it.method == "PATCH" && it.pattern == "/api/v1/members/me" }

        assertThat(profileUpdates.map { it.consumes })
            .containsExactly(listOf("application/json"), listOf("multipart/form-data"))
        assertThatThrownBy { catalog.find("PATCH", "/api/v1/members/me") }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `엔드포인트 목록은 실행마다 같은 순서이고 조건까지 겹치는 항목이 없다`() {
        val keys = catalog.endpoints.map {
            listOf(it.method, it.pattern, it.consumes, it.produces, it.params, it.headers)
        }

        assertThat(HandlerCatalog(handlerMapping, handlerAdapter).endpoints.map { it.handlerName })
            .containsExactlyElementsOf(catalog.endpoints.map { it.handlerName })
        assertThat(keys).doesNotHaveDuplicates()
        assertThat(catalog.inArea(EndpointArea.APP)).allMatch { endpoint ->
            endpoint.arguments.count { it.kind == ArgumentKind.MEMBER } <= 1
        }
    }
}
