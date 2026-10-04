package com.example.mykku.dailymessage.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.dailymessage.application.port.output.DailyMessageCommentRepository
import com.example.mykku.dailymessage.application.port.output.DailyMessageRepository
import com.example.mykku.dailymessage.domain.entity.DailyMessage
import com.example.mykku.dailymessage.domain.entity.DailyMessageComment
import com.example.mykku.dailymessage.domain.vo.DailyMessageCommentId
import com.example.mykku.dailymessage.domain.vo.DailyMessageId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate

@DisplayName("DailyMessageCommentRepositoryAdapter 통합 테스트")
class DailyMessageCommentRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var dailyMessageCommentRepository: DailyMessageCommentRepository

    @Autowired
    private lateinit var dailyMessageRepository: DailyMessageRepository

    private lateinit var savedDailyMessage: DailyMessage
    private lateinit var savedMember: com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity

    @BeforeEach
    fun setUp() {
        savedMember = createAndSaveMember(
            memberId = "testMember",
            nickname = "테스트유저",
            email = "test@example.com"
        )
        savedDailyMessage = dailyMessageRepository.save(
            DailyMessage.create(
                title = "테스트 일상 메시지",
                content = "테스트 내용",
                date = LocalDate.of(2025, 1, 26)
            )
        )
    }

    @Nested
    @DisplayName("save 메서드")
    inner class Save {

        @Test
        @DisplayName("댓글을 저장하면 ID가 부여된다")
        fun `저장 - 정상 케이스`() {
            val comment = createComment()

            val savedComment = dailyMessageCommentRepository.save(comment)

            assertThat(savedComment.id.value).isNotEqualTo(0L)
            assertThat(savedComment.dailyMessageId).isEqualTo(savedDailyMessage.id.value)
            assertThat(savedComment.memberId).isEqualTo(savedMember.id)
            assertThat(savedComment.content).isEqualTo(comment.content)
        }

        @Test
        @DisplayName("대댓글을 저장할 수 있다")
        fun `저장 - 대댓글`() {
            val parentComment = dailyMessageCommentRepository.save(createComment())
            val replyComment = createComment(parentCommentId = parentComment.id.value, content = "대댓글 내용")

            val savedReply = dailyMessageCommentRepository.save(replyComment)

            assertThat(savedReply.id.value).isNotEqualTo(0L)
            assertThat(savedReply.parentCommentId).isEqualTo(parentComment.id.value)
        }

        @Test
        @DisplayName("댓글 내용을 수정하여 저장할 수 있다")
        fun `저장 - 수정 케이스`() {
            val comment = dailyMessageCommentRepository.save(createComment())
            val updatedComment = comment.updateContent("수정된 내용")

            val result = dailyMessageCommentRepository.save(updatedComment)

            assertThat(result.id).isEqualTo(comment.id)
            assertThat(result.content).isEqualTo("수정된 내용")
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("ID로 댓글을 조회한다")
        fun `조회 - 정상 케이스`() {
            val comment = dailyMessageCommentRepository.save(createComment())

            val foundComment = dailyMessageCommentRepository.findById(comment.id)

            assertThat(foundComment).isNotNull
            assertThat(foundComment!!.id).isEqualTo(comment.id)
            assertThat(foundComment.content).isEqualTo(comment.content)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun `조회 - 존재하지 않는 ID`() {
            val foundComment = dailyMessageCommentRepository.findById(DailyMessageCommentId(999999L))

            assertThat(foundComment).isNull()
        }
    }

    @Nested
    @DisplayName("findByIdAndDailyMessageId 메서드")
    inner class FindByIdAndDailyMessageId {

        @Test
        @DisplayName("댓글 ID와 일상 메시지 ID로 댓글을 조회한다")
        fun `조회 - 정상 케이스`() {
            val comment = dailyMessageCommentRepository.save(createComment())

            val foundComment = dailyMessageCommentRepository.findByIdAndDailyMessageId(
                commentId = comment.id,
                dailyMessageId = DailyMessageId(savedDailyMessage.id.value)
            )

            assertThat(foundComment).isNotNull
            assertThat(foundComment!!.id).isEqualTo(comment.id)
            assertThat(foundComment.dailyMessageId).isEqualTo(savedDailyMessage.id.value)
        }

        @Test
        @DisplayName("일상 메시지 ID가 다르면 null을 반환한다")
        fun `조회 - 일상 메시지 ID 불일치`() {
            val comment = dailyMessageCommentRepository.save(createComment())

            val foundComment = dailyMessageCommentRepository.findByIdAndDailyMessageId(
                commentId = comment.id,
                dailyMessageId = DailyMessageId(999999L)
            )

            assertThat(foundComment).isNull()
        }

        @Test
        @DisplayName("댓글 ID가 존재하지 않으면 null을 반환한다")
        fun `조회 - 댓글 ID 존재하지 않음`() {
            val foundComment = dailyMessageCommentRepository.findByIdAndDailyMessageId(
                commentId = DailyMessageCommentId(999999L),
                dailyMessageId = DailyMessageId(savedDailyMessage.id.value)
            )

            assertThat(foundComment).isNull()
        }
    }

    @Nested
    @DisplayName("findByDailyMessageIdAndParentCommentIsNull 메서드")
    inner class FindByDailyMessageIdAndParentCommentIsNull {

        @Test
        @DisplayName("일상 메시지의 부모 댓글만 조회한다")
        fun `조회 - 부모 댓글만`() {
            val parentComment1 = dailyMessageCommentRepository.save(createComment(content = "부모댓글1"))
            val parentComment2 = dailyMessageCommentRepository.save(createComment(content = "부모댓글2"))
            dailyMessageCommentRepository.save(createComment(parentCommentId = parentComment1.id.value, content = "대댓글1"))
            dailyMessageCommentRepository.save(createComment(parentCommentId = parentComment2.id.value, content = "대댓글2"))

            val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
            val result = dailyMessageCommentRepository.findByDailyMessageIdAndParentCommentIsNull(
                dailyMessageId = DailyMessageId(savedDailyMessage.id.value),
                pageable = pageable
            )

            assertThat(result.content).hasSize(2)
            assertThat(result.content.all { it.parentCommentId == null }).isTrue()
        }

        @Test
        @DisplayName("페이지 크기만큼 조회한다")
        fun `조회 - 페이지 크기 제한`() {
            for (i in 1..5) {
                dailyMessageCommentRepository.save(createComment(content = "부모댓글$i"))
            }

            val pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"))
            val result = dailyMessageCommentRepository.findByDailyMessageIdAndParentCommentIsNull(
                dailyMessageId = DailyMessageId(savedDailyMessage.id.value),
                pageable = pageable
            )

            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(5)
            assertThat(result.totalPages).isEqualTo(3)
        }

        @Test
        @DisplayName("다른 일상 메시지의 댓글은 조회되지 않는다")
        fun `조회 - 다른 일상 메시지 댓글 제외`() {
            dailyMessageCommentRepository.save(createComment(content = "현재 메시지 댓글"))

            val otherDailyMessage = dailyMessageRepository.save(
                DailyMessage.create(
                    title = "다른 일상 메시지",
                    content = "다른 내용",
                    date = LocalDate.of(2025, 1, 27)
                )
            )
            dailyMessageCommentRepository.save(
                DailyMessageComment.create(
                    dailyMessageId = otherDailyMessage.id.value,
                    memberId = savedMember.id,
                    content = "다른 메시지 댓글"
                )
            )

            val pageable = PageRequest.of(0, 10)
            val result = dailyMessageCommentRepository.findByDailyMessageIdAndParentCommentIsNull(
                dailyMessageId = DailyMessageId(savedDailyMessage.id.value),
                pageable = pageable
            )

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].content).isEqualTo("현재 메시지 댓글")
        }

        @Test
        @DisplayName("댓글이 없으면 빈 페이지를 반환한다")
        fun `조회 - 결과 없음`() {
            val pageable = PageRequest.of(0, 10)
            val result = dailyMessageCommentRepository.findByDailyMessageIdAndParentCommentIsNull(
                dailyMessageId = DailyMessageId(savedDailyMessage.id.value),
                pageable = pageable
            )

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("findByParentCommentIds 메서드")
    inner class FindByParentCommentIds {

        @Test
        @DisplayName("답글을 부모 댓글 순, 같은 부모 안에서는 작성 순으로 반환한다")
        fun `답글 정렬 - 부모 순 그리고 작성 순`() {
            val parentA = dailyMessageCommentRepository.save(createComment(content = "부모A"))
            val parentB = dailyMessageCommentRepository.save(createComment(content = "부모B"))
            dailyMessageCommentRepository.save(createComment(content = "B-1", parentCommentId = parentB.id.value))
            dailyMessageCommentRepository.save(createComment(content = "A-1", parentCommentId = parentA.id.value))
            dailyMessageCommentRepository.save(createComment(content = "A-2", parentCommentId = parentA.id.value))
            dailyMessageCommentRepository.save(createComment(content = "B-2", parentCommentId = parentB.id.value))

            val replies = dailyMessageCommentRepository.findByParentCommentIds(
                listOf(parentB.id.value, parentA.id.value)
            )

            assertThat(replies.map { it.content }).containsExactly("A-1", "A-2", "B-1", "B-2")
        }

        @Test
        @DisplayName("여러 부모 댓글의 대댓글을 조회한다")
        fun `조회 - 정상 케이스`() {
            val parent1 = dailyMessageCommentRepository.save(createComment(content = "부모1"))
            val parent2 = dailyMessageCommentRepository.save(createComment(content = "부모2"))
            dailyMessageCommentRepository.save(createComment(parentCommentId = parent1.id.value, content = "대댓글1-1"))
            dailyMessageCommentRepository.save(createComment(parentCommentId = parent1.id.value, content = "대댓글1-2"))
            dailyMessageCommentRepository.save(createComment(parentCommentId = parent2.id.value, content = "대댓글2-1"))

            val result = dailyMessageCommentRepository.findByParentCommentIds(
                listOf(parent1.id.value, parent2.id.value)
            )

            assertThat(result).hasSize(3)
        }

        @Test
        @DisplayName("특정 부모 댓글의 대댓글만 조회한다")
        fun `조회 - 특정 부모만`() {
            val parent1 = dailyMessageCommentRepository.save(createComment(content = "부모1"))
            val parent2 = dailyMessageCommentRepository.save(createComment(content = "부모2"))
            dailyMessageCommentRepository.save(createComment(parentCommentId = parent1.id.value, content = "대댓글1-1"))
            dailyMessageCommentRepository.save(createComment(parentCommentId = parent1.id.value, content = "대댓글1-2"))
            dailyMessageCommentRepository.save(createComment(parentCommentId = parent2.id.value, content = "대댓글2-1"))

            val result = dailyMessageCommentRepository.findByParentCommentIds(listOf(parent1.id.value))

            assertThat(result).hasSize(2)
            assertThat(result.all { it.parentCommentId == parent1.id.value }).isTrue()
        }

        @Test
        @DisplayName("빈 ID 목록으로 조회하면 빈 리스트를 반환한다")
        fun `조회 - 빈 ID 목록`() {
            val result = dailyMessageCommentRepository.findByParentCommentIds(emptyList())

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("대댓글이 없는 부모 ID로 조회하면 빈 리스트를 반환한다")
        fun `조회 - 대댓글 없음`() {
            val parent = dailyMessageCommentRepository.save(createComment(content = "부모"))

            val result = dailyMessageCommentRepository.findByParentCommentIds(listOf(parent.id.value))

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    inner class Delete {

        @Test
        @DisplayName("댓글을 삭제한다")
        fun `삭제 - 정상 케이스`() {
            val comment = dailyMessageCommentRepository.save(createComment())

            dailyMessageCommentRepository.delete(comment)

            val foundComment = dailyMessageCommentRepository.findById(comment.id)
            assertThat(foundComment).isNull()
        }

        @Test
        @DisplayName("부모 댓글 삭제 시 대댓글은 유지된다")
        fun `삭제 - 부모 삭제 시 대댓글 유지`() {
            val parent = dailyMessageCommentRepository.save(createComment(content = "부모"))
            val reply = dailyMessageCommentRepository.save(
                createComment(parentCommentId = parent.id.value, content = "대댓글")
            )

            dailyMessageCommentRepository.delete(parent)

            val foundParent = dailyMessageCommentRepository.findById(parent.id)
            val foundReply = dailyMessageCommentRepository.findById(reply.id)
            assertThat(foundParent).isNull()
            assertThat(foundReply).isNotNull
        }
    }

    private fun createComment(
        content: String = "테스트 댓글",
        parentCommentId: Long? = null
    ): DailyMessageComment {
        return DailyMessageComment.create(
            dailyMessageId = savedDailyMessage.id.value,
            memberId = savedMember.id,
            content = content,
            parentCommentId = parentCommentId
        )
    }
}
