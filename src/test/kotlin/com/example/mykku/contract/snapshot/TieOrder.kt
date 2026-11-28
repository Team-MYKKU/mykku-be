package com.example.mykku.contract.snapshot

import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

class TieSection(
    val path: String,
    val idPath: String,
    val caller: Caller = Caller.Anonymous,
    val query: Map<String, Any> = emptyMap(),
    val paged: Boolean = true,
    val rows: String = TieOrder.ROWS_LABEL
)

class TieCase(
    val golden: String,
    val fixture: String,
    val sections: List<TieSection>,
    val prepare: SnapshotSession.() -> Unit = {}
) {
    override fun toString(): String = golden
}

data class TiePaging(val page: Int, val size: Int) {
    val label: String
        get() = "size=$size page=$page"

    val query: Map<String, Any>
        get() = mapOf("page" to page, "size" to size)
}

class TiePage(val label: String, val ids: List<String>)

class TieObservation(val golden: String, val explain: String)

object TieOrder {
    const val ROWS_LABEL = "101=T0 102~104=T1 105=T2"
    val COMPARED_PAGES: List<TiePaging> = listOf(TiePaging(0, 2), TiePaging(1, 2), TiePaging(2, 2))
    val FULL_PAGE = TiePaging(0, 20)
    private val ROWS = (101..105).map { it.toString() }
    private const val NONE = "없음"

    fun title(section: TieSection, fixture: String): String {
        val request = "GET ${section.path}${RequestTitles.queryString(section.query)} [${section.caller.label}]"
        return "$request ${section.rows} ($fixture)"
    }

    fun render(title: String, pages: List<TiePage>, compared: List<TiePage>): String {
        val seen = compared.flatMap { it.ids }
        val duplicates = seen.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.toList()
        val scope = compared.joinToString(", ") { it.label }
        val lines = listOf("### $title") + pages.map { "${it.label}: ${listed(it.ids)}" } + listOf(
            "중복($scope): ${listed(duplicates)}",
            "누락($scope): ${listed(ROWS - seen.toSet())}"
        )
        return lines.joinToString("\n", postfix = "\n")
    }

    private fun listed(ids: List<String>): String = if (ids.isEmpty()) NONE else ids.joinToString(", ")
}

class StatementLog(private val jdbcTemplate: JdbcTemplate) {

    fun analyze(tables: Collection<String>) {
        tables.forEach { jdbcTemplate.queryForList("ANALYZE TABLE `$it`") }
    }

    fun <T> capture(action: () -> T): Pair<T, List<String>> {
        val wasEnabled = runCatching { consumerEnabled() }.getOrNull()
        val baseline = wasEnabled?.let { runCatching { enableConsumer() }.getOrNull() }
        try {
            val result = action()
            return result to (baseline?.let { selectsAfter(it) } ?: emptyList())
        } finally {
            if (wasEnabled == false) {
                runCatching { setConsumer(DISABLED) }
            }
        }
    }

    fun explain(title: String, statements: List<String>): String {
        if (statements.isEmpty()) {
            return "### $title\nSELECT를 찾지 못했습니다(performance_schema 문장 기록을 켜지 못했거나 같은 MySQL의 다른 실행이 껐음)\n"
        }
        return "### $title\n" + statements.joinToString("") { sql -> "SQL: $sql\n" + explainRows(sql) }
    }

    fun mysqlVersion(): String = jdbcTemplate.queryForObject("SELECT VERSION()", String::class.java).orEmpty()

    fun write(relativePath: String, text: String) {
        val path = Path.of(EXPLAIN_ROOT, relativePath)
        Files.createDirectories(path.parent)
        Files.write(path, text.toByteArray(UTF_8))
    }

    private fun explainRows(sql: String): String = runCatching {
        jdbcTemplate.queryForList("EXPLAIN $sql").joinToString("") { row ->
            row.entries.joinToString(" ", postfix = "\n") { "${it.key}=${it.value}" }
        }
    }.getOrElse { "EXPLAIN 실패: ${it.message}\n" }

    private fun consumerEnabled(): Boolean =
        jdbcTemplate.queryForObject(CONSUMER_STATE, String::class.java, HISTORY_LONG) == ENABLED

    private fun enableConsumer(): Map<Long, Long> {
        setConsumer(ENABLED)
        return jdbcTemplate.query(
            "SELECT THREAD_ID, MAX(EVENT_ID) FROM performance_schema.$HISTORY_LONG GROUP BY THREAD_ID"
        ) { row, _ -> row.getLong(1) to row.getLong(2) }.toMap()
    }

    private fun setConsumer(state: String) {
        jdbcTemplate.update("UPDATE $CONSUMERS SET ENABLED = ? WHERE NAME = ?", state, HISTORY_LONG)
    }

    private fun selectsAfter(baseline: Map<Long, Long>): List<String> = runCatching { recordedSelects() }
        .getOrDefault(emptyList())
        .filter { it.eventId > (baseline[it.threadId] ?: 0L) }
        .sortedBy { it.timerStart }
        .mapNotNull { it.sql }

    private fun recordedSelects(): List<RecordedStatement> = jdbcTemplate.query(
        "SELECT THREAD_ID, EVENT_ID, TIMER_START, SQL_TEXT FROM performance_schema.$HISTORY_LONG " +
            "WHERE CURRENT_SCHEMA = DATABASE() AND LOWER(SQL_TEXT) LIKE 'select%' " +
            "AND SQL_TEXT NOT LIKE '%performance_schema%'"
    ) { row, _ -> RecordedStatement(row.getLong(1), row.getLong(2), row.getBigDecimal(3), row.getString(4)) }

    private class RecordedStatement(
        val threadId: Long,
        val eventId: Long,
        val timerStart: BigDecimal?,
        val sql: String?
    )

    private companion object {
        const val EXPLAIN_ROOT = "build/contract-actual/explain"
        const val HISTORY_LONG = "events_statements_history_long"
        const val CONSUMERS = "performance_schema.setup_consumers"
        const val CONSUMER_STATE = "SELECT ENABLED FROM $CONSUMERS WHERE NAME = ?"
        const val ENABLED = "YES"
        const val DISABLED = "NO"
    }
}
