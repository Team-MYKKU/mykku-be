package com.example.mykku.config.fork

import java.sql.Connection

class ForkSchema(private val connection: Connection) {

    fun recreate(schema: String, likeSchema: String) {
        val (charset, collation) = defaultsOf(likeSchema)
        connection.createStatement().use {
            it.execute("DROP DATABASE IF EXISTS `$schema`")
            it.execute("CREATE DATABASE `$schema` CHARACTER SET $charset COLLATE $collation")
        }
    }

    fun switchTo(schema: String) {
        connection.createStatement().use { it.execute("USE `$schema`") }
    }

    fun sessionsUsing(schema: String): Int = connection.prepareStatement(SESSIONS_USING).use { statement ->
        statement.setString(1, schema)
        statement.executeQuery().use { row -> if (row.next()) row.getInt(1) else 0 }
    }

    fun connectionId(): Long = connection.createStatement().use { statement ->
        statement.executeQuery("SELECT CONNECTION_ID()").use { row -> if (row.next()) row.getLong(1) else 0L }
    }

    private fun defaultsOf(schema: String): Pair<String, String> =
        connection.prepareStatement(SCHEMA_DEFAULTS).use { statement ->
            statement.setString(1, schema)
            statement.executeQuery().use { row ->
                check(row.next()) { "기준 스키마 $schema 가 없어 문자셋·콜레이션을 복사하지 못했습니다" }
                row.getString(1) to row.getString(2)
            }
        }

    private companion object {
        const val SCHEMA_DEFAULTS = "SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME " +
            "FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?"
        const val SESSIONS_USING = "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE DB = ?"
    }
}
