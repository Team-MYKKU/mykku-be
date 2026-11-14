package com.example.mykku.block.application.usecase

import com.example.mykku.block.application.port.input.BlockFilterUseCase
import com.example.mykku.block.application.port.output.KeywordBlockRepository
import com.example.mykku.block.application.port.output.MemberBlockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BlockFilterService(
    private val memberBlockRepository: MemberBlockRepository,
    private val keywordBlockRepository: KeywordBlockRepository
) : BlockFilterUseCase {

    override fun getBlockedMemberIds(memberId: Long): Set<Long> {
        return memberBlockRepository.findBlockedIdsByBlockerId(memberId).toSet()
    }

    override fun getBlockerMemberIds(memberId: Long): Set<Long> {
        return memberBlockRepository.findBlockerIdsByBlockedId(memberId).toSet()
    }

    override fun getAllBlockRelatedMemberIds(memberId: Long): Set<Long> {
        val blockedByMe = getBlockedMemberIds(memberId)
        val blockedMe = getBlockerMemberIds(memberId)
        return blockedByMe + blockedMe
    }

    override fun getHiddenMemberIds(viewerId: Long?): Set<Long> {
        return viewerId?.let { getAllBlockRelatedMemberIds(it) } ?: emptySet()
    }

    override fun getBlockedKeywords(memberId: Long): List<String> {
        return keywordBlockRepository.findKeywordsByMemberId(memberId)
    }

    override fun <T> filterContent(
        items: List<T>,
        memberId: Long?,
        memberIdExtractor: (T) -> Long?,
        contentExtractors: List<(T) -> String?>
    ): List<T> {
        if (memberId == null || items.isEmpty()) return items

        val blockedMemberIds = getAllBlockRelatedMemberIds(memberId)
        val blockedKeywords = getBlockedKeywords(memberId)

        return items.filter { item ->
            val authorId = memberIdExtractor(item)
            if (authorId != null && authorId in blockedMemberIds) return@filter false

            val hasBlockedKeyword = contentExtractors.any { extractor ->
                val content = extractor(item)?.lowercase() ?: return@any false
                blockedKeywords.any { keyword -> content.contains(keyword) }
            }

            !hasBlockedKeyword
        }
    }

    override fun <T> filterByBlockedMembers(
        items: List<T>,
        memberId: Long?,
        memberIdExtractor: (T) -> Long?
    ): List<T> {
        if (memberId == null || items.isEmpty()) return items

        val blockedMemberIds = getAllBlockRelatedMemberIds(memberId)

        return items.filter { item ->
            val authorId = memberIdExtractor(item)
            authorId == null || authorId !in blockedMemberIds
        }
    }

    override fun <T> filterByBlockedKeywords(
        items: List<T>,
        memberId: Long?,
        contentExtractors: List<(T) -> String?>
    ): List<T> {
        if (memberId == null || items.isEmpty()) return items

        val blockedKeywords = getBlockedKeywords(memberId)
        if (blockedKeywords.isEmpty()) return items

        return items.filter { item ->
            val hasBlockedKeyword = contentExtractors.any { extractor ->
                val content = extractor(item)?.lowercase() ?: return@any false
                blockedKeywords.any { keyword -> content.contains(keyword) }
            }
            !hasBlockedKeyword
        }
    }
}
