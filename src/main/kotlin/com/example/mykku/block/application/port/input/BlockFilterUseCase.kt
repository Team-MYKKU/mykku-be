package com.example.mykku.block.application.port.input

interface BlockFilterUseCase {
    fun getBlockedMemberIds(memberId: Long): Set<Long>
    fun getBlockerMemberIds(memberId: Long): Set<Long>
    fun getAllBlockRelatedMemberIds(memberId: Long): Set<Long>
    fun getHiddenMemberIds(viewerId: Long?): Set<Long>
    fun getBlockedKeywords(memberId: Long): List<String>

    fun <T> filterContent(
        items: List<T>,
        memberId: Long?,
        memberIdExtractor: (T) -> Long?,
        contentExtractors: List<(T) -> String?>
    ): List<T>

    fun <T> filterByBlockedMembers(
        items: List<T>,
        memberId: Long?,
        memberIdExtractor: (T) -> Long?
    ): List<T>

    fun <T> filterByBlockedKeywords(
        items: List<T>,
        memberId: Long?,
        contentExtractors: List<(T) -> String?>
    ): List<T>
}
