package com.example.mykku.comment.application.usecase

import com.example.mykku.comment.application.dto.CommentAuthorResult
import com.example.mykku.comment.application.dto.CommentLikeStats
import com.example.mykku.comment.application.dto.CommentSnapshot
import com.example.mykku.role.application.dto.RoleResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("CommentResultAssembler 단위 테스트")
class CommentResultAssemblerTest {

    @Mock
    private lateinit var commentAuthorResolver: CommentAuthorResolver

    @InjectMocks
    private lateinit var assembler: CommentResultAssembler

    private val now: LocalDateTime = LocalDateTime.of(2026, 10, 1, 12, 0, 0)

    private val alive = CommentAuthorResult("alive", "살아있는회원", "", RoleResult(1L, "덕담왕", "설명"))

    private fun snapshot(id: Long, memberId: Long?) = CommentSnapshot(id, "내용$id", memberId, now, now)

    @Test
    @DisplayName("작성자 memberId가 null인 댓글과 답글은 탈퇴한 회원 author를 가진다")
    fun `memberId null - withdrawn author`() {
        whenever(commentAuthorResolver.resolve(any())).thenReturn(emptyMap())

        val results = assembler.assemble(
            listOf(snapshot(1L, null)),
            mapOf(1L to listOf(snapshot(2L, null))),
            CommentLikeStats.NONE
        )

        assertThat(results.single().author).isEqualTo(CommentAuthorResult.withdrawn())
        assertThat(results.single().author.nickname).isEqualTo("탈퇴한 회원")
        assertThat(results.single().replies.single().author).isEqualTo(CommentAuthorResult.withdrawn())
    }

    @Test
    @DisplayName("회원 행이 없어 리졸버 결과에 없는 memberId도 탈퇴한 회원 author를 가진다")
    fun `missing member - withdrawn author`() {
        whenever(commentAuthorResolver.resolve(listOf(10L, 11L))).thenReturn(mapOf(10L to alive))

        val results = assembler.assemble(
            listOf(snapshot(1L, 10L), snapshot(2L, 11L)),
            emptyMap(),
            CommentLikeStats.NONE
        )

        assertThat(results[0].author).isEqualTo(alive)
        assertThat(results[1].author).isEqualTo(CommentAuthorResult.withdrawn())
    }

    @Test
    @DisplayName("replyCount는 replies 길이와 같고 답글은 입력 순서를 유지한다")
    fun `replies keep order and count`() {
        whenever(commentAuthorResolver.resolve(any())).thenReturn(mapOf(10L to alive))

        val results = assembler.assemble(
            listOf(snapshot(1L, 10L), snapshot(5L, 10L)),
            mapOf(1L to listOf(snapshot(3L, 10L), snapshot(2L, 10L))),
            CommentLikeStats.NONE
        )

        assertThat(results[0].replies.map { it.id }).containsExactly(3L, 2L)
        assertThat(results[0].replyCount).isEqualTo(2)
        assertThat(results[1].replies).isEmpty()
        assertThat(results[1].replyCount).isEqualTo(0)
    }

    @Test
    @DisplayName("좋아요 통계로 likeCount와 isLiked를 채운다")
    fun `like stats mapping`() {
        whenever(commentAuthorResolver.resolve(any())).thenReturn(mapOf(10L to alive))
        val likes = CommentLikeStats(mapOf(1L to 3, 2L to 1), setOf(2L))

        val result = assembler.assemble(
            listOf(snapshot(1L, 10L)),
            mapOf(1L to listOf(snapshot(2L, 10L))),
            likes
        ).single()

        assertThat(result.likeCount).isEqualTo(3)
        assertThat(result.isLiked).isFalse()
        assertThat(result.replies.single().likeCount).isEqualTo(1)
        assertThat(result.replies.single().isLiked).isTrue()
    }
}
