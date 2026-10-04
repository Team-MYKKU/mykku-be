package com.example.mykku.feed.application.usecase

import com.example.mykku.achievement.application.event.ActivityEvent
import com.example.mykku.achievement.application.port.output.ActivityEventPublisher
import com.example.mykku.achievement.domain.vo.ActivityType
import com.example.mykku.feed.application.dto.CreateFeedCommentCommand
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.entity.FeedComment
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.member.domain.entity.Member
import com.example.mykku.member.domain.vo.SocialProvider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("피드 댓글 작성 활동 이벤트 발행 단위 테스트")
class CreateFeedCommentActivityPublishTest {

    @Mock
    private lateinit var feedRepository: FeedRepository

    @Mock
    private lateinit var activityEventPublisher: ActivityEventPublisher

    @Mock
    private lateinit var feedCommentResultReader: FeedCommentResultReader

    @Test
    @DisplayName("피드 댓글을 작성하면 COMMENT_CREATE 이벤트를 발행한다")
    fun publishesCommentCreate() {
        val feedCommentRepository: FeedCommentRepository = mock(defaultAnswer = { stubSavedComment() })
        val useCase = CreateFeedCommentUseCaseImpl(
            feedRepository,
            feedCommentRepository,
            activityEventPublisher,
            feedCommentResultReader
        )
        whenever(feedRepository.findByIdOrThrow(FeedId.of(2L))).thenReturn(stubFeed())

        useCase.execute(
            CreateFeedCommentCommand(feedId = 2L, memberId = 1L, content = "댓글", parentCommentId = null),
            stubMember()
        )

        verify(activityEventPublisher).publish(ActivityEvent(1L, ActivityType.COMMENT_CREATE))
    }

    private fun stubFeed(): Feed =
        Feed.reconstitute(
            id = 2L,
            title = "제목",
            content = "내용",
            likeCount = 0,
            commentCount = 0,
            boardId = 1L,
            memberId = 1L,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

    private fun stubSavedComment(): FeedComment =
        FeedComment.reconstitute(
            id = 5L,
            content = "댓글",
            likeCount = 0,
            feedId = 2L,
            parentCommentId = null,
            memberId = 1L,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

    private fun stubMember(): Member =
        Member.reconstitute(
            id = 1L,
            memberId = "tester",
            nickname = "테스터",
            roleId = null,
            profileImage = "",
            provider = SocialProvider.GOOGLE,
            socialId = "social-1",
            email = "tester@test.com",
            password = null,
            emailVerified = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
}
