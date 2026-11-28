package com.example.mykku.config.fork

import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration

class ForkDatabase(val slot: Int, val schema: String, val url: String, val lockConnectionId: Long)

object TestFork {

    private val LOCK_SESSION_IDLE_LIMIT: Duration = Duration.ofDays(365)
    private val SCHEMA_DDL_LOCK_WAIT: Duration = Duration.ofSeconds(60)
    private val SLOT_LOCK_SESSION = listOf(
        "SET SESSION wait_timeout = ${LOCK_SESSION_IDLE_LIMIT.toSeconds()}",
        "SET SESSION lock_wait_timeout = ${SCHEMA_DDL_LOCK_WAIT.toSeconds()}"
    )
    private val log = LoggerFactory.getLogger(TestFork::class.java)
    private var acquired: ForkDatabase? = null
    private var failure: Throwable? = null

    @Synchronized
    fun acquire(environment: Environment): ForkDatabase {
        acquired?.let { return it }
        failure?.let { throw IllegalStateException("이 테스트 JVM은 앞서 fork 슬롯을 잡지 못했습니다", it) }
        return runCatching { allocate(environment) }
            .onSuccess { acquired = it }
            .onFailure { failure = it }
            .getOrThrow()
    }

    @Synchronized
    fun current(): ForkDatabase? = acquired

    private fun allocate(environment: Environment): ForkDatabase {
        val url = environment.getRequiredProperty("spring.datasource.url")
        val connection = connect(environment, url)
        try {
            return prepare(connection, MysqlUrl(url), environment).also { closeAtExit(connection) }
        } catch (exception: Exception) {
            connection.close()
            throw exception
        }
    }

    private fun prepare(connection: Connection, base: MysqlUrl, environment: Environment): ForkDatabase {
        connection.createStatement().use { statement -> SLOT_LOCK_SESSION.forEach { statement.execute(it) } }
        val schemas = ForkSchema(connection)
        val slot = SlotLocks(connection).lockFree { schemas.sessionsUsing(schemaOf(base, it)) > 0 }
        val schema = schemaOf(base, slot)
        schemas.recreate(schema, likeSchema = base.database)
        schemas.switchTo(schema)
        val redis = ForkRedisDatabase.addressOf(environment)
        log.info("테스트 fork 슬롯 {}: MySQL {} 스키마 {}, Redis {} DB {}", slot, base.server, schema, redis, slot)
        return ForkDatabase(slot, schema, base.withDatabase(schema), schemas.connectionId())
    }

    private fun closeAtExit(connection: Connection) {
        Runtime.getRuntime().addShutdownHook(Thread { connection.close() })
    }

    private fun schemaOf(base: MysqlUrl, slot: Int): String = "${base.database}_$slot"

    private fun connect(environment: Environment, url: String): Connection = DriverManager.getConnection(
        url,
        environment.getProperty("spring.datasource.username"),
        environment.getProperty("spring.datasource.password")
    )
}
