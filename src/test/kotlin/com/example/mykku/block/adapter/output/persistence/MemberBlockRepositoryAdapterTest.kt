package com.example.mykku.block.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.block.application.port.output.MemberBlockRepository
import com.example.mykku.block.domain.entity.MemberBlock
import com.example.mykku.block.domain.vo.MemberBlockId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

@DisplayName("MemberBlockRepository 통합 테스트")
class MemberBlockRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var memberBlockRepository: MemberBlockRepository

    private fun createMemberBlock(
        blockerId: Long,
        blockedId: Long
    ): MemberBlock {
        return MemberBlock.create(blockerId = blockerId, blockedId = blockedId)
    }

    @Nested
    @DisplayName("save 메서드")
    inner class Save {

        @Test
        @DisplayName("새로운 멤버 차단을 저장하면 ID가 생성된다")
        fun saveNewMemberBlock() {
            val blocker = createAndSaveMember(memberId = "sv_blker1", email = "sv_blker1@test.com", socialId = "sv_s1")
            val blocked = createAndSaveMember(memberId = "sv_blked1", email = "sv_blked1@test.com", socialId = "sv_s2")

            val memberBlock = createMemberBlock(blockerId = blocker.id, blockedId = blocked.id)
            val savedMemberBlock = memberBlockRepository.save(memberBlock)

            assertThat(savedMemberBlock.id).isNotNull
            assertThat(savedMemberBlock.id!!.value).isGreaterThan(0)
            assertThat(savedMemberBlock.blockerId).isEqualTo(blocker.id)
            assertThat(savedMemberBlock.blockedId).isEqualTo(blocked.id)
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("존재하는 ID로 조회하면 멤버 차단을 반환한다")
        fun findByExistingId() {
            val blocker = createAndSaveMember(memberId = "fbi_blker1", email = "fbi_blker1@test.com", socialId = "fbi_s1")
            val blocked = createAndSaveMember(memberId = "fbi_blked1", email = "fbi_blked1@test.com", socialId = "fbi_s2")

            val savedMemberBlock = memberBlockRepository.save(createMemberBlock(blockerId = blocker.id, blockedId = blocked.id))

            val foundMemberBlock = memberBlockRepository.findById(savedMemberBlock.id!!)

            assertThat(foundMemberBlock).isNotNull
            assertThat(foundMemberBlock!!.id).isEqualTo(savedMemberBlock.id)
            assertThat(foundMemberBlock.blockerId).isEqualTo(savedMemberBlock.blockerId)
            assertThat(foundMemberBlock.blockedId).isEqualTo(savedMemberBlock.blockedId)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun findByNonExistingId() {
            val foundMemberBlock = memberBlockRepository.findById(MemberBlockId(999999L))

            assertThat(foundMemberBlock).isNull()
        }
    }

    @Nested
    @DisplayName("findByBlockerIdAndBlockedId 메서드")
    inner class FindByBlockerIdAndBlockedId {

        @Test
        @DisplayName("차단자와 피차단자 ID로 조회하면 멤버 차단을 반환한다")
        fun findByBlockerIdAndBlockedId() {
            val blocker = createAndSaveMember(memberId = "fbb_blker1", email = "fbb_blker1@test.com", socialId = "fbb_s1")
            val blocked = createAndSaveMember(memberId = "fbb_blked1", email = "fbb_blked1@test.com", socialId = "fbb_s2")

            memberBlockRepository.save(createMemberBlock(blockerId = blocker.id, blockedId = blocked.id))

            val foundMemberBlock = memberBlockRepository.findByBlockerIdAndBlockedId(blocker.id, blocked.id)

            assertThat(foundMemberBlock).isNotNull
            assertThat(foundMemberBlock!!.blockerId).isEqualTo(blocker.id)
            assertThat(foundMemberBlock.blockedId).isEqualTo(blocked.id)
        }

        @Test
        @DisplayName("존재하지 않는 조합으로 조회하면 null을 반환한다")
        fun findByNonExistingBlockerIdAndBlockedId() {
            val foundMemberBlock = memberBlockRepository.findByBlockerIdAndBlockedId(-1L, -2L)

            assertThat(foundMemberBlock).isNull()
        }
    }

    @Nested
    @DisplayName("existsByBlockerIdAndBlockedId 메서드")
    inner class ExistsByBlockerIdAndBlockedId {

        @Test
        @DisplayName("차단 관계가 존재하면 true를 반환한다")
        fun existsByBlockerIdAndBlockedId() {
            val blocker = createAndSaveMember(memberId = "ex_blker1", email = "ex_blker1@test.com", socialId = "ex_s1")
            val blocked = createAndSaveMember(memberId = "ex_blked1", email = "ex_blked1@test.com", socialId = "ex_s2")

            memberBlockRepository.save(createMemberBlock(blockerId = blocker.id, blockedId = blocked.id))

            val exists = memberBlockRepository.existsByBlockerIdAndBlockedId(blocker.id, blocked.id)

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("차단 관계가 존재하지 않으면 false를 반환한다")
        fun notExistsByBlockerIdAndBlockedId() {
            val exists = memberBlockRepository.existsByBlockerIdAndBlockedId(-1L, -2L)

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("findAllByBlockerId 메서드")
    inner class FindAllByBlockerId {

        @Test
        @DisplayName("차단자 ID로 조회하면 페이징된 차단 목록을 반환한다")
        fun findAllByBlockerId() {
            val blocker1 = createAndSaveMember(memberId = "fa_blker1", email = "fa_blker1@test.com", socialId = "fa_s1")
            val blocker2 = createAndSaveMember(memberId = "fa_blker2", email = "fa_blker2@test.com", socialId = "fa_s2")
            val blocked1 = createAndSaveMember(memberId = "fa_blked1", email = "fa_blked1@test.com", socialId = "fa_s3")
            val blocked2 = createAndSaveMember(memberId = "fa_blked2", email = "fa_blked2@test.com", socialId = "fa_s4")
            val blocked3 = createAndSaveMember(memberId = "fa_blked3", email = "fa_blked3@test.com", socialId = "fa_s5")

            memberBlockRepository.save(createMemberBlock(blockerId = blocker1.id, blockedId = blocked1.id))
            memberBlockRepository.save(createMemberBlock(blockerId = blocker1.id, blockedId = blocked2.id))
            memberBlockRepository.save(createMemberBlock(blockerId = blocker1.id, blockedId = blocked3.id))
            memberBlockRepository.save(createMemberBlock(blockerId = blocker2.id, blockedId = blocked1.id))

            val pageable = PageRequest.of(0, 10)
            val page = memberBlockRepository.findAllByBlockerId(blocker1.id, pageable)

            assertThat(page.content).hasSize(3)
            assertThat(page.totalElements).isEqualTo(3)
            assertThat(page.content.map { it.blockedId }).containsExactlyInAnyOrder(blocked1.id, blocked2.id, blocked3.id)
        }

        @Test
        @DisplayName("차단 목록이 없으면 빈 페이지를 반환한다")
        fun findAllByBlockerIdWhenEmpty() {
            val pageable = PageRequest.of(0, 10)
            val page = memberBlockRepository.findAllByBlockerId(-1L, pageable)

            assertThat(page.content).isEmpty()
            assertThat(page.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("페이지 크기에 맞게 결과를 반환한다")
        fun findAllByBlockerIdWithPaging() {
            val blocker = createAndSaveMember(memberId = "pg_blker1", email = "pg_blker1@test.com", socialId = "pg_s1")
            for (i in 1..5) {
                val blocked = createAndSaveMember(memberId = "pg_blked$i", email = "pg_blked$i@test.com", socialId = "pg_sb$i")
                memberBlockRepository.save(createMemberBlock(blockerId = blocker.id, blockedId = blocked.id))
            }

            val pageable = PageRequest.of(0, 2)
            val page = memberBlockRepository.findAllByBlockerId(blocker.id, pageable)

            assertThat(page.content).hasSize(2)
            assertThat(page.totalElements).isEqualTo(5)
            assertThat(page.totalPages).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("findBlockedIdsByBlockerId 메서드")
    inner class FindBlockedIdsByBlockerId {

        @Test
        @DisplayName("차단자 ID로 피차단자 ID 목록을 반환한다")
        fun findBlockedIdsByBlockerId() {
            val blocker1 = createAndSaveMember(memberId = "fbd_blker1", email = "fbd_blker1@test.com", socialId = "fbd_s1")
            val blocker2 = createAndSaveMember(memberId = "fbd_blker2", email = "fbd_blker2@test.com", socialId = "fbd_s2")
            val blocked1 = createAndSaveMember(memberId = "fbd_blked1", email = "fbd_blked1@test.com", socialId = "fbd_s3")
            val blocked2 = createAndSaveMember(memberId = "fbd_blked2", email = "fbd_blked2@test.com", socialId = "fbd_s4")
            val blocked3 = createAndSaveMember(memberId = "fbd_blked3", email = "fbd_blked3@test.com", socialId = "fbd_s5")

            memberBlockRepository.save(createMemberBlock(blockerId = blocker1.id, blockedId = blocked1.id))
            memberBlockRepository.save(createMemberBlock(blockerId = blocker1.id, blockedId = blocked2.id))
            memberBlockRepository.save(createMemberBlock(blockerId = blocker2.id, blockedId = blocked3.id))

            val blockedIds = memberBlockRepository.findBlockedIdsByBlockerId(blocker1.id)

            assertThat(blockedIds).hasSize(2)
            assertThat(blockedIds).containsExactlyInAnyOrder(blocked1.id, blocked2.id)
        }

        @Test
        @DisplayName("차단한 사람이 없으면 빈 리스트를 반환한다")
        fun findBlockedIdsByBlockerIdWhenEmpty() {
            val blockedIds = memberBlockRepository.findBlockedIdsByBlockerId(-1L)

            assertThat(blockedIds).isEmpty()
        }
    }

    @Nested
    @DisplayName("findBlockerIdsByBlockedId 메서드")
    inner class FindBlockerIdsByBlockedId {

        @Test
        @DisplayName("피차단자 ID로 차단자 ID 목록을 반환한다")
        fun findBlockerIdsByBlockedId() {
            val blocker1 = createAndSaveMember(memberId = "fbr_blker1", email = "fbr_blker1@test.com", socialId = "fbr_s1")
            val blocker2 = createAndSaveMember(memberId = "fbr_blker2", email = "fbr_blker2@test.com", socialId = "fbr_s2")
            val blocker3 = createAndSaveMember(memberId = "fbr_blker3", email = "fbr_blker3@test.com", socialId = "fbr_s3")
            val blocked1 = createAndSaveMember(memberId = "fbr_blked1", email = "fbr_blked1@test.com", socialId = "fbr_s4")
            val blocked2 = createAndSaveMember(memberId = "fbr_blked2", email = "fbr_blked2@test.com", socialId = "fbr_s5")

            memberBlockRepository.save(createMemberBlock(blockerId = blocker1.id, blockedId = blocked1.id))
            memberBlockRepository.save(createMemberBlock(blockerId = blocker2.id, blockedId = blocked1.id))
            memberBlockRepository.save(createMemberBlock(blockerId = blocker3.id, blockedId = blocked2.id))

            val blockerIds = memberBlockRepository.findBlockerIdsByBlockedId(blocked1.id)

            assertThat(blockerIds).hasSize(2)
            assertThat(blockerIds).containsExactlyInAnyOrder(blocker1.id, blocker2.id)
        }

        @Test
        @DisplayName("차단당한 적이 없으면 빈 리스트를 반환한다")
        fun findBlockerIdsByBlockedIdWhenEmpty() {
            val blockerIds = memberBlockRepository.findBlockerIdsByBlockedId(-1L)

            assertThat(blockerIds).isEmpty()
        }
    }

    @Nested
    @DisplayName("countByBlockerId 메서드")
    inner class CountByBlockerId {

        @Test
        @DisplayName("차단자의 차단 수를 반환한다")
        fun countByBlockerId() {
            val blocker = createAndSaveMember(memberId = "ct_blker1", email = "ct_blker1@test.com", socialId = "ct_s1")
            val blocked1 = createAndSaveMember(memberId = "ct_blked1", email = "ct_blked1@test.com", socialId = "ct_s2")
            val blocked2 = createAndSaveMember(memberId = "ct_blked2", email = "ct_blked2@test.com", socialId = "ct_s3")
            val blocked3 = createAndSaveMember(memberId = "ct_blked3", email = "ct_blked3@test.com", socialId = "ct_s4")

            memberBlockRepository.save(createMemberBlock(blockerId = blocker.id, blockedId = blocked1.id))
            memberBlockRepository.save(createMemberBlock(blockerId = blocker.id, blockedId = blocked2.id))
            memberBlockRepository.save(createMemberBlock(blockerId = blocker.id, blockedId = blocked3.id))

            val count = memberBlockRepository.countByBlockerId(blocker.id)

            assertThat(count).isEqualTo(3)
        }

        @Test
        @DisplayName("차단한 사람이 없으면 0을 반환한다")
        fun countByBlockerIdWhenEmpty() {
            val count = memberBlockRepository.countByBlockerId(-1L)

            assertThat(count).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    inner class Delete {

        @Test
        @DisplayName("멤버 차단을 삭제하면 조회되지 않는다")
        fun deleteMemberBlock() {
            val blocker = createAndSaveMember(memberId = "dl_blker1", email = "dl_blker1@test.com", socialId = "dl_s1")
            val blocked = createAndSaveMember(memberId = "dl_blked1", email = "dl_blked1@test.com", socialId = "dl_s2")

            val savedMemberBlock = memberBlockRepository.save(createMemberBlock(blockerId = blocker.id, blockedId = blocked.id))

            memberBlockRepository.delete(savedMemberBlock)

            val foundMemberBlock = memberBlockRepository.findById(savedMemberBlock.id!!)
            assertThat(foundMemberBlock).isNull()
            flushAndClear()
            assertThat(memberBlockRepository.findById(savedMemberBlock.id!!)).isNull()
        }
    }

    @Nested
    @DisplayName("deleteByBlockerIdAndBlockedId 메서드")
    inner class DeleteByBlockerIdAndBlockedId {

        @Test
        @DisplayName("차단자와 피차단자 ID로 삭제하면 조회되지 않는다")
        fun deleteByBlockerIdAndBlockedId() {
            val blocker = createAndSaveMember(memberId = "dbb_blker1", email = "dbb_blker1@test.com", socialId = "dbb_s1")
            val blocked = createAndSaveMember(memberId = "dbb_blked1", email = "dbb_blked1@test.com", socialId = "dbb_s2")

            memberBlockRepository.save(createMemberBlock(blockerId = blocker.id, blockedId = blocked.id))

            memberBlockRepository.deleteByBlockerIdAndBlockedId(blocker.id, blocked.id)

            val foundMemberBlock = memberBlockRepository.findByBlockerIdAndBlockedId(blocker.id, blocked.id)
            assertThat(foundMemberBlock).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 조합으로 삭제해도 예외가 발생하지 않는다")
        fun deleteByNonExistingBlockerIdAndBlockedId() {
            memberBlockRepository.deleteByBlockerIdAndBlockedId(-1L, -2L)
        }
    }
}
