package com.example.mykku.event.application.usecase

import com.example.mykku.event.application.dto.EventDeletionSummaryResult
import com.example.mykku.event.application.port.output.EventAttendanceRepository
import com.example.mykku.event.application.port.output.EventParticipationRepository
import com.example.mykku.event.application.port.output.EventWinnerRepository
import com.example.mykku.event.domain.vo.EventId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("이벤트 삭제 요약 UseCase 단위 테스트")
class GetEventDeletionSummariesUseCaseImplTest {

    @Mock
    private lateinit var eventParticipationRepository: EventParticipationRepository

    @Mock
    private lateinit var eventWinnerRepository: EventWinnerRepository

    @Mock
    private lateinit var eventAttendanceRepository: EventAttendanceRepository

    @InjectMocks
    private lateinit var useCase: GetEventDeletionSummariesUseCaseImpl

    @Test
    @DisplayName("이벤트마다 참여·당첨·출석 건수를 각 포트에서 읽어 담고, 포트에 없는 건수는 0이다")
    fun `요약 - 포트별 건수`() {
        val ids = listOf(EventId(1L), EventId(2L), EventId(3L))
        whenever(eventParticipationRepository.countByEventIds(ids)).thenReturn(mapOf(1L to 3))
        whenever(eventWinnerRepository.countByEventIds(ids)).thenReturn(mapOf(1L to 2))
        whenever(eventAttendanceRepository.countByEventIds(ids)).thenReturn(mapOf(1L to 5, 2L to 7))

        val summaries = useCase.execute(listOf(1L, 2L, 3L))

        assertThat(summaries).containsExactlyEntriesOf(
            mapOf(
                1L to EventDeletionSummaryResult(participationCount = 3, winnerCount = 2, attendanceCount = 5),
                2L to EventDeletionSummaryResult(participationCount = 0, winnerCount = 0, attendanceCount = 7),
                3L to EventDeletionSummaryResult(participationCount = 0, winnerCount = 0, attendanceCount = 0)
            )
        )
    }
}
