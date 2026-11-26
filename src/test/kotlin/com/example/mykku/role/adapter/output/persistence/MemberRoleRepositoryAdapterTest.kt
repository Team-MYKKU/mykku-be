package com.example.mykku.role.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.role.application.port.output.MemberRoleRepository
import com.example.mykku.role.domain.entity.MemberRole
import com.example.mykku.role.domain.vo.MemberRoleId
import com.example.mykku.role.domain.vo.RoleId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("MemberRoleRepository 통합 테스트")
class MemberRoleRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var memberRoleRepository: MemberRoleRepository

    private fun createMemberRole(
        memberId: Long,
        roleId: RoleId
    ): MemberRole {
        return MemberRole.create(memberId = memberId, roleId = roleId)
    }

    @Nested
    @DisplayName("save 메서드")
    inner class Save {

        @Test
        @DisplayName("새로운 멤버 역할을 저장하면 ID가 생성된다")
        fun saveNewMemberRole() {
            val role = createAndSaveRole(name = "save_role", description = "저장 테스트 역할")
            val member = createAndSaveMember(email = "save_member1@test.com", socialId = "save_social1")

            val memberRole = createMemberRole(memberId = member.id, roleId = RoleId(role.id!!))

            val savedMemberRole = memberRoleRepository.save(memberRole)

            assertThat(savedMemberRole.id.value).isGreaterThan(0)
            assertThat(savedMemberRole.memberId).isEqualTo(member.id)
            assertThat(savedMemberRole.roleId.value).isEqualTo(role.id)
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("존재하는 ID로 조회하면 멤버 역할을 반환한다")
        fun findByExistingId() {
            val role = createAndSaveRole(name = "findById_role", description = "조회 테스트 역할")
            val member = createAndSaveMember(email = "findById_member1@test.com", socialId = "findById_social1")

            val savedMemberRole = memberRoleRepository.save(createMemberRole(memberId = member.id, roleId = RoleId(role.id!!)))

            val foundMemberRole = memberRoleRepository.findById(savedMemberRole.id)

            assertThat(foundMemberRole).isNotNull
            assertThat(foundMemberRole!!.id).isEqualTo(savedMemberRole.id)
            assertThat(foundMemberRole.memberId).isEqualTo(savedMemberRole.memberId)
            assertThat(foundMemberRole.roleId).isEqualTo(savedMemberRole.roleId)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun findByNonExistingId() {
            val foundMemberRole = memberRoleRepository.findById(MemberRoleId(999999L))

            assertThat(foundMemberRole).isNull()
        }
    }

    @Nested
    @DisplayName("findByMemberId 메서드")
    inner class FindByMemberId {

        @Test
        @DisplayName("멤버 ID로 조회하면 해당 멤버의 모든 역할을 반환한다")
        fun findByMemberId() {
            val role1 = createAndSaveRole(name = "fmid_role1", description = "역할1")
            val role2 = createAndSaveRole(name = "fmid_role2", description = "역할2")
            val member1 = createAndSaveMember(memberId = "fmid_m1", email = "fmid_m1@test.com", socialId = "fmid_s1")
            val member2 = createAndSaveMember(memberId = "fmid_m2", email = "fmid_m2@test.com", socialId = "fmid_s2")

            memberRoleRepository.save(createMemberRole(memberId = member1.id, roleId = RoleId(role1.id!!)))
            memberRoleRepository.save(createMemberRole(memberId = member1.id, roleId = RoleId(role2.id!!)))
            memberRoleRepository.save(createMemberRole(memberId = member2.id, roleId = RoleId(role1.id!!)))

            val memberRoles = memberRoleRepository.findByMemberId(member1.id)

            assertThat(memberRoles).hasSize(2)
            assertThat(memberRoles.map { it.roleId.value }).containsExactlyInAnyOrder(role1.id, role2.id)
        }

        @Test
        @DisplayName("존재하지 않는 멤버 ID로 조회하면 빈 리스트를 반환한다")
        fun findByNonExistingMemberId() {
            val memberRoles = memberRoleRepository.findByMemberId(999999L)

            assertThat(memberRoles).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByMemberIdWithRole 메서드")
    inner class FindByMemberIdWithRole {

        @Test
        @DisplayName("멤버 ID로 조회하면 역할 정보와 함께 반환한다")
        fun findByMemberIdWithRole() {
            val role1 = createAndSaveRole(name = "fwr_ADMIN", description = "관리자")
            val role2 = createAndSaveRole(name = "fwr_USER", description = "일반 사용자")
            val member = createAndSaveMember(email = "fwr_m1@test.com", socialId = "fwr_s1")

            memberRoleRepository.save(createMemberRole(memberId = member.id, roleId = RoleId(role1.id!!)))
            memberRoleRepository.save(createMemberRole(memberId = member.id, roleId = RoleId(role2.id!!)))

            val memberRolesWithRole = memberRoleRepository.findByMemberIdWithRole(member.id)

            assertThat(memberRolesWithRole).hasSize(2)
            assertThat(memberRolesWithRole.map { it.role.name }).containsExactlyInAnyOrder("fwr_ADMIN", "fwr_USER")
        }

        @Test
        @DisplayName("존재하지 않는 멤버 ID로 조회하면 빈 리스트를 반환한다")
        fun findByNonExistingMemberIdWithRole() {
            val memberRolesWithRole = memberRoleRepository.findByMemberIdWithRole(999999L)

            assertThat(memberRolesWithRole).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByMemberIdAndRoleId 메서드")
    inner class FindByMemberIdAndRoleId {

        @Test
        @DisplayName("해당 멤버와 역할 조합이 존재하면 보유 칭호를 반환한다")
        fun findByMemberIdAndRoleId() {
            val role = createAndSaveRole(name = "emr_role", description = "존재 확인 역할")
            val member = createAndSaveMember(email = "emr_m1@test.com", socialId = "emr_s1")

            val saved = memberRoleRepository.save(createMemberRole(memberId = member.id, roleId = RoleId(role.id!!)))

            val found = memberRoleRepository.findByMemberIdAndRoleId(member.id, RoleId(role.id!!))

            assertThat(found).isNotNull
            assertThat(found!!.id).isEqualTo(saved.id)
            assertThat(found.memberId).isEqualTo(member.id)
            assertThat(found.roleId).isEqualTo(RoleId(role.id!!))
        }

        @Test
        @DisplayName("해당 멤버와 역할 조합이 존재하지 않으면 null을 반환한다")
        fun notFoundByMemberIdAndRoleId() {
            val role = createAndSaveRole(name = "nemr_role", description = "존재하지 않는 역할")

            val found = memberRoleRepository.findByMemberIdAndRoleId(999999L, RoleId(role.id!!))

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("existsByRoleId 메서드")
    inner class ExistsByRoleId {

        @Test
        @DisplayName("해당 역할 ID를 가진 멤버 역할이 존재하면 true를 반환한다")
        fun existsByRoleId() {
            val role = createAndSaveRole(name = "eri_role", description = "역할 존재 확인")
            val member = createAndSaveMember(email = "eri_m1@test.com", socialId = "eri_s1")

            memberRoleRepository.save(createMemberRole(memberId = member.id, roleId = RoleId(role.id!!)))

            val exists = memberRoleRepository.existsByRoleId(RoleId(role.id!!))

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("해당 역할 ID를 가진 멤버 역할이 존재하지 않으면 false를 반환한다")
        fun notExistsByRoleId() {
            val exists = memberRoleRepository.existsByRoleId(RoleId(999999L))

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    inner class Delete {

        @Test
        @DisplayName("멤버 역할을 삭제하면 조회되지 않는다")
        fun deleteMemberRole() {
            val role = createAndSaveRole(name = "delete_role", description = "삭제 테스트 역할")
            val member = createAndSaveMember(email = "delete_member1@test.com", socialId = "delete_social1")

            val savedMemberRole = memberRoleRepository.save(createMemberRole(memberId = member.id, roleId = RoleId(role.id!!)))

            memberRoleRepository.delete(savedMemberRole)

            val foundMemberRole = memberRoleRepository.findById(savedMemberRole.id)
            assertThat(foundMemberRole).isNull()
            flushAndClear()
            assertThat(memberRoleRepository.findById(savedMemberRole.id)).isNull()
        }
    }

    @Nested
    @DisplayName("findUncheckedByMemberIdWithRole / markChecked 메서드")
    inner class UncheckedRoles {

        @Test
        @DisplayName("확인 처리되지 않은 보유 칭호만 역할 정보와 함께 반환한다")
        fun findsOnlyUnchecked() {
            val role1 = createAndSaveRole(name = "unchecked_role1", description = "미확인1")
            val role2 = createAndSaveRole(name = "unchecked_role2", description = "미확인2")
            val member = createAndSaveMember(email = "unchecked_m1@test.com", socialId = "unchecked_s1")

            val target = memberRoleRepository.save(createMemberRole(member.id, RoleId(role1.id!!)))
            val checked = memberRoleRepository.save(createMemberRole(member.id, RoleId(role2.id!!)))
            memberRoleRepository.markChecked(checked.id)

            val unchecked = memberRoleRepository.findUncheckedByMemberIdWithRole(member.id)

            assertThat(unchecked).hasSize(1)
            assertThat(unchecked.first().memberRole.id).isEqualTo(target.id)
            assertThat(unchecked.first().role.name).isEqualTo("unchecked_role1")
        }

        @Test
        @DisplayName("markChecked 후에는 미확인 목록이 비어 있다")
        fun markCheckedEmptiesUnchecked() {
            val role = createAndSaveRole(name = "mark_role", description = "마킹")
            val member = createAndSaveMember(email = "mark_m1@test.com", socialId = "mark_s1")
            val saved = memberRoleRepository.save(createMemberRole(member.id, RoleId(role.id!!)))

            memberRoleRepository.markChecked(saved.id)

            assertThat(memberRoleRepository.findUncheckedByMemberIdWithRole(member.id)).isEmpty()
        }

        @Test
        @DisplayName("이미 확인된 칭호는 markChecked를 재호출해도 checkedAt이 갱신되지 않는다")
        fun markCheckedIsIdempotent() {
            val role = createAndSaveRole(name = "idem_role", description = "멱등")
            val member = createAndSaveMember(email = "idem_m1@test.com", socialId = "idem_s1")
            val saved = memberRoleRepository.save(createMemberRole(member.id, RoleId(role.id!!)))

            memberRoleRepository.markChecked(saved.id)
            val firstCheckedAt = memberRoleRepository.findById(saved.id)!!.checkedAt
            memberRoleRepository.markChecked(saved.id)

            assertThat(firstCheckedAt).isNotNull()
            assertThat(memberRoleRepository.findById(saved.id)!!.checkedAt).isEqualTo(firstCheckedAt)
        }

        @Test
        @DisplayName("markChecked는 checkedAt과 함께 updatedAt도 갱신한다")
        fun markCheckedRefreshesUpdatedAt() {
            val role = createAndSaveRole(name = "audit_role", description = "감사")
            val member = createAndSaveMember(email = "audit_m1@test.com", socialId = "audit_s1")
            val saved = memberRoleRepository.save(createMemberRole(member.id, RoleId(role.id!!)))
            val updatedAtBeforeCheck = memberRoleRepository.findById(saved.id)!!.updatedAt

            memberRoleRepository.markChecked(saved.id)

            val reloaded = memberRoleRepository.findById(saved.id)!!
            assertThat(reloaded.checkedAt).isNotNull()
            assertThat(reloaded.updatedAt).isEqualTo(reloaded.checkedAt)
            assertThat(reloaded.updatedAt).isAfterOrEqualTo(updatedAtBeforeCheck)
        }

        @Test
        @DisplayName("markChecked는 미확인 칭호를 처음 확인할 때만 true를 반환한다")
        fun markCheckedReturnsTrueOnlyOnFirstCall() {
            val role = createAndSaveRole(name = "rowcount_role", description = "행수")
            val member = createAndSaveMember(email = "rowcount_m1@test.com", socialId = "rowcount_s1")
            val saved = memberRoleRepository.save(createMemberRole(member.id, RoleId(role.id!!)))

            assertThat(memberRoleRepository.markChecked(saved.id)).isTrue()
            assertThat(memberRoleRepository.markChecked(saved.id)).isFalse()
        }

        @Test
        @DisplayName("새로 저장한 보유 칭호의 checkedAt은 null이다")
        fun newMemberRoleIsUnchecked() {
            val role = createAndSaveRole(name = "fresh_role", description = "신규")
            val member = createAndSaveMember(email = "fresh_m1@test.com", socialId = "fresh_s1")

            val saved = memberRoleRepository.save(createMemberRole(member.id, RoleId(role.id!!)))

            assertThat(memberRoleRepository.findById(saved.id)!!.checkedAt).isNull()
        }

        @Test
        @DisplayName("saveIfAbsent로 넣은 보유 칭호도 미확인 상태로 조회된다")
        fun saveIfAbsentLeavesUnchecked() {
            val role = createAndSaveRole(name = "absent_role", description = "멱등 저장")
            val member = createAndSaveMember(email = "absent_m1@test.com", socialId = "absent_s1")

            val inserted = memberRoleRepository.saveIfAbsent(member.id, RoleId(role.id!!))

            assertThat(inserted).isTrue()
            assertThat(memberRoleRepository.findUncheckedByMemberIdWithRole(member.id)).hasSize(1)
        }
    }
}
