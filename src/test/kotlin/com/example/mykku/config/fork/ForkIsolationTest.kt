package com.example.mykku.config.fork

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.config.RedisConfig
import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.DriverManager
import java.util.UUID
import javax.sql.DataSource

@DisplayName("테스트 fork 격리: fork마다 MySQL 스키마와 Redis DB를 따로 쓴다")
class ForkIsolationTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var lettuceConnectionFactory: LettuceConnectionFactory

    private val fork: ForkDatabase
        get() = requireNotNull(TestFork.current()) { "테스트 컨텍스트가 fork 슬롯을 잡지 않았습니다" }

    @Test
    fun `컨텍스트는 이 fork 슬롯의 스키마에 연결하고 슬롯 잠금은 이 JVM이 잡고 있다`() {
        val schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String::class.java)
        val lockHolder = jdbcTemplate.queryForObject("SELECT IS_USED_LOCK(?)", Long::class.java, lockName())

        assertThat(fork.slot).isBetween(1, SlotLocks.MAX_SLOTS)
        assertThat(schema).isEqualTo(fork.schema).endsWith("_${fork.slot}")
        assertThat(lockHolder).isEqualTo(fork.lockConnectionId)
    }

    @Test
    fun `다른 세션에서 보면 fork 스키마는 사용 중이라 다른 실행이 이 슬롯을 건너뛴다`() {
        assertThat(sessionsUsingForkSchemaFromBaseSchema()).isPositive()
    }

    @Test
    fun `fork 스키마는 기준 스키마와 문자셋·콜레이션이 같다`() {
        assertThat(defaultsOf(fork.schema)).isEqualTo(defaultsOf(baseSchema()))
    }

    @Test
    fun `Redisson과 Spring Data Redis는 fork 슬롯 번호의 DB를 쓴다`() {
        val key = "fork-isolation:${UUID.randomUUID()}"
        redissonClient.getBucket<String>(key).set("written")
        val otherDatabase = clientOn(0)
        try {
            assertThat(redissonClient.config.useSingleServer().database).isEqualTo(fork.slot)
            assertThat(lettuceConnectionFactory.database).isEqualTo(fork.slot)
            assertThat(otherDatabase.getBucket<String>(key).isExists).isFalse()
        } finally {
            otherDatabase.shutdown()
            redissonClient.getBucket<String>(key).delete()
        }
    }

    @Test
    fun `Redis 클라이언트 설정은 운영 RedisConfig가 만드는 설정과 DB 번호만 다르다`() {
        val production = productionRedisConfig().redissonClient()
        try {
            val expected = Config(production.config).apply { useSingleServer().setDatabase(fork.slot) }
            assertThat(redissonClient.config.toYAML()).isEqualTo(expected.toYAML())
        } finally {
            production.shutdown()
        }
    }

    @Test
    fun `Hikari는 유휴 연결을 적게 두고 최대 풀 크기는 기본값을 쓴다`() {
        val hikari = dataSource.unwrap(HikariDataSource::class.java)

        assertThat(hikari.minimumIdle).isEqualTo(ForkIsolationContextCustomizer.HIKARI_MINIMUM_IDLE)
        assertThat(hikari.idleTimeout).isEqualTo(ForkIsolationContextCustomizer.HIKARI_IDLE_TIMEOUT_MILLIS)
        assertThat(hikari.maximumPoolSize).isEqualTo(DEFAULT_MAXIMUM_POOL_SIZE)
    }

    private fun lockName(): String = SlotLocks.LOCK_PREFIX + fork.slot

    private fun baseSchema(): String = fork.schema.removeSuffix("_${fork.slot}")

    private fun sessionsUsingForkSchemaFromBaseSchema(): Int = DriverManager.getConnection(
        fork.url,
        environment.getProperty("spring.datasource.username"),
        environment.getProperty("spring.datasource.password")
    ).use { connection ->
        ForkSchema(connection).run {
            switchTo(baseSchema())
            sessionsUsing(fork.schema)
        }
    }

    private fun defaultsOf(schema: String): Map<String, Any> = jdbcTemplate.queryForMap(SCHEMA_DEFAULTS, schema)

    private fun productionRedisConfig(): RedisConfig = RedisConfig(
        environment.getRequiredProperty("spring.data.redis.host"),
        environment.getRequiredProperty("spring.data.redis.port"),
        environment.getRequiredProperty("spring.data.redis.ssl.enabled", Boolean::class.java)
    )

    private fun clientOn(database: Int): RedissonClient =
        Redisson.create(Config(redissonClient.config).apply { useSingleServer().setDatabase(database) })

    private companion object {
        const val DEFAULT_MAXIMUM_POOL_SIZE = 10
        const val SCHEMA_DEFAULTS = "SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME " +
            "FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?"
    }
}
