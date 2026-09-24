package com.example.mykku.member.application.usecase

import com.example.mykku.common.util.PageableValidator
import com.example.mykku.member.application.dto.MemberSearchItemResult
import com.example.mykku.member.application.dto.PagedMemberSearchResult
import com.example.mykku.member.application.dto.SearchMembersQuery
import com.example.mykku.member.application.port.input.SearchMembersUseCase
import com.example.mykku.member.application.port.output.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SearchMembersUseCaseImpl(
    private val memberRepository: MemberRepository
) : SearchMembersUseCase {

    override fun search(query: SearchMembersQuery): PagedMemberSearchResult {
        val pageable = PageableValidator.validateAndCreate(query.page, query.size)
        val page = memberRepository.search(query.keyword, pageable)
        return PagedMemberSearchResult(
            content = page.content.map { MemberSearchItemResult.from(it) },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            isLast = page.isLast
        )
    }
}
