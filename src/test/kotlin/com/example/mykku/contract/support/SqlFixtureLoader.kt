package com.example.mykku.contract.support

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.EncodedResource
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.jdbc.datasource.init.ScriptUtils
import java.nio.charset.StandardCharsets.UTF_8
import javax.sql.DataSource

class SqlFixtureLoader(private val dataSource: DataSource) {

    fun load(vararg fixtureNames: String) {
        fixtureNames.forEach { execute(resourceOf(it)) }
    }

    private fun resourceOf(fixtureName: String): ClassPathResource {
        require(FIXTURE_NAME.matches(fixtureName)) {
            "픽스처 이름은 contract/fixtures 바로 아래의 *.sql 파일 이름이어야 합니다: $fixtureName"
        }
        val resource = ClassPathResource("$FIXTURE_ROOT/$fixtureName")
        require(resource.exists()) { "픽스처가 없습니다: classpath:$FIXTURE_ROOT/$fixtureName" }
        return resource
    }

    private fun execute(resource: ClassPathResource) {
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            ScriptUtils.executeSqlScript(connection, EncodedResource(resource, UTF_8))
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }

    private companion object {
        const val FIXTURE_ROOT = "contract/fixtures"
        val FIXTURE_NAME = Regex("[a-z0-9][a-z0-9-]*\\.sql")
    }
}
