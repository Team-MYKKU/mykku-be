package com.example.mykku.config.fork

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("테스트 fork JDBC URL")
class MysqlUrlTest {

    @Test
    fun `DB 이름만 바꾸고 호스트와 접속 옵션은 그대로 둔다`() {
        val url = MysqlUrl("jdbc:mysql://localhost:3411/mykku_test?sessionVariables=time_zone='%2B09:00'")

        assertThat(url.database).isEqualTo("mykku_test")
        assertThat(url.server).isEqualTo("localhost:3411")
        assertThat(url.withDatabase("mykku_test_3"))
            .isEqualTo("jdbc:mysql://localhost:3411/mykku_test_3?sessionVariables=time_zone='%2B09:00'")
    }

    @Test
    fun `접속 옵션이 없는 URL도 DB 이름을 바꾼다`() {
        assertThat(MysqlUrl("jdbc:mysql://127.0.0.1:3311/mykku_test").withDatabase("mykku_test_1"))
            .isEqualTo("jdbc:mysql://127.0.0.1:3311/mykku_test_1")
    }

    @Test
    fun `로그에 쓰는 서버 주소에는 URL의 사용자 정보를 남기지 않는다`() {
        assertThat(MysqlUrl("jdbc:mysql://root:secret@localhost:3311/mykku_test").server).isEqualTo("localhost:3311")
    }

    @Test
    fun `MySQL이 아닌 URL은 fork 격리 대상이 아니다`() {
        assertThat(MysqlUrl.isMysql("jdbc:h2:mem:mykku_test")).isFalse()
        assertThat(MysqlUrl.isMysql(null)).isFalse()
        assertThat(MysqlUrl.isMysql("jdbc:mysql://localhost:3311/mykku_test")).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["jdbc:mysql://localhost:3311/", "jdbc:mysql://localhost:3311"])
    fun `DB 이름이 없는 MySQL URL은 조용히 건너뛰지 않고 거부한다`(url: String) {
        assertThat(MysqlUrl.isMysql(url)).isTrue()
        assertThatThrownBy { MysqlUrl(url) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
