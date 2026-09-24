package com.example.mykku

import com.example.mykku.util.DatabaseCleaner
import com.example.mykku.admin.service.AdminRoleService
import com.example.mykku.auth.adapter.output.persistence.JwtTokenProviderAdapter
import com.example.mykku.auth.application.port.input.LogoutUseCase
import com.example.mykku.auth.application.port.input.MobileLoginUseCase
import com.example.mykku.auth.application.port.input.RefreshTokenUseCase
import com.example.mykku.block.application.port.input.BlockKeywordUseCase
import com.example.mykku.block.application.port.input.BlockMemberUseCase
import com.example.mykku.block.application.port.input.GetKeywordBlocksUseCase
import com.example.mykku.block.application.port.input.GetMemberBlocksUseCase
import com.example.mykku.block.application.port.input.UnblockKeywordUseCase
import com.example.mykku.block.application.port.input.UnblockMemberUseCase
import com.example.mykku.board.application.port.input.ListBoardsUseCase
import com.example.mykku.contest.application.port.input.CreateContestUseCase
import com.example.mykku.contest.application.port.input.GetContestUseCase
import com.example.mykku.contest.application.port.input.GetContestWinnerAnnouncementUseCase
import com.example.mykku.contest.application.port.input.GetContestWinnerDetailUseCase
import com.example.mykku.contest.application.port.input.GetContestWinnersListUseCase
import com.example.mykku.contest.application.port.input.GetMyAwardContestsUseCase
import com.example.mykku.contest.application.port.input.GetMyAwardFeedsUseCase
import com.example.mykku.contest.application.port.input.GetMyAwardsPreviewUseCase
import com.example.mykku.contest.application.port.input.GetMyWinnerStatusUseCase
import com.example.mykku.contest.application.port.input.ListContestsUseCase
import com.example.mykku.contest.application.port.input.SetContestWinnersUseCase
import com.example.mykku.contest.application.port.input.UpdateAcceptanceSpeechUseCase
import com.example.mykku.dailymessage.application.port.input.CreateCommentUseCase
import com.example.mykku.dailymessage.application.port.input.DeleteCommentUseCase
import com.example.mykku.dailymessage.application.port.input.GetCommentsUseCase
import com.example.mykku.dailymessage.application.port.input.GetDailyMessageUseCase
import com.example.mykku.dailymessage.application.port.input.GetDailyMessagesUseCase
import com.example.mykku.dailymessage.application.port.input.UpdateCommentUseCase
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationFilterBuilder
import com.example.mykku.docs.RestDocumentationRequest
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.email.EmailAuthService
import com.example.mykku.event.application.port.input.CreateEventUseCase
import com.example.mykku.event.application.port.input.GetEventUseCase
import com.example.mykku.event.application.port.input.GetEventWinnerAnnouncementUseCase
import com.example.mykku.event.application.port.input.GetEventWinnersUseCase
import com.example.mykku.event.application.port.input.GetMyAwardEventsUseCase
import com.example.mykku.event.application.port.input.GetMyEventWinnerStatusUseCase
import com.example.mykku.event.application.port.input.ListEventsUseCase
import com.example.mykku.event.application.port.input.SetEventWinnersUseCase
import com.example.mykku.fannote.application.port.input.GetFanNoteDetailUseCase
import com.example.mykku.fannote.application.port.input.GetFanNoteListUseCase
import com.example.mykku.feed.application.port.input.CreateFeedCommentUseCase
import com.example.mykku.feed.application.port.input.CreateFeedUseCase
import com.example.mykku.feed.application.port.input.DeleteFeedCommentUseCase
import com.example.mykku.feed.application.port.input.DeleteFeedUseCase
import com.example.mykku.feed.application.port.input.GetFeedCommentsUseCase
import com.example.mykku.feed.application.port.input.GetFeedDetailUseCase
import com.example.mykku.feed.application.port.input.GetPopularFeedsUseCase
import com.example.mykku.feed.application.port.input.GetMyFeedsUseCase
import com.example.mykku.feed.application.port.input.ListFeedsUseCase
import com.example.mykku.feed.application.port.input.UpdateFeedCommentUseCase
import com.example.mykku.feed.application.port.input.UpdateFeedUseCase
import com.example.mykku.like.application.port.input.LikeBoardUseCase
import com.example.mykku.like.application.port.input.LikeDailyMessageCommentUseCase
import com.example.mykku.like.application.port.input.LikeFeedCommentUseCase
import com.example.mykku.like.application.port.input.LikeFeedUseCase
import com.example.mykku.member.adapter.output.persistence.MemberJpaRepository
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.member.application.port.input.ChangeMemberIdUseCase
import com.example.mykku.member.application.port.input.ChangePasswordUseCase
import com.example.mykku.member.application.port.input.CheckMemberIdUseCase
import com.example.mykku.member.application.port.input.WithdrawMemberUseCase
import com.example.mykku.member.application.port.input.SetupProfileUseCase
import com.example.mykku.member.application.port.input.GetMemberProfileUseCase
import com.example.mykku.member.application.port.input.UpdateMemberProfileUseCase
import com.example.mykku.event.application.port.input.GetMyParticipatedEventsUseCase
import com.example.mykku.member.domain.vo.SocialProvider
import com.example.mykku.notification.application.port.input.DeleteNotificationUseCase
import com.example.mykku.notification.application.port.input.GetNotificationsUseCase
import com.example.mykku.notification.application.port.input.ManageFcmTokenUseCase
import com.example.mykku.notification.application.port.input.ManageNotificationSettingUseCase
import com.example.mykku.notification.application.port.input.MarkNotificationReadUseCase
import com.example.mykku.preference.application.port.input.GetGenrePreferenceUseCase
import com.example.mykku.preference.application.port.input.GetGoodsPreferenceUseCase
import com.example.mykku.preference.application.port.input.GetMoodPreferenceUseCase
import com.example.mykku.preference.application.port.input.UpdateGenrePreferenceUseCase
import com.example.mykku.preference.application.port.input.UpdateGoodsPreferenceUseCase
import com.example.mykku.preference.application.port.input.UpdateMoodPreferenceUseCase
import com.example.mykku.report.application.port.input.CreateReportUseCase
import com.example.mykku.role.application.port.input.AcquireRoleUseCase
import com.example.mykku.role.application.port.input.ChangeRepresentativeRoleUseCase
import com.example.mykku.role.application.port.input.GetMyRolesUseCase
import com.example.mykku.role.application.port.input.GetNewRolesUseCase
import com.example.mykku.role.application.port.input.GetRolesUseCase
import com.example.mykku.contest.application.port.input.GetMyParticipatedContestsUseCase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.Header
import io.restassured.http.Headers
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.doReturn
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.restdocs.headers.HeaderDescriptor
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.restdocs.restassured.RestAssuredRestDocumentation
import org.springframework.restdocs.restassured.RestDocumentationFilter
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@ActiveProfiles("test")
@ExtendWith(DatabaseCleaner::class, RestDocumentationExtension::class, MockitoExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseDocumentTest {

    @MockitoBean
    protected lateinit var adminRoleService: AdminRoleService

    @MockitoBean
    protected lateinit var mobileLoginUseCase: MobileLoginUseCase

    @MockitoBean
    protected lateinit var refreshTokenUseCase: RefreshTokenUseCase

    @MockitoBean
    protected lateinit var logoutUseCase: LogoutUseCase

    @MockitoBean
    protected lateinit var listBoardsUseCase: ListBoardsUseCase

    @MockitoBean
    protected lateinit var createContestUseCase: CreateContestUseCase

    @MockitoBean
    protected lateinit var getContestUseCase: GetContestUseCase

    @MockitoBean
    protected lateinit var listContestsUseCase: ListContestsUseCase

    @MockitoBean
    protected lateinit var getContestWinnersListUseCase: GetContestWinnersListUseCase

    @MockitoBean
    protected lateinit var getContestWinnerDetailUseCase: GetContestWinnerDetailUseCase

    @MockitoBean
    protected lateinit var getContestWinnerAnnouncementUseCase: GetContestWinnerAnnouncementUseCase

    @MockitoBean
    protected lateinit var updateAcceptanceSpeechUseCase: UpdateAcceptanceSpeechUseCase

    @MockitoBean
    protected lateinit var getMyWinnerStatusUseCase: GetMyWinnerStatusUseCase

    @MockitoBean
    protected lateinit var getMyAwardContestsUseCase: GetMyAwardContestsUseCase

    @MockitoBean
    protected lateinit var getMyAwardFeedsUseCase: GetMyAwardFeedsUseCase

    @MockitoBean
    protected lateinit var getMyAwardsPreviewUseCase: GetMyAwardsPreviewUseCase

    @MockitoBean
    protected lateinit var setContestWinnersUseCase: SetContestWinnersUseCase

    @MockitoBean
    protected lateinit var getMyRolesUseCase: GetMyRolesUseCase

    @MockitoBean
    protected lateinit var changeRepresentativeRoleUseCase: ChangeRepresentativeRoleUseCase

    @MockitoBean
    protected lateinit var acquireRoleUseCase: AcquireRoleUseCase

    @MockitoBean
    protected lateinit var getRolesUseCase: GetRolesUseCase

    @MockitoBean
    protected lateinit var getNewRolesUseCase: GetNewRolesUseCase

    @MockitoBean
    protected lateinit var getGenrePreferenceUseCase: GetGenrePreferenceUseCase

    @MockitoBean
    protected lateinit var updateGenrePreferenceUseCase: UpdateGenrePreferenceUseCase

    @MockitoBean
    protected lateinit var getGoodsPreferenceUseCase: GetGoodsPreferenceUseCase

    @MockitoBean
    protected lateinit var updateGoodsPreferenceUseCase: UpdateGoodsPreferenceUseCase

    @MockitoBean
    protected lateinit var getMoodPreferenceUseCase: GetMoodPreferenceUseCase

    @MockitoBean
    protected lateinit var updateMoodPreferenceUseCase: UpdateMoodPreferenceUseCase

    @MockitoBean
    protected lateinit var manageNotificationSettingUseCase: ManageNotificationSettingUseCase

    @MockitoBean
    protected lateinit var getNotificationsUseCase: GetNotificationsUseCase

    @MockitoBean
    protected lateinit var markNotificationReadUseCase: MarkNotificationReadUseCase

    @MockitoBean
    protected lateinit var deleteNotificationUseCase: DeleteNotificationUseCase

    @MockitoBean
    protected lateinit var getMemberProfileUseCase: GetMemberProfileUseCase

    @MockitoBean
    protected lateinit var updateMemberProfileUseCase: UpdateMemberProfileUseCase

    @MockitoBean
    protected lateinit var changePasswordUseCase: ChangePasswordUseCase

    @MockitoBean
    protected lateinit var setupProfileUseCase: SetupProfileUseCase

    @MockitoBean
    protected lateinit var checkMemberIdUseCase: CheckMemberIdUseCase

    @MockitoBean
    protected lateinit var changeMemberIdUseCase: ChangeMemberIdUseCase

    @MockitoBean
    protected lateinit var withdrawMemberUseCase: WithdrawMemberUseCase

    @MockitoBean
    protected lateinit var getMyParticipatedContestsUseCase: GetMyParticipatedContestsUseCase

    @MockitoBean
    protected lateinit var getMyParticipatedEventsUseCase: GetMyParticipatedEventsUseCase

    @MockitoBean
    protected lateinit var likeFeedUseCase: LikeFeedUseCase

    @MockitoBean
    protected lateinit var likeFeedCommentUseCase: LikeFeedCommentUseCase

    @MockitoBean
    protected lateinit var likeBoardUseCase: LikeBoardUseCase

    @MockitoBean
    protected lateinit var likeDailyMessageCommentUseCase: LikeDailyMessageCommentUseCase

    @MockitoBean
    protected lateinit var manageFcmTokenUseCase: ManageFcmTokenUseCase

    @MockitoBean
    protected lateinit var getFanNoteListUseCase: GetFanNoteListUseCase

    @MockitoBean
    protected lateinit var getFanNoteDetailUseCase: GetFanNoteDetailUseCase

    @MockitoBean
    protected lateinit var createEventUseCase: CreateEventUseCase

    @MockitoBean
    protected lateinit var getEventUseCase: GetEventUseCase

    @MockitoBean
    protected lateinit var listEventsUseCase: ListEventsUseCase

    @MockitoBean
    protected lateinit var getEventWinnersUseCase: GetEventWinnersUseCase

    @MockitoBean
    protected lateinit var getEventWinnerAnnouncementUseCase: GetEventWinnerAnnouncementUseCase

    @MockitoBean
    protected lateinit var getMyEventWinnerStatusUseCase: GetMyEventWinnerStatusUseCase

    @MockitoBean
    protected lateinit var getMyAwardEventsUseCase: GetMyAwardEventsUseCase

    @MockitoBean
    protected lateinit var setEventWinnersUseCase: SetEventWinnersUseCase

    @MockitoBean
    protected lateinit var emailAuthService: EmailAuthService

    @MockitoBean
    protected lateinit var getDailyMessagesUseCase: GetDailyMessagesUseCase

    @MockitoBean
    protected lateinit var getDailyMessageUseCase: GetDailyMessageUseCase

    @MockitoBean
    protected lateinit var getCommentsUseCase: GetCommentsUseCase

    @MockitoBean
    protected lateinit var createCommentUseCase: CreateCommentUseCase

    @MockitoBean
    protected lateinit var updateCommentUseCase: UpdateCommentUseCase

    @MockitoBean
    protected lateinit var deleteCommentUseCase: DeleteCommentUseCase

    @MockitoBean
    protected lateinit var blockMemberUseCase: BlockMemberUseCase

    @MockitoBean
    protected lateinit var unblockMemberUseCase: UnblockMemberUseCase

    @MockitoBean
    protected lateinit var getMemberBlocksUseCase: GetMemberBlocksUseCase

    @MockitoBean
    protected lateinit var blockKeywordUseCase: BlockKeywordUseCase

    @MockitoBean
    protected lateinit var unblockKeywordUseCase: UnblockKeywordUseCase

    @MockitoBean
    protected lateinit var getKeywordBlocksUseCase: GetKeywordBlocksUseCase

    @MockitoBean
    protected lateinit var createFeedUseCase: CreateFeedUseCase

    @MockitoBean
    protected lateinit var getFeedDetailUseCase: GetFeedDetailUseCase

    @MockitoBean
    protected lateinit var updateFeedUseCase: UpdateFeedUseCase

    @MockitoBean
    protected lateinit var deleteFeedUseCase: DeleteFeedUseCase

    @MockitoBean
    protected lateinit var listFeedsUseCase: ListFeedsUseCase

    @MockitoBean
    protected lateinit var getMyFeedsUseCase: GetMyFeedsUseCase

    @MockitoBean
    protected lateinit var getFeedCommentsUseCase: GetFeedCommentsUseCase

    @MockitoBean
    protected lateinit var getPopularFeedsUseCase: GetPopularFeedsUseCase

    @MockitoBean
    protected lateinit var createFeedCommentUseCase: CreateFeedCommentUseCase

    @MockitoBean
    protected lateinit var updateFeedCommentUseCase: UpdateFeedCommentUseCase

    @MockitoBean
    protected lateinit var deleteFeedCommentUseCase: DeleteFeedCommentUseCase

    @MockitoBean
    protected lateinit var createReportUseCase: CreateReportUseCase

    companion object {
        const val TEST_MEMBER_EMAIL = "test@example.com"
        const val TEST_ACCESS_TOKEN = "test-access-token"

        val AUTH_HEADER: Headers = Headers(
            Header(HttpHeaders.AUTHORIZATION, "Bearer $TEST_ACCESS_TOKEN")
        )

        val AUTH_HEADER_DESCRIPTOR: List<HeaderDescriptor> = listOf(
            headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {JWT 액세스 토큰}")
        )
    }

    protected val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @MockitoBean
    protected lateinit var jwtTokenProvider: JwtTokenProviderAdapter

    @Autowired
    protected lateinit var memberJpaRepository: MemberJpaRepository

    @LocalServerPort
    private var port: Int = 0

    private lateinit var spec: RequestSpecification

    protected lateinit var testMember: MemberJpaEntity

    @BeforeEach
    fun setEnvironment(restDocumentation: RestDocumentationContextProvider) {
        setRestAssured(restDocumentation)
        setLoginMember()
    }

    private fun setRestAssured(restDocumentation: RestDocumentationContextProvider) {
        RestAssured.port = port
        val documentationConfigurer = RestAssuredRestDocumentation.documentationConfiguration(restDocumentation)
            .operationPreprocessors()
            .withRequestDefaults(
                Preprocessors.modifyUris()
                    .scheme("https")
                    .host("api.dev.mykku.kr")
                    .removePort(),
                Preprocessors.prettyPrint()
            )
            .withResponseDefaults(Preprocessors.prettyPrint())

        spec = RequestSpecBuilder()
            .addFilter(documentationConfigurer)
            .addFilter(RequestLoggingFilter())
            .addFilter(ResponseLoggingFilter())
            .build()
    }

    private fun setLoginMember() {
        testMember = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid",
                nickname = "testuser",
                role = null,
                profileImage = "https://example.com/profile.jpg",
                provider = SocialProvider.GOOGLE,
                socialId = "123456",
                email = TEST_MEMBER_EMAIL
            )
        )

        doReturn(true).`when`(jwtTokenProvider).validateToken(TEST_ACCESS_TOKEN)
        doReturn(true).`when`(jwtTokenProvider).isAccessToken(TEST_ACCESS_TOKEN)
        doReturn(testMember.id).`when`(jwtTokenProvider).getMemberIdFromToken(TEST_ACCESS_TOKEN)
    }

    protected fun request(): RestDocumentationRequest = RestDocumentationRequest()

    protected fun response(): RestDocumentationResponse = RestDocumentationResponse()

    protected fun RestDocumentationRequest.applyConfig(config: ApiRequestConfig): RestDocumentationRequest {
        if (config.pathParameters.isNotEmpty()) {
            pathParameter(*config.pathParameters.toTypedArray())
        }
        if (config.queryParameters.isNotEmpty()) {
            queryParameter(*config.queryParameters.toTypedArray())
        }
        if (config.requestBodyFields.isNotEmpty()) {
            requestBodyField(*config.requestBodyFields.toTypedArray())
        }
        if (config.requestParts.isNotEmpty()) {
            requestPart(*config.requestParts.toTypedArray())
        }
        if (config.requestPartFields.isNotEmpty()) {
            config.requestPartFields.forEach { (partName, fields) ->
                requestPartField(partName, *fields.toTypedArray())
            }
        }
        if (config.headerDescriptors.isNotEmpty()) {
            requestHeader(*config.headerDescriptors.toTypedArray())
        }
        return this
    }

    protected fun document(identifierPrefix: String, status: Int): RestDocumentationFilterBuilder =
        RestDocumentationFilterBuilder(identifierPrefix, status.toString())

    protected fun document(identifierPrefix: String, errorCode: String): RestDocumentationFilterBuilder =
        RestDocumentationFilterBuilder(identifierPrefix, errorCode)

    protected fun given(documentationFilter: RestDocumentationFilter): RequestSpecification =
        RestAssured.given(spec).filter(documentationFilter)

    protected fun given(): RequestSpecification = RestAssured.given(spec)
}
