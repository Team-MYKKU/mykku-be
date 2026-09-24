package com.example.mykku.member.adapter.output.persistence

import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.member.domain.vo.SocialProvider
import com.example.mykku.role.adapter.output.persistence.entity.RoleJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MemberJpaRepository : JpaRepository<MemberJpaEntity, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = "UPDATE member SET role_id = :roleId, updated_at = NOW(6) WHERE id = :memberId",
        nativeQuery = true
    )
    fun assignRole(memberId: Long, roleId: Long)

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = "UPDATE member SET role_id = :roleId, updated_at = NOW(6) WHERE id = :memberId AND role_id IS NULL",
        nativeQuery = true
    )
    fun assignRoleIfAbsent(memberId: Long, roleId: Long): Int

    fun existsByNickname(nickname: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): MemberJpaEntity?
    fun existsByRole(role: RoleJpaEntity): Boolean
    fun existsByRoleId(roleId: Long): Boolean
    fun existsByMemberId(memberId: String): Boolean
    fun existsByMemberIdAndIdNot(memberId: String, id: Long): Boolean
    fun findByMemberId(memberId: String): MemberJpaEntity?
    fun findByProviderAndSocialId(provider: SocialProvider, socialId: String): MemberJpaEntity?
    fun findAllByOrderByCreatedAtDescIdDesc(pageable: Pageable): Page<MemberJpaEntity>
    fun findByMemberIdContainingOrNicknameContainingOrEmailContainingOrderByCreatedAtDescIdDesc(
        memberId: String,
        nickname: String,
        email: String,
        pageable: Pageable
    ): Page<MemberJpaEntity>
}
