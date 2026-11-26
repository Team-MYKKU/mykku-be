package com.example.mykku.member.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.entity.Member
import com.example.mykku.member.domain.vo.MemberPk
import com.example.mykku.member.domain.vo.SocialProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

@DisplayName("MemberRepositoryAdapter 통합 테스트")
class MemberRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Nested
    @DisplayName("save 메서드는")
    inner class SaveMethod {

        @Test
        @DisplayName("새로운 회원을 저장하고 반환한다")
        fun saveNewMember() {
            val member = Member.createSocialMember(
                profileImage = "profile.png",
                provider = SocialProvider.GOOGLE,
                socialId = "socialId123",
                email = "test@example.com"
            )

            val savedMember = memberRepository.save(member)

            assertThat(savedMember.id.value).isNotEqualTo(0L)
            assertThat(savedMember.email).isEqualTo(member.email)
            assertThat(savedMember.provider).isEqualTo(member.provider)
            assertThat(savedMember.socialId).isEqualTo(member.socialId)
            assertThat(savedMember.profileImage).isEqualTo(member.profileImage)
        }

        @Test
        @DisplayName("이메일 회원을 저장하고 반환한다")
        fun saveEmailMember() {
            val member = Member.createEmailMember(
                email = "email@example.com",
                password = "encodedPassword123",
                profileImage = "profile.png"
            )

            val savedMember = memberRepository.save(member)

            assertThat(savedMember.id.value).isNotEqualTo(0L)
            assertThat(savedMember.memberId).isEqualTo(member.memberId)
            assertThat(savedMember.email).isEqualTo(member.email)
            assertThat(savedMember.password).isEqualTo(member.password)
            assertThat(savedMember.provider).isEqualTo(SocialProvider.EMAIL)
            assertThat(savedMember.emailVerified).isFalse()
        }

        @Test
        @DisplayName("기존 회원 정보를 업데이트하고 반환한다")
        fun updateExistingMember() {
            val savedMember = saveSocialMember()

            savedMember.updateProfile("newNick", "newProfile.png")
            val updatedMember = memberRepository.save(savedMember)
            flushAndClear()

            val reloadedMember = memberRepository.findById(savedMember.id)!!
            assertThat(updatedMember.id.value).isEqualTo(savedMember.id.value)
            assertThat(updatedMember.nickname).isEqualTo("newNick")
            assertThat(updatedMember.profileImage).isEqualTo("newProfile.png")
            assertThat(reloadedMember.nickname).isEqualTo("newNick")
            assertThat(reloadedMember.profileImage).isEqualTo("newProfile.png")
            assertThat(reloadedMember.socialId).isEqualTo("socialId123")
        }

        private fun saveSocialMember(): Member {
            val member = Member.createSocialMember(
                profileImage = "profile.png",
                provider = SocialProvider.GOOGLE,
                socialId = "socialId123",
                email = "test@example.com"
            )
            return memberRepository.save(member)
        }
    }

    @Nested
    @DisplayName("findById 메서드는")
    inner class FindByIdMethod {

        @Test
        @DisplayName("존재하는 회원 ID로 조회하면 회원을 반환한다")
        fun findExistingMemberById() {
            val member = Member.createSocialMember(
                profileImage = "profile.png",
                provider = SocialProvider.GOOGLE,
                socialId = "socialId123",
                email = "test@example.com"
            )
            val savedMember = memberRepository.save(member)

            val foundMember = memberRepository.findById(MemberPk.of(savedMember.id.value))

            assertThat(foundMember).isNotNull
            assertThat(foundMember!!.id.value).isEqualTo(savedMember.id.value)
            assertThat(foundMember.email).isEqualTo(savedMember.email)
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID로 조회하면 null을 반환한다")
        fun findNonExistingMemberById() {
            val foundMember = memberRepository.findById(MemberPk.of(999999L))

            assertThat(foundMember).isNull()
        }
    }

    @Nested
    @DisplayName("findByProviderAndSocialId 메서드는")
    inner class FindByProviderAndSocialIdMethod {

        @Test
        @DisplayName("존재하는 provider와 socialId로 조회하면 회원을 반환한다")
        fun findExistingMember() {
            val member = Member.createSocialMember(
                profileImage = "profile.png",
                provider = SocialProvider.GOOGLE,
                socialId = "google123",
                email = "test@example.com"
            )
            memberRepository.save(member)

            val foundMember = memberRepository.findByProviderAndSocialId(SocialProvider.GOOGLE, "google123")

            assertThat(foundMember).isNotNull
            assertThat(foundMember!!.provider).isEqualTo(SocialProvider.GOOGLE)
            assertThat(foundMember.socialId).isEqualTo("google123")
        }

        @Test
        @DisplayName("존재하지 않는 조합으로 조회하면 null을 반환한다")
        fun findNonExistingMember() {
            val foundMember = memberRepository.findByProviderAndSocialId(SocialProvider.GOOGLE, "nonExisting")

            assertThat(foundMember).isNull()
        }
    }

    @Nested
    @DisplayName("existsByNickname 메서드는")
    inner class ExistsByNicknameMethod {

        @Test
        @DisplayName("존재하는 닉네임이면 true를 반환한다")
        fun existingNicknameReturnsTrue() {
            createAndSaveMember(nickname = "uniqueNick")

            val exists = memberRepository.existsByNickname("uniqueNick")

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 닉네임이면 false를 반환한다")
        fun nonExistingNicknameReturnsFalse() {
            val exists = memberRepository.existsByNickname("nonExistingNickname")

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("existsByEmail 메서드는")
    inner class ExistsByEmailMethod {

        @Test
        @DisplayName("존재하는 이메일이면 true를 반환한다")
        fun existingEmailReturnsTrue() {
            createAndSaveMember(email = "unique@example.com")

            val exists = memberRepository.existsByEmail("unique@example.com")

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 false를 반환한다")
        fun nonExistingEmailReturnsFalse() {
            val exists = memberRepository.existsByEmail("nonexisting@example.com")

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("findByEmail 메서드는")
    inner class FindByEmailMethod {

        @Test
        @DisplayName("존재하는 이메일로 조회하면 회원을 반환한다")
        fun findExistingMemberByEmail() {
            createAndSaveMember(email = "search@example.com", nickname = "testNick")

            val foundMember = memberRepository.findByEmail("search@example.com")

            assertThat(foundMember).isNotNull
            assertThat(foundMember!!.email).isEqualTo("search@example.com")
            assertThat(foundMember.nickname).isEqualTo("testNick")
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회하면 null을 반환한다")
        fun findNonExistingMemberByEmail() {
            val foundMember = memberRepository.findByEmail("notfound@example.com")

            assertThat(foundMember).isNull()
        }
    }

    @Nested
    @DisplayName("existsByMemberId 메서드는")
    inner class ExistsByMemberIdMethod {

        @Test
        @DisplayName("존재하는 memberId이면 true를 반환한다")
        fun existingMemberIdReturnsTrue() {
            createAndSaveMember(memberId = "uniqueMemId")

            val exists = memberRepository.existsByMemberId("uniqueMemId")

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 memberId이면 false를 반환한다")
        fun nonExistingMemberIdReturnsFalse() {
            val exists = memberRepository.existsByMemberId("nonExistingMemberId")

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("existsByMemberIdExcludingId 메서드는")
    inner class ExistsByMemberIdExcludingIdMethod {

        @Test
        @DisplayName("다른 회원이 사용 중인 memberId이면 true를 반환한다")
        fun memberIdOfOtherMemberReturnsTrue() {
            createAndSaveMember(memberId = "takenid")
            val requester = createAndSaveMember(memberId = "requester")

            val exists = memberRepository.existsByMemberIdExcludingId("takenid", MemberPk.of(requester.id))

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("제외한 회원만 사용 중인 memberId이면 false를 반환한다")
        fun memberIdOfExcludedMemberOnlyReturnsFalse() {
            val owner = createAndSaveMember(memberId = "ownedid")
            createAndSaveMember(memberId = "otherid")

            val exists = memberRepository.existsByMemberIdExcludingId("ownedid", MemberPk.of(owner.id))

            assertThat(exists).isFalse()
        }

        @Test
        @DisplayName("아무도 사용하지 않는 memberId이면 false를 반환한다")
        fun unusedMemberIdReturnsFalse() {
            val requester = createAndSaveMember(memberId = "requester")
            createAndSaveMember(memberId = "otherid")

            val exists = memberRepository.existsByMemberIdExcludingId("freeid", MemberPk.of(requester.id))

            assertThat(exists).isFalse()
        }

        @Test
        @DisplayName("다른 회원이 대소문자만 다르게 사용 중인 memberId이면 true를 반환한다")
        fun memberIdOfOtherMemberInDifferentCaseReturnsTrue() {
            createAndSaveMember(memberId = "caseowner")
            val requester = createAndSaveMember(memberId = "requester")

            val exists = memberRepository.existsByMemberIdExcludingId("CASEOWNER", MemberPk.of(requester.id))

            assertThat(exists).isTrue()
        }
    }

    @Nested
    @DisplayName("findByMemberId 메서드는")
    inner class FindByMemberIdMethod {

        @Test
        @DisplayName("존재하는 memberId로 조회하면 회원을 반환한다")
        fun findExistingMemberByMemberId() {
            createAndSaveMember(memberId = "searchMemId", nickname = "testNick")

            val foundMember = memberRepository.findByMemberId("searchMemId")

            assertThat(foundMember).isNotNull
            assertThat(foundMember!!.memberId).isEqualTo("searchMemId")
            assertThat(foundMember.nickname).isEqualTo("testNick")
        }

        @Test
        @DisplayName("존재하지 않는 memberId로 조회하면 null을 반환한다")
        fun findNonExistingMemberByMemberId() {
            val foundMember = memberRepository.findByMemberId("notFoundMemberId")

            assertThat(foundMember).isNull()
        }
    }

    @Nested
    @DisplayName("search 메서드는")
    inner class SearchMethod {

        @Test
        @DisplayName("키워드가 null이면 전체 회원을 최근 가입한 순서로 반환한다")
        fun nullKeywordReturnsAllMembersNewestFirst() {
            val first = createAndSaveMember(memberId = "firstmember")
            val second = createAndSaveMember(memberId = "secondmember")
            val third = createAndSaveMember(memberId = "thirdmember")

            val result = memberRepository.search(null, PageRequest.of(0, 10))

            assertThat(result.content.map { it.id.value }).containsExactly(third.id, second.id, first.id)
        }

        @Test
        @DisplayName("키워드가 공백뿐이면 전체 회원을 최근 가입한 순서로 반환한다")
        fun blankKeywordReturnsAllMembersNewestFirst() {
            val first = createAndSaveMember(memberId = "firstmember")
            val second = createAndSaveMember(memberId = "secondmember")
            val third = createAndSaveMember(memberId = "thirdmember")

            val result = memberRepository.search("   ", PageRequest.of(0, 10))

            assertThat(result.content.map { it.id.value }).containsExactly(third.id, second.id, first.id)
        }

        @Test
        @DisplayName("키워드 앞뒤 공백을 제거하고 memberId에 키워드가 포함된 회원을 반환한다")
        fun trimmedKeywordMatchesMemberId() {
            val target = createAndSaveMember(memberId = "alphamember")
            createAndSaveMember(memberId = "betamember")

            val result = memberRepository.search("  alpha ", PageRequest.of(0, 10))

            assertThat(result.content.map { it.id.value }).containsExactly(target.id)
        }

        @Test
        @DisplayName("키워드와 일치하는 회원도 최근 가입한 순서로 반환한다")
        fun keywordMatchesReturnNewestFirst() {
            val older = createAndSaveMember(memberId = "alphaone")
            val newer = createAndSaveMember(memberId = "alphatwo")
            createAndSaveMember(memberId = "betaone")

            val result = memberRepository.search("alpha", PageRequest.of(0, 10))

            assertThat(result.content.map { it.id.value }).containsExactly(newer.id, older.id)
        }

        @Test
        @DisplayName("닉네임에 키워드가 포함된 회원을 반환한다")
        fun keywordMatchesNickname() {
            val target = createAndSaveMember(memberId = "nicktarget", nickname = "gammaNick")
            createAndSaveMember(memberId = "nickother", nickname = "deltaNick")

            val result = memberRepository.search("gamma", PageRequest.of(0, 10))

            assertThat(result.content.map { it.id.value }).containsExactly(target.id)
        }

        @Test
        @DisplayName("이메일에 키워드가 포함된 회원을 반환한다")
        fun keywordMatchesEmail() {
            val target = createAndSaveMember(memberId = "mailtarget", email = "omega@example.com")
            createAndSaveMember(memberId = "mailother", email = "sigma@example.com")

            val result = memberRepository.search("omega", PageRequest.of(0, 10))

            assertThat(result.content.map { it.id.value }).containsExactly(target.id)
        }

        @Test
        @DisplayName("키워드와 일치하는 회원이 없으면 빈 페이지를 반환한다")
        fun unmatchedKeywordReturnsEmptyPage() {
            createAndSaveMember(memberId = "lonelymember")

            val result = memberRepository.search("nomatch", PageRequest.of(0, 10))

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }
    }
}
