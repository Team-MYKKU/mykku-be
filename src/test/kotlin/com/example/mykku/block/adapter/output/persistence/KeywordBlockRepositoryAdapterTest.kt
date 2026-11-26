package com.example.mykku.block.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.block.application.port.output.KeywordBlockRepository
import com.example.mykku.block.domain.entity.KeywordBlock
import com.example.mykku.block.domain.vo.KeywordBlockId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

@DisplayName("KeywordBlockRepository 통합 테스트")
class KeywordBlockRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var keywordBlockRepository: KeywordBlockRepository

    private fun createKeywordBlock(
        memberId: Long,
        keyword: String = "차단키워드"
    ): KeywordBlock {
        return KeywordBlock.create(memberId = memberId, keyword = keyword)
    }

    @Nested
    @DisplayName("save 메서드")
    inner class Save {

        @Test
        @DisplayName("새로운 키워드 차단을 저장하면 ID가 생성된다")
        fun saveNewKeywordBlock() {
            val member = createAndSaveMember(email = "sv_m1@test.com", socialId = "sv_s1")
            val keywordBlock = createKeywordBlock(memberId = member.id)

            val savedKeywordBlock = keywordBlockRepository.save(keywordBlock)

            assertThat(savedKeywordBlock.id).isNotNull
            assertThat(savedKeywordBlock.id!!.value).isGreaterThan(0)
            assertThat(savedKeywordBlock.memberId).isEqualTo(keywordBlock.memberId)
            assertThat(savedKeywordBlock.keyword).isEqualTo(keywordBlock.keyword)
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("존재하는 ID로 조회하면 키워드 차단을 반환한다")
        fun findByExistingId() {
            val member = createAndSaveMember(email = "fbi_m1@test.com", socialId = "fbi_s1")
            val savedKeywordBlock = keywordBlockRepository.save(createKeywordBlock(memberId = member.id))

            val foundKeywordBlock = keywordBlockRepository.findById(savedKeywordBlock.id!!)

            assertThat(foundKeywordBlock).isNotNull
            assertThat(foundKeywordBlock!!.id).isEqualTo(savedKeywordBlock.id)
            assertThat(foundKeywordBlock.memberId).isEqualTo(savedKeywordBlock.memberId)
            assertThat(foundKeywordBlock.keyword).isEqualTo(savedKeywordBlock.keyword)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun findByNonExistingId() {
            val foundKeywordBlock = keywordBlockRepository.findById(KeywordBlockId(999999L))

            assertThat(foundKeywordBlock).isNull()
        }
    }

    @Nested
    @DisplayName("findByMemberIdAndKeyword 메서드")
    inner class FindByMemberIdAndKeyword {

        @Test
        @DisplayName("멤버 ID와 키워드로 조회하면 키워드 차단을 반환한다")
        fun findByMemberIdAndKeyword() {
            val member = createAndSaveMember(email = "fmk_m1@test.com", socialId = "fmk_s1")
            keywordBlockRepository.save(createKeywordBlock(memberId = member.id, keyword = "스포일러"))

            val foundKeywordBlock = keywordBlockRepository.findByMemberIdAndKeyword(member.id, "스포일러")

            assertThat(foundKeywordBlock).isNotNull
            assertThat(foundKeywordBlock!!.memberId).isEqualTo(member.id)
            assertThat(foundKeywordBlock.keyword).isEqualTo("스포일러")
        }

        @Test
        @DisplayName("존재하지 않는 조합으로 조회하면 null을 반환한다")
        fun findByNonExistingMemberIdAndKeyword() {
            val foundKeywordBlock = keywordBlockRepository.findByMemberIdAndKeyword(999999L, "keyword")

            assertThat(foundKeywordBlock).isNull()
        }
    }

    @Nested
    @DisplayName("existsByMemberIdAndKeyword 메서드")
    inner class ExistsByMemberIdAndKeyword {

        @Test
        @DisplayName("키워드 차단이 존재하면 true를 반환한다")
        fun existsByMemberIdAndKeyword() {
            val member = createAndSaveMember(email = "ex_m1@test.com", socialId = "ex_s1")
            keywordBlockRepository.save(createKeywordBlock(memberId = member.id, keyword = "스포일러"))

            val exists = keywordBlockRepository.existsByMemberIdAndKeyword(member.id, "스포일러")

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("키워드 차단이 존재하지 않으면 false를 반환한다")
        fun notExistsByMemberIdAndKeyword() {
            val exists = keywordBlockRepository.existsByMemberIdAndKeyword(999999L, "스포일러")

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("findAllByMemberId 메서드")
    inner class FindAllByMemberId {

        @Test
        @DisplayName("멤버 ID로 조회하면 페이징된 키워드 차단 목록을 반환한다")
        fun findAllByMemberId() {
            val member1 = createAndSaveMember(memberId = "fa_m1", email = "fa_m1@test.com", socialId = "fa_s1")
            val member2 = createAndSaveMember(memberId = "fa_m2", email = "fa_m2@test.com", socialId = "fa_s2")
            keywordBlockRepository.save(createKeywordBlock(memberId = member1.id, keyword = "키워드1"))
            keywordBlockRepository.save(createKeywordBlock(memberId = member1.id, keyword = "키워드2"))
            keywordBlockRepository.save(createKeywordBlock(memberId = member1.id, keyword = "키워드3"))
            keywordBlockRepository.save(createKeywordBlock(memberId = member2.id, keyword = "키워드1"))

            val pageable = PageRequest.of(0, 10)
            val page = keywordBlockRepository.findAllByMemberId(member1.id, pageable)

            assertThat(page.content).hasSize(3)
            assertThat(page.totalElements).isEqualTo(3)
            assertThat(page.content.map { it.keyword }).containsExactlyInAnyOrder("키워드1", "키워드2", "키워드3")
        }

        @Test
        @DisplayName("키워드 차단이 없으면 빈 페이지를 반환한다")
        fun findAllByMemberIdWhenEmpty() {
            val pageable = PageRequest.of(0, 10)
            val page = keywordBlockRepository.findAllByMemberId(999999L, pageable)

            assertThat(page.content).isEmpty()
            assertThat(page.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("페이지 크기에 맞게 결과를 반환한다")
        fun findAllByMemberIdWithPaging() {
            val member = createAndSaveMember(email = "pg_m1@test.com", socialId = "pg_s1")
            for (i in 1..5) {
                keywordBlockRepository.save(createKeywordBlock(memberId = member.id, keyword = "키워드$i"))
            }

            val pageable = PageRequest.of(0, 2)
            val page = keywordBlockRepository.findAllByMemberId(member.id, pageable)

            assertThat(page.content).hasSize(2)
            assertThat(page.totalElements).isEqualTo(5)
            assertThat(page.totalPages).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("findKeywordsByMemberId 메서드")
    inner class FindKeywordsByMemberId {

        @Test
        @DisplayName("멤버 ID로 차단된 키워드 목록을 반환한다")
        fun findKeywordsByMemberId() {
            val member1 = createAndSaveMember(memberId = "fk_m1", email = "fk_m1@test.com", socialId = "fk_s1")
            val member2 = createAndSaveMember(memberId = "fk_m2", email = "fk_m2@test.com", socialId = "fk_s2")
            keywordBlockRepository.save(createKeywordBlock(memberId = member1.id, keyword = "스포일러"))
            keywordBlockRepository.save(createKeywordBlock(memberId = member1.id, keyword = "광고"))
            keywordBlockRepository.save(createKeywordBlock(memberId = member2.id, keyword = "홍보"))

            val keywords = keywordBlockRepository.findKeywordsByMemberId(member1.id)

            assertThat(keywords).hasSize(2)
            assertThat(keywords).containsExactlyInAnyOrder("스포일러", "광고")
        }

        @Test
        @DisplayName("차단된 키워드가 없으면 빈 리스트를 반환한다")
        fun findKeywordsByMemberIdWhenEmpty() {
            val keywords = keywordBlockRepository.findKeywordsByMemberId(999999L)

            assertThat(keywords).isEmpty()
        }
    }

    @Nested
    @DisplayName("countByMemberId 메서드")
    inner class CountByMemberId {

        @Test
        @DisplayName("멤버의 키워드 차단 수를 반환한다")
        fun countByMemberId() {
            val member = createAndSaveMember(email = "ct_m1@test.com", socialId = "ct_s1")
            keywordBlockRepository.save(createKeywordBlock(memberId = member.id, keyword = "키워드1"))
            keywordBlockRepository.save(createKeywordBlock(memberId = member.id, keyword = "키워드2"))
            keywordBlockRepository.save(createKeywordBlock(memberId = member.id, keyword = "키워드3"))

            val count = keywordBlockRepository.countByMemberId(member.id)

            assertThat(count).isEqualTo(3)
        }

        @Test
        @DisplayName("차단된 키워드가 없으면 0을 반환한다")
        fun countByMemberIdWhenEmpty() {
            val count = keywordBlockRepository.countByMemberId(999999L)

            assertThat(count).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    inner class Delete {

        @Test
        @DisplayName("키워드 차단을 삭제하면 조회되지 않는다")
        fun deleteKeywordBlock() {
            val member = createAndSaveMember(email = "dl_m1@test.com", socialId = "dl_s1")
            val savedKeywordBlock = keywordBlockRepository.save(createKeywordBlock(memberId = member.id))

            keywordBlockRepository.delete(savedKeywordBlock)

            val foundKeywordBlock = keywordBlockRepository.findById(savedKeywordBlock.id!!)
            assertThat(foundKeywordBlock).isNull()
            flushAndClear()
            assertThat(keywordBlockRepository.findById(savedKeywordBlock.id!!)).isNull()
        }
    }

    @Nested
    @DisplayName("deleteByMemberIdAndKeyword 메서드")
    inner class DeleteByMemberIdAndKeyword {

        @Test
        @DisplayName("멤버 ID와 키워드로 삭제하면 조회되지 않는다")
        fun deleteByMemberIdAndKeyword() {
            val member = createAndSaveMember(email = "dk_m1@test.com", socialId = "dk_s1")
            keywordBlockRepository.save(createKeywordBlock(memberId = member.id, keyword = "스포일러"))

            keywordBlockRepository.deleteByMemberIdAndKeyword(member.id, "스포일러")

            val foundKeywordBlock = keywordBlockRepository.findByMemberIdAndKeyword(member.id, "스포일러")
            assertThat(foundKeywordBlock).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 조합으로 삭제해도 예외가 발생하지 않는다")
        fun deleteByNonExistingMemberIdAndKeyword() {
            keywordBlockRepository.deleteByMemberIdAndKeyword(999999L, "keyword")
        }
    }
}
