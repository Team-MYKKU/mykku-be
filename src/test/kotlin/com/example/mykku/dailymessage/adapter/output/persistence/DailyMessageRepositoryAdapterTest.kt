package com.example.mykku.dailymessage.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.dailymessage.application.port.output.DailyMessageRepository
import com.example.mykku.dailymessage.domain.entity.DailyMessage
import com.example.mykku.dailymessage.domain.vo.DailyMessageId
import com.example.mykku.dailymessage.domain.vo.SortDirection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate

@DisplayName("DailyMessageRepositoryAdapter 통합 테스트")
class DailyMessageRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var dailyMessageRepository: DailyMessageRepository

    @Nested
    @DisplayName("save 메서드")
    inner class Save {

        @Test
        @DisplayName("일상 메시지를 저장하면 ID가 부여된다")
        fun `저장 - 정상 케이스`() {
            val dailyMessage = createDailyMessage()

            val savedMessage = dailyMessageRepository.save(dailyMessage)

            assertThat(savedMessage.id.value).isNotEqualTo(0L)
            assertThat(savedMessage.title).isEqualTo(dailyMessage.title)
            assertThat(savedMessage.content).isEqualTo(dailyMessage.content)
            assertThat(savedMessage.date).isEqualTo(dailyMessage.date)
        }

        @Test
        @DisplayName("기존 일상 메시지를 수정하여 저장할 수 있다")
        fun `저장 - 수정 케이스`() {
            val dailyMessage = createDailyMessage()
            val savedMessage = dailyMessageRepository.save(dailyMessage)

            val result = dailyMessageRepository.save(updateFields(savedMessage))
            flushAndClear()

            assertThat(result.id).isEqualTo(savedMessage.id)
            assertThat(result.title).isEqualTo("수정된 제목")
            assertThat(result.content).isEqualTo("수정된 내용")
            assertThat(result.date).isEqualTo(LocalDate.of(2025, 2, 1))
            val foundMessage = dailyMessageRepository.findById(savedMessage.id)!!
            assertThat(foundMessage.title).isEqualTo("수정된 제목")
            assertThat(foundMessage.content).isEqualTo("수정된 내용")
            assertThat(foundMessage.date).isEqualTo(LocalDate.of(2025, 2, 1))
        }

        private fun updateFields(message: DailyMessage): DailyMessage {
            return message.update(
                title = "수정된 제목",
                content = "수정된 내용",
                date = LocalDate.of(2025, 2, 1)
            )
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("ID로 일상 메시지를 조회한다")
        fun `조회 - 정상 케이스`() {
            val dailyMessage = createDailyMessage()
            val savedMessage = dailyMessageRepository.save(dailyMessage)

            val foundMessage = dailyMessageRepository.findById(savedMessage.id)

            assertThat(foundMessage).isNotNull
            assertThat(foundMessage!!.id).isEqualTo(savedMessage.id)
            assertThat(foundMessage.title).isEqualTo(savedMessage.title)
            assertThat(foundMessage.content).isEqualTo(savedMessage.content)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun `조회 - 존재하지 않는 ID`() {
            val foundMessage = dailyMessageRepository.findById(DailyMessageId(999999L))

            assertThat(foundMessage).isNull()
        }
    }

    @Nested
    @DisplayName("findByDate 메서드")
    inner class FindByDate {

        @Test
        @DisplayName("날짜로 일상 메시지를 조회한다")
        fun `조회 - 정상 케이스`() {
            val targetDate = LocalDate.of(2025, 1, 26)
            val dailyMessage = createDailyMessage(date = targetDate)
            dailyMessageRepository.save(dailyMessage)

            val foundMessage = dailyMessageRepository.findByDate(targetDate)

            assertThat(foundMessage).isNotNull
            assertThat(foundMessage!!.date).isEqualTo(targetDate)
        }

        @Test
        @DisplayName("해당 날짜의 메시지가 없으면 null을 반환한다")
        fun `조회 - 해당 날짜 없음`() {
            val foundMessage = dailyMessageRepository.findByDate(LocalDate.of(2099, 12, 31))

            assertThat(foundMessage).isNull()
        }

        @Test
        @DisplayName("여러 날짜의 메시지 중 특정 날짜를 정확히 조회한다")
        fun `조회 - 여러 날짜 중 특정 날짜`() {
            val date1 = LocalDate.of(2025, 1, 25)
            val date2 = LocalDate.of(2025, 1, 26)
            val date3 = LocalDate.of(2025, 1, 27)
            dailyMessageRepository.save(createDailyMessage(date = date1, title = "첫번째"))
            dailyMessageRepository.save(createDailyMessage(date = date2, title = "두번째"))
            dailyMessageRepository.save(createDailyMessage(date = date3, title = "세번째"))

            val foundMessage = dailyMessageRepository.findByDate(date2)

            assertThat(foundMessage).isNotNull
            assertThat(foundMessage!!.title).isEqualTo("두번째")
        }
    }

    @Nested
    @DisplayName("findByDateBeforeOrEqual 메서드")
    inner class FindByDateBeforeOrEqual {

        @Test
        @DisplayName("특정 날짜 이전 또는 같은 메시지를 페이지네이션하여 조회한다")
        fun `조회 - 정상 케이스`() {
            val date1 = LocalDate.of(2025, 1, 23)
            val date2 = LocalDate.of(2025, 1, 24)
            val date3 = LocalDate.of(2025, 1, 25)
            val date4 = LocalDate.of(2025, 1, 26)
            val message1 = dailyMessageRepository.save(createDailyMessage(date = date1))
            val message2 = dailyMessageRepository.save(createDailyMessage(date = date2))
            val message3 = dailyMessageRepository.save(createDailyMessage(date = date3))
            dailyMessageRepository.save(createDailyMessage(date = date4))

            val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "date"))
            val result = dailyMessageRepository.findByDateBeforeOrEqual(date3, pageable)

            assertThat(result.content.map { it.id }).containsExactlyInAnyOrder(message1.id, message2.id, message3.id)
            assertThat(result.content.map { it.date }).allMatch { it <= date3 }
        }

        @Test
        @DisplayName("페이지 크기만큼 조회한다")
        fun `조회 - 페이지 크기 제한`() {
            for (i in 1..5) {
                dailyMessageRepository.save(createDailyMessage(date = LocalDate.of(2025, 1, i)))
            }

            val pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "date"))
            val result = dailyMessageRepository.findByDateBeforeOrEqual(LocalDate.of(2025, 1, 5), pageable)

            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(5)
            assertThat(result.totalPages).isEqualTo(3)
        }

        @Test
        @DisplayName("날짜 이전 메시지가 없으면 빈 결과를 반환한다")
        fun `조회 - 결과 없음`() {
            dailyMessageRepository.save(createDailyMessage(date = LocalDate.of(2025, 2, 1)))

            val pageable = PageRequest.of(0, 10)
            val result = dailyMessageRepository.findByDateBeforeOrEqual(LocalDate.of(2025, 1, 1), pageable)

            assertThat(result.content).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByDateBeforeOrEqualWithSort 메서드")
    inner class FindByDateBeforeOrEqualWithSort {

        @Test
        @DisplayName("날짜 내림차순으로 조회한다")
        fun `조회 - 내림차순`() {
            val date1 = LocalDate.of(2025, 1, 23)
            val date2 = LocalDate.of(2025, 1, 24)
            val date3 = LocalDate.of(2025, 1, 25)
            val message1 = dailyMessageRepository.save(createDailyMessage(date = date1))
            val message2 = dailyMessageRepository.save(createDailyMessage(date = date2))
            val message3 = dailyMessageRepository.save(createDailyMessage(date = date3))

            val result = dailyMessageRepository.findByDateBeforeOrEqualWithSort(
                date = LocalDate.of(2025, 1, 25),
                limit = 10,
                sort = SortDirection.DESC
            )

            assertThat(result.map { it.id }).containsExactlyInAnyOrder(message1.id, message2.id, message3.id)
            assertThat(result[0].date).isEqualTo(date3)
            assertThat(result[1].date).isEqualTo(date2)
            assertThat(result[2].date).isEqualTo(date1)
        }

        @Test
        @DisplayName("날짜 오름차순으로 조회한다")
        fun `조회 - 오름차순`() {
            val date1 = LocalDate.of(2025, 1, 23)
            val date2 = LocalDate.of(2025, 1, 24)
            val date3 = LocalDate.of(2025, 1, 25)
            val message1 = dailyMessageRepository.save(createDailyMessage(date = date1))
            val message2 = dailyMessageRepository.save(createDailyMessage(date = date2))
            val message3 = dailyMessageRepository.save(createDailyMessage(date = date3))

            val result = dailyMessageRepository.findByDateBeforeOrEqualWithSort(
                date = LocalDate.of(2025, 1, 25),
                limit = 10,
                sort = SortDirection.ASC
            )

            assertThat(result.map { it.id }).containsExactlyInAnyOrder(message1.id, message2.id, message3.id)
            assertThat(result[0].date).isEqualTo(date1)
            assertThat(result[1].date).isEqualTo(date2)
            assertThat(result[2].date).isEqualTo(date3)
        }

        @Test
        @DisplayName("limit 만큼만 조회한다")
        fun `조회 - limit 제한`() {
            for (i in 1..5) {
                dailyMessageRepository.save(createDailyMessage(date = LocalDate.of(2025, 1, i)))
            }

            val result = dailyMessageRepository.findByDateBeforeOrEqualWithSort(
                date = LocalDate.of(2025, 1, 5),
                limit = 3,
                sort = SortDirection.DESC
            )

            assertThat(result).hasSize(3)
        }

        @Test
        @DisplayName("결과가 없으면 빈 리스트를 반환한다")
        fun `조회 - 결과 없음`() {
            dailyMessageRepository.save(createDailyMessage(date = LocalDate.of(2025, 2, 1)))

            val result = dailyMessageRepository.findByDateBeforeOrEqualWithSort(
                date = LocalDate.of(2025, 1, 1),
                limit = 10,
                sort = SortDirection.DESC
            )

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findAll 메서드")
    inner class FindAll {

        @Test
        @DisplayName("모든 일상 메시지를 페이지네이션하여 조회한다")
        fun `조회 - 정상 케이스`() {
            val savedIds = (1..5).map { i ->
                dailyMessageRepository.save(createDailyMessage(date = LocalDate.of(2025, 1, i))).id
            }

            val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "date"))
            val result = dailyMessageRepository.findAll(pageable)

            assertThat(result.content.map { it.id }).containsExactlyInAnyOrderElementsOf(savedIds)
            assertThat(result.totalElements).isEqualTo(5)
        }

        @Test
        @DisplayName("페이지 크기만큼 조회한다")
        fun `조회 - 페이지 크기 제한`() {
            for (i in 1..10) {
                dailyMessageRepository.save(createDailyMessage(date = LocalDate.of(2025, 1, i)))
            }

            val pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "date"))
            val result = dailyMessageRepository.findAll(pageable)

            assertThat(result.content).hasSize(3)
            assertThat(result.totalElements).isEqualTo(10)
            assertThat(result.totalPages).isEqualTo(4)
        }

        @Test
        @DisplayName("두번째 페이지를 조회한다")
        fun `조회 - 두번째 페이지`() {
            for (i in 1..10) {
                dailyMessageRepository.save(createDailyMessage(date = LocalDate.of(2025, 1, i)))
            }

            val pageable = PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "date"))
            val result = dailyMessageRepository.findAll(pageable)

            assertThat(result.content).hasSize(3)
            assertThat(result.number).isEqualTo(1)
        }

        @Test
        @DisplayName("메시지가 없으면 빈 페이지를 반환한다")
        fun `조회 - 결과 없음`() {
            val pageable = PageRequest.of(0, 10)
            val result = dailyMessageRepository.findAll(pageable)

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("deleteById 메서드")
    inner class DeleteById {

        @Test
        @DisplayName("ID로 일상 메시지를 삭제한다")
        fun `삭제 - 정상 케이스`() {
            val dailyMessage = createDailyMessage()
            val savedMessage = dailyMessageRepository.save(dailyMessage)

            dailyMessageRepository.deleteById(savedMessage.id)

            val foundMessage = dailyMessageRepository.findById(savedMessage.id)
            assertThat(foundMessage).isNull()
            flushAndClear()
            assertThat(dailyMessageRepository.findById(savedMessage.id)).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 ID로 삭제해도 예외가 발생하지 않는다")
        fun `삭제 - 존재하지 않는 ID`() {
            dailyMessageRepository.deleteById(DailyMessageId(999999L))
        }
    }

    private fun createDailyMessage(
        title: String = "테스트 제목",
        content: String = "테스트 내용",
        date: LocalDate = LocalDate.of(2025, 1, 26)
    ): DailyMessage {
        return DailyMessage.create(
            title = title,
            content = content,
            date = date
        )
    }
}
