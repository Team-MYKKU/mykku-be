package com.example.mykku.contract.support

import com.example.mykku.BaseControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@DisplayName("SQL 픽스처 로더")
class SqlFixtureLoaderTest : BaseControllerTest() {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun loadFixture() {
        SqlFixtureLoader(dataSource).load("support-selftest.sql")
    }

    @Test
    fun `DatabaseCleaner 뒤에 넣은 픽스처가 명시한 ID와 시각 그대로 남는다`() {
        val board = jdbcTemplate.queryForMap(
            "SELECT title, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s.%f') AS created " +
                "FROM board WHERE id = 9001"
        )

        assertThat(board["title"]).isEqualTo("계약 픽스처")
        assertThat(board["created"]).isEqualTo("2026-01-02 03:04:05.120000")
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member WHERE id = 9001", Long::class.java))
            .isEqualTo(1L)
    }

    @Test
    fun `contract-fixtures 밖 경로나 없는 픽스처는 거부한다`() {
        val loader = SqlFixtureLoader(dataSource)

        listOf("../schema.sql", "nested/a.sql", "Upper.sql", "not-sql.txt", "absent-fixture.sql").forEach { name ->
            assertThatThrownBy { loader.load(name) }.`as`(name).isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
