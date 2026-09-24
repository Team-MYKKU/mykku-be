package com.example.mykku.member.application.port.input

import com.example.mykku.member.application.dto.PagedMemberSearchResult
import com.example.mykku.member.application.dto.SearchMembersQuery

interface SearchMembersUseCase {
    fun search(query: SearchMembersQuery): PagedMemberSearchResult
}
