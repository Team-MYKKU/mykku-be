package com.example.mykku.dailymessage.application.usecase

import com.example.mykku.achievement.application.event.ActivityEvent
import com.example.mykku.achievement.application.port.output.ActivityEventPublisher
import com.example.mykku.achievement.domain.vo.ActivityType
import com.example.mykku.dailymessage.application.dto.CreateCommentCommand
import com.example.mykku.dailymessage.application.port.output.DailyMessageCommentRepository
import com.example.mykku.dailymessage.application.port.output.DailyMessageRepository
import com.example.mykku.dailymessage.domain.entity.DailyMessage
import com.example.mykku.dailymessage.domain.entity.DailyMessageComment
import com.example.mykku.dailymessage.domain.vo.DailyMessageCommentId
import com.example.mykku.dailymessage.domain.vo.DailyMessageId
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.entity.Member
import com.example.mykku.member.domain.vo.MemberPk
import com.example.mykku.member.domain.vo.SocialProvider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("하루덕담 댓글 작성 활동 이벤트 발행 단위 테스트")
class CreateCommentActivityPublishTest {

    @Mock
    private lateinit var dailyMessageRepository: DailyMessageRepository

    @Mock
    private lateinit var dailyMessageCommentRepository: DailyMessageCommentRepository

    @Mock
    private lateinit var activityEventPublisher: ActivityEventPublisher

    @Mock
    private lateinit var dailyMessageCommentResultReader: DailyMessageCommentResultReader

    @Mock
    private lateinit var memberRepository: MemberRepository

    @InjectMocks
    private lateinit var useCase: CreateCommentUseCaseImpl

    @Test
    @DisplayName("하루덕담 댓글을 작성하면 COMMENT_CREATE 이벤트를 발행한다")
    fun publishesCommentCreate() {
        whenever(memberRepository.findById(MemberPk.of(1L))).thenReturn(stubMember())
        whenever(dailyMessageRepository.findById(DailyMessageId.of(3L))).thenReturn(stubDailyMessage())
        whenever(dailyMessageCommentRepository.save(any())).thenReturn(stubSavedComment())

        useCase.execute(
            CreateCommentCommand(
                dailyMessageId = 3L,
                memberId = 1L,
                content = "댓글",
                parentCommentId = null
            )
        )

        verify(activityEventPublisher).publish(ActivityEvent(1L, ActivityType.COMMENT_CREATE))
    }

    private fun stubMember(): Member =
        Member.reconstitute(
            id = 1L,
            memberId = "testmember",
            nickname = "테스터",
            roleId = null,
            profileImage = "",
            provider = SocialProvider.GOOGLE,
            socialId = "12345",
            email = "test@example.com",
            password = null,
            emailVerified = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

    private fun stubDailyMessage(): DailyMessage =
        DailyMessage.reconstitute(
            id = DailyMessageId.of(3L),
            title = "제목",
            content = "내용",
            date = LocalDate.now(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

    private fun stubSavedComment(): DailyMessageComment =
        DailyMessageComment.reconstitute(
            id = DailyMessageCommentId.of(9L),
            dailyMessageId = 3L,
            memberId = 1L,
            content = "댓글",
            likeCount = 0,
            parentCommentId = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
}
