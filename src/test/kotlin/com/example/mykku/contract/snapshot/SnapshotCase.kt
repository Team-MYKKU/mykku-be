package com.example.mykku.contract.snapshot

import com.example.mykku.contract.support.IssuedTokens
import com.example.mykku.contract.support.JsonRules
import io.restassured.response.Response
import io.restassured.specification.MultiPartSpecification
import java.time.LocalDate

sealed interface Caller {
    val label: String

    data object Anonymous : Caller {
        override val label = "익명"
    }

    data class Member(val pk: Long) : Caller {
        override val label = "회원 $pk"
    }
}

interface SnapshotSession {
    val fixtureDate: LocalDate

    fun get(path: String, caller: Caller = Caller.Anonymous, query: Map<String, Any> = emptyMap()): Response

    fun send(method: String, path: String, caller: Caller = Caller.Anonymous, json: Any? = null): Response

    fun multipart(method: String, path: String, caller: Caller, parts: List<MultiPartSpecification>): Response

    fun login(email: String, password: String): IssuedTokens

    fun issuedVerificationCode(email: String, purpose: String): String

    fun requireStatus(status: Int, response: Response): Response

    fun sql(statement: String, vararg args: Any)
}

class SnapshotRules(
    private val masks: List<SnapshotMask> = emptyList(),
    private val pageImplAt: String? = null
) {
    fun bind(clock: SnapshotClock): JsonRules {
        val masked = JsonRules(masks = masks.map { it.bind(clock) })
        return pageImplAt?.let { masked + JsonRules.pageImpl(it) } ?: masked
    }

    companion object {
        val NONE = SnapshotRules()
        val PAGE = SnapshotRules(pageImplAt = "/data")

        fun masks(vararg masks: SnapshotMask): SnapshotRules = SnapshotRules(masks.toList())
    }
}

class SnapshotCase(
    val golden: String,
    val rules: SnapshotRules = SnapshotRules.NONE,
    val tokens: List<String> = emptyList(),
    val call: SnapshotSession.() -> Response
) {
    override fun toString(): String = golden
}

object RequestTitles {
    fun queryString(query: Map<String, Any>): String =
        if (query.isEmpty()) "" else query.entries.joinToString("&", prefix = "?") { "${it.key}=${it.value}" }
}
