package com.example.mykku.contract.snapshot

import io.restassured.response.Response

interface GrayZoneSession : SnapshotSession {
    val memo: MutableMap<String, String>
}

class GrayZoneStep(
    val rules: SnapshotRules = SnapshotRules.NONE,
    val call: GrayZoneSession.() -> Response
)

class GrayZoneCase(
    val golden: String,
    val reportId: String,
    val summary: String,
    val fixtures: List<String>,
    val steps: List<GrayZoneStep>
) {
    val header: String
        get() = "# $reportId · $APPROVAL_RULE · $summary\n"

    override fun toString(): String = "$reportId $golden"

    companion object {
        const val APPROVAL_RULE = "승인된 B PR만 바꿈"
    }
}
