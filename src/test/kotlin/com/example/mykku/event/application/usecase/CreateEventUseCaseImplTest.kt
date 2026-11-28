package com.example.mykku.event.application.usecase

import com.example.mykku.event.application.dto.CreateEventCommand
import com.example.mykku.event.application.port.output.EventImageRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.EventException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("이벤트 생성 UseCase 단위 테스트")
class CreateEventUseCaseImplTest {

    @Mock
    private lateinit var eventRepository: EventRepository

    @Mock
    private lateinit var eventImageRepository: EventImageRepository

    @InjectMocks
    private lateinit var useCase: CreateEventUseCaseImpl

    @ParameterizedTest(name = "{displayName} [{0}]")
    @EnumSource(EventType::class)
    @DisplayName("명령의 유형과 구글폼 링크를 그대로 담아 이벤트를 저장한다")
    fun `생성 - 유형과 구글폼 링크 전달`(type: EventType) {
        val googleFormUrl = FORM_URL.takeIf { type == EventType.GOOGLE_FORM }
        whenever(eventRepository.save(any())).thenAnswer { it.getArgument<Event>(0) }

        useCase.execute(command(type, googleFormUrl))

        val saved = argumentCaptor<Event>().apply { verify(eventRepository).save(capture()) }.firstValue
        assertThat(saved.type).isEqualTo(type)
        assertThat(saved.googleFormUrl).isEqualTo(googleFormUrl)
    }

    @Test
    @DisplayName("유형 설정이 맞지 않으면 EV109이고 아무것도 저장하지 않는다")
    fun `생성 - 유형 설정 위반`() {
        val exception = assertThrows<EventException> {
            useCase.execute(command(EventType.GOOGLE_FORM, null))
        }

        assertThat(exception.errorCode).isEqualTo(EventErrorCode.INVALID_EVENT_TYPE_SETTING)
        verifyNoInteractions(eventRepository, eventImageRepository)
    }

    private fun command(type: EventType, googleFormUrl: String?): CreateEventCommand {
        return CreateEventCommand(
            title = "유형 이벤트",
            subTitle = null,
            description = null,
            startedAt = LocalDateTime.of(2026, 12, 1, 0, 0),
            expiredAt = LocalDateTime.of(2026, 12, 7, 23, 59, 59),
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            images = emptyList(),
            type = type,
            googleFormUrl = googleFormUrl
        )
    }

    companion object {
        private const val FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSc_example/viewform"
    }
}
