package com.example.mykku.comment.application.usecase

import com.example.mykku.comment.application.dto.CommentAuthorResult
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.entity.Member
import com.example.mykku.member.domain.vo.MemberPk
import com.example.mykku.role.application.dto.RoleResult
import com.example.mykku.role.application.port.output.RoleRepository
import com.example.mykku.role.domain.vo.RoleId
import org.springframework.stereotype.Component

@Component
class CommentAuthorResolver(
    private val memberRepository: MemberRepository,
    private val roleRepository: RoleRepository
) {
    fun resolve(memberIds: List<Long>): Map<Long, CommentAuthorResult> {
        if (memberIds.isEmpty()) return emptyMap()
        val members = memberRepository.findByIds(memberIds.distinct().map { MemberPk.of(it) })
        val rolesById = loadRoles(members)
        return members.associate { it.id.value to CommentAuthorResult.of(it, it.roleId?.let(rolesById::get)) }
    }

    private fun loadRoles(members: List<Member>): Map<Long, RoleResult> {
        val roleIds = members.mapNotNull { it.roleId }.distinct()
        if (roleIds.isEmpty()) return emptyMap()
        return roleRepository.findByIds(roleIds.map { RoleId.of(it) })
            .associate { it.id.value to RoleResult(it.id.value, it.name, it.description) }
    }
}
