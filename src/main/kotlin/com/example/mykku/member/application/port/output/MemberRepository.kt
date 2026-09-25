package com.example.mykku.member.application.port.output

import com.example.mykku.member.domain.entity.Member
import com.example.mykku.member.domain.vo.MemberPk
import com.example.mykku.member.domain.vo.SocialProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface MemberRepository {
    fun save(member: Member): Member
    fun assignRole(id: MemberPk, roleId: Long)
    fun assignRoleIfAbsent(id: MemberPk, roleId: Long): Boolean
    fun findById(id: MemberPk): Member?
    fun findByIds(ids: List<MemberPk>): List<Member>
    fun findByProviderAndSocialId(provider: SocialProvider, socialId: String): Member?
    fun existsByNickname(nickname: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): Member?
    fun existsByMemberId(memberId: String): Boolean
    fun existsByMemberIdExcludingId(memberId: String, id: MemberPk): Boolean
    fun findByMemberId(memberId: String): Member?
    fun findByMemberIds(memberIds: List<String>): List<Member>
    fun deleteById(id: MemberPk)
    fun search(keyword: String?, pageable: Pageable): Page<Member>
}
