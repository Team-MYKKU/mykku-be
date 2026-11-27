package com.example.mykku.role.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.role.adapter.output.persistence.entity.MemberRoleJpaEntity
import com.example.mykku.role.adapter.output.persistence.entity.RoleJpaEntity
import com.example.mykku.role.adapter.output.persistence.repository.MemberRoleJpaRepository
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("RoleController 통합 테스트")
class RoleControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var memberRoleJpaRepository: MemberRoleJpaRepository

    private lateinit var role1: RoleJpaEntity
    private lateinit var role2: RoleJpaEntity

    @BeforeEach
    fun setUp() {
        role1 = roleJpaRepository.save(RoleJpaEntity(name = "칭호1", description = "설명1"))
        role2 = roleJpaRepository.save(RoleJpaEntity(name = "칭호2", description = "설명2"))
    }

    @Test
    @DisplayName("내 칭호 목록을 조회할 수 있다")
    fun `getMyRoles - 내 칭호 목록을 조회한다`() {
        // given
        val member = createAndSaveMember(
            nickname = "테스터",
            role = role1
        )
        memberRoleJpaRepository.save(MemberRoleJpaEntity(memberId = member.id, role = role1))
        memberRoleJpaRepository.save(MemberRoleJpaEntity(memberId = member.id, role = role2))

        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .headers(createAuthHeaders(member.id))
            .`when`()
            .get("/api/v1/roles/me")
            .then()
            .statusCode(200)
            .body("message", equalTo("내 칭호 목록 조회 성공"))
            .body("data", hasSize<Any>(2))
            .body("data[0].role.name", notNullValue())
            .body("data[0].isRepresentative", notNullValue())
    }

    @Test
    @DisplayName("새로 획득한 칭호를 조회할 수 있다")
    fun `getNewRoles - 새로 획득한 칭호를 조회한다`() {
        // given
        val member = createAndSaveMember(nickname = "테스터", role = role1)
        memberRoleJpaRepository.save(MemberRoleJpaEntity(memberId = member.id, role = role1))

        // when & then
        requestNewRoles(member.id)
            .body("message", equalTo("새로 획득한 칭호 조회 성공"))
            .body("data", hasSize<Any>(1))
            .body("data[0].role.name", equalTo("칭호1"))
            .body("data[0].isRepresentative", equalTo(true))
    }

    @Test
    @DisplayName("한 번 조회한 칭호는 다시 조회되지 않는다")
    fun `getNewRoles - 재호출하면 빈 배열을 반환한다`() {
        // given
        val member = createAndSaveMember(nickname = "테스터", role = role1)
        memberRoleJpaRepository.save(MemberRoleJpaEntity(memberId = member.id, role = role1))

        requestNewRoles(member.id)
            .body("data", hasSize<Any>(1))

        // when & then
        requestNewRoles(member.id)
            .body("data", hasSize<Any>(0))
    }

    @Test
    @DisplayName("이미 확인 처리된 칭호는 신규 목록에 나오지 않는다")
    fun `getNewRoles - 확인된 칭호는 제외된다`() {
        // given
        val member = createAndSaveMember(nickname = "테스터", role = role1)
        memberRoleJpaRepository.save(
            MemberRoleJpaEntity(memberId = member.id, role = role1, checkedAt = LocalDateTime.now())
        )
        memberRoleJpaRepository.save(MemberRoleJpaEntity(memberId = member.id, role = role2))

        // when & then
        requestNewRoles(member.id)
            .body("data", hasSize<Any>(1))
            .body("data[0].role.name", equalTo("칭호2"))
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 새로 획득한 칭호를 조회할 수 없다")
    fun `getNewRoles - 인증되지 않은 사용자는 401을 반환한다`() {
        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/api/v1/roles/me/new")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("전체 칭호 목록을 조회할 수 있다")
    fun `getRoles - 전체 칭호 목록을 조회한다`() {
        // given
        val member = createAndSaveMember(nickname = "테스터")

        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .headers(createAuthHeaders(member.id))
            .`when`()
            .get("/api/v1/roles")
            .then()
            .statusCode(200)
            .body("message", equalTo("칭호 목록 조회 성공"))
            .body("data.name", hasItems("칭호1", "칭호2"))
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 전체 칭호 목록을 조회할 수 없다")
    fun `getRoles - 인증되지 않은 사용자는 401을 반환한다`() {
        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/api/v1/roles")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("대표 칭호를 변경할 수 있다")
    fun `changeRepresentativeRole - 대표 칭호를 변경한다`() {
        // given
        val member = createAndSaveMember(
            nickname = "테스터",
            role = role1
        )
        val memberRole1 = memberRoleJpaRepository.save(MemberRoleJpaEntity(memberId = member.id, role = role1))
        val memberRole2 = memberRoleJpaRepository.save(MemberRoleJpaEntity(memberId = member.id, role = role2))

        // when & then
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .headers(createAuthHeaders(member.id))
            .`when`()
            .patch("/api/v1/roles/${memberRole2.id}/representative")
            .then()
            .statusCode(200)
            .body("message", equalTo("대표 칭호 변경 성공"))
    }
}
