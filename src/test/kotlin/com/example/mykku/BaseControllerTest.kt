package com.example.mykku

import com.example.mykku.board.adapter.output.persistence.BoardJpaRepository
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.config.TestAsyncConfig
import com.example.mykku.config.TestEmailConfig
import com.example.mykku.config.TestEmailSenderConfig
import com.example.mykku.config.TestImageUploadConfig
import com.example.mykku.contract.support.ContractManifestExtension
import com.example.mykku.member.adapter.output.persistence.MemberJpaRepository
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.member.domain.vo.SocialProvider
import com.example.mykku.role.adapter.output.persistence.entity.RoleJpaEntity
import com.example.mykku.role.adapter.output.persistence.repository.RoleJpaRepository
import com.example.mykku.util.DatabaseCleaner
import com.example.mykku.util.TestTokenGenerator
import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestEmailConfig::class, TestEmailSenderConfig::class, TestImageUploadConfig::class, TestAsyncConfig::class)
@ExtendWith(DatabaseCleaner::class, ContractManifestExtension::class)
abstract class BaseControllerTest {

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    protected lateinit var memberJpaRepository: MemberJpaRepository

    @Autowired
    protected lateinit var boardJpaRepository: BoardJpaRepository

    @Autowired
    protected lateinit var roleJpaRepository: RoleJpaRepository

    @BeforeEach
    fun baseSetUp() {
        RestAssured.port = port
    }

    protected fun createAndSaveMember(
        memberId: String? = "testmember",
        nickname: String = "테스트유저",
        email: String = "test@example.com",
        socialId: String = "12345",
        provider: SocialProvider = SocialProvider.GOOGLE,
        role: RoleJpaEntity? = null,
        profileImage: String = ""
    ): MemberJpaEntity {
        val member = MemberJpaEntity(
            memberId = memberId,
            nickname = nickname,
            email = email,
            socialId = socialId,
            provider = provider,
            role = role,
            profileImage = profileImage
        )
        return memberJpaRepository.save(member)
    }

    protected fun createAndSaveBoard(
        title: String = "테스트 게시판",
        logo: String = "test_logo.png"
    ): BoardJpaEntity {
        val board = BoardJpaEntity(
            title = title,
            logo = logo
        )
        return boardJpaRepository.save(board)
    }

    protected fun getBearerToken(memberPk: Long): String {
        return TestTokenGenerator.getBearerToken(memberPk)
    }

    protected fun createAuthHeaders(memberPk: Long): Map<String, String> {
        return TestTokenGenerator.createAuthHeaders(memberPk)
    }

    /**
     * 관리자 세션 생성 및 SessionId 반환
     */
    protected fun getAdminSessionId(): String {
        return RestAssured
            .given()
                .formParam("token", "test-admin-token")
                .redirects().follow(false)
            .`when`()
                .post("/admin/api/login")
            .then()
                .statusCode(302)
                .extract()
                .sessionId()
    }
}
