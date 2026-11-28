package com.example.mykku.config.fork

class MysqlUrl(url: String) {

    private val parts: MatchResult = requireNotNull(PATTERN.matchEntire(url)) {
        "spring.datasource.url이 jdbc:mysql://<호스트>/<DB>[?<옵션>] 형식이어야 테스트 fork마다 스키마를 나눌 수 있습니다"
    }

    val database: String
        get() = parts.groupValues[DATABASE]

    val server: String
        get() = parts.groupValues[PREFIX].removePrefix(SCHEME).removeSuffix("/").substringAfterLast('@')

    fun withDatabase(name: String): String = parts.groupValues[PREFIX] + name + parts.groupValues[OPTIONS]

    companion object {
        private const val SCHEME = "jdbc:mysql://"
        private val PATTERN = Regex("(jdbc:mysql://[^/?]+/)([^/?]+)(\\?.*)?")
        private const val PREFIX = 1
        private const val DATABASE = 2
        private const val OPTIONS = 3

        fun isMysql(url: String?): Boolean = url?.startsWith(SCHEME) == true
    }
}
