package com.example.mykku.comment.application.usecase

import com.example.mykku.comment.application.dto.CommentAuthorResult
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.entity.Member
import com.example.mykku.member.domain.vo.MemberPk
import com.example.mykku.member.domain.vo.SocialProvider
import com.example.mykku.role.application.dto.RoleResult
import com.example.mykku.role.application.port.output.RoleRepository
import com.example.mykku.role.domain.entity.Role
import com.example.mykku.role.domain.vo.RoleId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("CommentAuthorResolver 단위 테스트")
class CommentAuthorResolverTest {

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var roleRepository: RoleRepository

    @InjectMocks
    private lateinit var resolver: CommentAuthorResolver

    private val now: LocalDateTime = LocalDateTime.of(2026, 10, 1, 12, 0, 0)

    private fun member(id: Long, memberId: String?, nickname: String?, roleId: Long?): Member =
        Member.reconstitute(
            id = id,
            memberId = memberId,
            nickname = nickname,
            roleId = roleId,
            profileImage = "https://example.com/$id.jpg",
            provider = SocialProvider.GOOGLE,
            socialId = "social$id",
            email = "m$id@example.com",
            password = null,
            emailVerified = false,
            createdAt = now,
            updatedAt = now
        )

    @Test
    @DisplayName("대표 칭호가 있는 회원은 role을, 없는 회원은 null role을 가진다")
    fun `role mapping`() {
        whenever(memberRepository.findByIds(listOf(MemberPk.of(1L), MemberPk.of(2L))))
            .thenReturn(listOf(member(1L, "withrole", "칭호회원", 7L), member(2L, "norole", "일반회원", null)))
        whenever(roleRepository.findByIds(listOf(RoleId.of(7L))))
            .thenReturn(listOf(Role.reconstitute(RoleId.of(7L), "덕담왕", "설명", now, now)))

        val authors = resolver.resolve(listOf(1L, 2L, 1L))

        assertThat(authors[1L]).isEqualTo(
            CommentAuthorResult("withrole", "칭호회원", "https://example.com/1.jpg", RoleResult(7L, "덕담왕", "설명"))
        )
        assertThat(authors[2L]?.role).isNull()
    }

    @Test
    @DisplayName("칭호가 없으면 role 저장소를 조회하지 않고 닉네임이 없는 회원은 빈 문자열이다")
    fun `no roles and null nickname`() {
        whenever(memberRepository.findByIds(listOf(MemberPk.of(3L))))
            .thenReturn(listOf(member(3L, null, null, null)))

        val authors = resolver.resolve(listOf(3L))

        assertThat(authors[3L]?.nickname).isEqualTo("")
        assertThat(authors[3L]?.memberId).isNull()
        verify(roleRepository, never()).findByIds(any())
    }

    @Test
    @DisplayName("빈 입력이면 저장소를 호출하지 않는다")
    fun `empty input`() {
        assertThat(resolver.resolve(emptyList())).isEmpty()
        verifyNoInteractions(memberRepository, roleRepository)
    }
}
