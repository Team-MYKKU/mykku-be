package com.example.mykku.util

import com.example.mykku.BaseRepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@DisplayName("리포지토리 테스트 정리 가드: 클래스 시작 TRUNCATE")
class ClassLevelDatabaseCleanerTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `리포지토리 테스트 클래스는 모든 테이블이 비고 AUTO_INCREMENT가 1인 상태로 시작한다`() {
        val tableNames = jdbcTemplate.queryForList(TABLE_NAME_SELECT, String::class.java)
        val nonEmptyTables = tableNames.filter { rowCount(it) > 0 }

        jdbcTemplate.update(BOARD_INSERT)
        val boardIds = jdbcTemplate.queryForList("SELECT id FROM board", Long::class.java)

        assertThat(nonEmptyTables).`as`(GUIDE).isEmpty()
        assertThat(boardIds).`as`(GUIDE).containsExactly(1L)
    }

    private fun rowCount(table: String): Long =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `$table`", Long::class.java) ?: 0L

    private companion object {
        const val GUIDE = "BaseRepositoryTest는 ClassLevelDatabaseCleaner로 클래스 시작 때 모든 테이블을 TRUNCATE한다. " +
            "빈 결과·전체 행 수 단언과 Flyway 시드(role) 제거가 이 정리에 기댄다(P01 TEST-020)"
        const val TABLE_NAME_SELECT = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' AND TABLE_NAME NOT LIKE 'flyway_%'"
        const val BOARD_INSERT = "INSERT INTO board (title, logo, created_at, updated_at) " +
            "VALUES ('가드', 'guard.png', '2001-01-01 00:00:00', '2001-01-01 00:00:00')"
    }
}
