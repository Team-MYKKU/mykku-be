package com.example.mykku.contract.guard

import com.example.mykku.BaseControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@DisplayName("계약 스냅샷 가드: DatabaseCleaner 뒤 AUTO_INCREMENT")
class AutoIncrementResetGuardTest : BaseControllerTest() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @RepeatedTest(REPETITIONS)
    fun `정리 뒤 첫 삽입 ID는 매번 1이다`() {
        val firstIds = INSERTS.mapValues { insertAndReadIds(it.key, it.value) }

        assertThat(firstIds).`as`(GUIDE).allSatisfy { _, ids -> assertThat(ids).containsExactly(1L) }
    }

    private fun insertAndReadIds(table: String, insert: String): List<Long> {
        jdbcTemplate.update(insert)
        return jdbcTemplate.queryForList("SELECT id FROM $table", Long::class.java)
    }

    private companion object {
        const val REPETITIONS = 2
        const val GUIDE = "실경로 스냅샷은 픽스처 뒤 서버가 만드는 ID를 그대로 비교한다. DatabaseCleaner는 TRUNCATE로 " +
            "AUTO_INCREMENT를 되돌려야 한다(P01 TEST-020은 TRUNCATE를 바꾸지 않는다)"
        val INSERTS = mapOf(
            "board" to "INSERT INTO board (title, logo, created_at, updated_at) " +
                "VALUES ('가드', 'guard.png', '2001-01-01 00:00:00', '2001-01-01 00:00:00')",
            "member" to "INSERT INTO member (profile_image, email, created_at, updated_at) " +
                "VALUES ('', 'guard@example.com', '2001-01-01 00:00:00', '2001-01-01 00:00:00')",
            "role" to "INSERT INTO role (name, created_at, updated_at) " +
                "VALUES ('가드', '2001-01-01 00:00:00', '2001-01-01 00:00:00')"
        )
    }
}
