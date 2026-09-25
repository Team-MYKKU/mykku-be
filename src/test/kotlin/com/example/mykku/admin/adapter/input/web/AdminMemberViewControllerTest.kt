package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.member.domain.vo.SocialProvider
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AdminMemberViewController 통합 테스트")
class AdminMemberViewControllerTest : BaseControllerTest() {

    @Test
    @DisplayName("검색어 없이 들어가면 전체 회원이 보이고 사이드바 회원 메뉴가 활성화된다")
    fun `listPage - 전체 회원`() {
        createAndSaveMember(memberId = "alpha", nickname = "알파", email = "alpha@example.com", socialId = "s1")
        createAndSaveMember(memberId = "beta", nickname = "베타", email = "beta@example.com", socialId = "s2")

        getMemberPage()
            .body(containsString("alpha@example.com"))
            .body(containsString("beta@example.com"))
            .body(containsString("class=\"nav-link active\" href=\"/admin/member\""))
    }

    @Test
    @DisplayName("아이디, 닉네임, 이메일 부분 일치로 검색한다")
    fun `listPage - 부분 일치 검색`() {
        createAndSaveMember(memberId = "alpha", nickname = "알파", email = "alpha@example.com", socialId = "s1")
        createAndSaveMember(memberId = "beta", nickname = "베타", email = "beta@test.com", socialId = "s2")

        getMemberPage("keyword" to "lph").body(containsString("alpha@example.com"))
            .body(not(containsString("beta@test.com")))
        getMemberPage("keyword" to "베").body(containsString("beta@test.com"))
            .body(not(containsString("alpha@example.com")))
        getMemberPage("keyword" to "test.com").body(containsString("beta@test.com"))
            .body(not(containsString("alpha@example.com")))
    }

    @Test
    @DisplayName("검색 결과가 없으면 안내 문구를 보여 준다")
    fun `listPage - 검색 결과 없음`() {
        createAndSaveMember(memberId = "alpha", nickname = "알파", email = "alpha@example.com", socialId = "s1")

        getMemberPage("keyword" to "없는회원").body(containsString("검색 결과가 없습니다"))
    }

    @Test
    @DisplayName("검색어의 밑줄은 와일드카드가 아니라 글자 그대로 찾는다")
    fun `listPage - 밑줄 이스케이프`() {
        createAndSaveMember(memberId = "under", nickname = "밑줄", email = "x_y@example.com", socialId = "s1")
        createAndSaveMember(memberId = "plain", nickname = "평범", email = "xay@example.com", socialId = "s2")

        getMemberPage("keyword" to "x_y")
            .body(containsString("x_y@example.com"))
            .body(not(containsString("xay@example.com")))
    }

    @Test
    @DisplayName("프로필이 완성되지 않은 회원 행에만 당첨자 입력 불가 배지를 붙이고 복사 버튼을 숨긴다")
    fun `listPage - 프로필 미완료 회원`() {
        createAndSaveMember(memberId = "complete1", nickname = "완료1", email = "c1@example.com", socialId = "s1")
        createAndSaveMember(memberId = "complete2", nickname = "완료2", email = "c2@example.com", socialId = "s2")
        saveIncompleteMember(memberId = "nonick", email = "nonick@example.com", socialId = "s3")
        saveIncompleteMember(memberId = null, email = "nomid@example.com", socialId = "s4")

        val body = getMemberPage().extract().asString()

        assertThat(countOf(body, "당첨자 입력 불가")).isEqualTo(2)
        assertThat(countOf(body, "data-member-id=\"")).isEqualTo(2)
        assertThat(rowOf(body, "nonick@example.com")).contains("당첨자 입력 불가").doesNotContain("data-member-id")
        assertThat(rowOf(body, "nomid@example.com")).contains("(미설정)").contains("당첨자 입력 불가")
        assertThat(rowOf(body, "c1@example.com")).doesNotContain("당첨자 입력 불가")
    }

    @Test
    @DisplayName("memberId에만 들어 있는 검색어로도 찾는다")
    fun `listPage - memberId 검색`() {
        createAndSaveMember(memberId = "zeta99", nickname = "제타", email = "z@example.com", socialId = "s1")
        createAndSaveMember(memberId = "omega", nickname = "오메가", email = "o@example.com", socialId = "s2")

        getMemberPage("keyword" to "eta9")
            .body(containsString("z@example.com"))
            .body(not(containsString("o@example.com")))
    }

    @Test
    @DisplayName("닉네임에 들어 있는 HTML은 이스케이프해서 보여 준다")
    fun `listPage - 닉네임 이스케이프`() {
        saveIncompleteMember(memberId = "xss", email = "xss@example.com", socialId = "s1", nickname = "<b>x</b>")

        getMemberPage()
            .body(containsString("&lt;b&gt;x&lt;/b&gt;"))
            .body(not(containsString("<b>x</b>")))
    }

    @Test
    @DisplayName("소셜 제공처가 이메일을 주지 않아 만든 주소는 (제공 안 됨)으로, 애플 실제 릴레이 주소는 그대로 보여 준다")
    fun `listPage - 이메일 표시`() {
        createAndSaveMember(
            memberId = "kakao",
            email = SocialProvider.KAKAO.placeholderEmail("1001")!!,
            socialId = "1001",
            provider = SocialProvider.KAKAO
        )
        createAndSaveMember(
            memberId = "apple",
            email = "abc123xyz@privaterelay.appleid.com",
            socialId = "apple-sub",
            provider = SocialProvider.APPLE
        )

        getMemberPage()
            .body(containsString("(제공 안 됨)"))
            .body(not(containsString("kakao_1001@kakao.com")))
            .body(containsString("abc123xyz@privaterelay.appleid.com"))
            .body(containsString("카카오"))
            .body(containsString("애플"))
    }

    @Test
    @DisplayName("페이지 링크는 검색어를 유지한다")
    fun `listPage - 페이지 링크 검색어 유지`() {
        repeat(21) { index ->
            createAndSaveMember(memberId = "fan$index", email = "fan$index@example.com", socialId = "s$index")
        }

        getMemberPage("keyword" to "fan").body(containsString("page=1&amp;keyword=fan"))
    }

    @Test
    @DisplayName("세션 없이 접근하면 로그인으로 리다이렉트된다")
    fun `listPage - 미인증이면 302`() {
        RestAssured.given()
            .redirects().follow(false)
            .`when`()
            .get("/admin/member")
            .then()
            .statusCode(302)
            .header("Location", endsWith("/admin/login"))
    }

    private fun saveIncompleteMember(memberId: String?, email: String, socialId: String, nickname: String? = null) {
        memberJpaRepository.save(
            MemberJpaEntity(
                memberId = memberId,
                nickname = nickname,
                email = email,
                socialId = socialId,
                provider = SocialProvider.GOOGLE,
                profileImage = ""
            )
        )
    }

    private fun countOf(body: String, token: String): Int {
        return body.windowed(token.length).count { it == token }
    }

    private fun rowOf(body: String, email: String): String {
        val emailIndex = body.indexOf(email)
        return body.substring(body.lastIndexOf("<tr", emailIndex), body.indexOf("</tr>", emailIndex))
    }

    private fun getMemberPage(vararg params: Pair<String, String>): ValidatableResponse {
        val request = RestAssured.given().sessionId(getAdminSessionId())
        params.forEach { (name, value) -> request.queryParam(name, value) }
        return request
            .`when`()
            .get("/admin/member")
            .then()
            .statusCode(200)
    }
}
