package com.example.mykku.fannote.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.fannote.application.port.output.FanNoteRepository
import com.example.mykku.fannote.domain.entity.FanNote
import com.example.mykku.fannote.domain.vo.FanNoteId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate

@DisplayName("FanNoteRepositoryAdapter 통합 테스트")
class FanNoteRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var fanNoteRepository: FanNoteRepository

    private fun createFanNote(
        title: String = "테스트 덕질노트",
        subtitle: String? = "서브타이틀",
        content: String? = "내용",
        productionDate: LocalDate = LocalDate.of(2024, 1, 1),
        coverImageUrl: String? = "https://example.com/cover.jpg"
    ): FanNote {
        return FanNote.create(
            title = title,
            subtitle = subtitle,
            content = content,
            productionDate = productionDate,
            coverImageUrl = coverImageUrl
        )
    }

    @Nested
    @DisplayName("save 메서드")
    inner class SaveTest {

        @Test
        @DisplayName("새로운 덕질노트를 저장한다")
        fun `새로운 덕질노트를 저장한다`() {
            // given
            val fanNote = createFanNote()

            // when
            val savedFanNote = fanNoteRepository.save(fanNote)

            // then
            assertThat(savedFanNote.id.value).isNotEqualTo(0L)
            assertThat(savedFanNote.title).isEqualTo("테스트 덕질노트")
            assertThat(savedFanNote.subtitle).isEqualTo("서브타이틀")
            assertThat(savedFanNote.content).isEqualTo("내용")
            assertThat(savedFanNote.productionDate).isEqualTo(LocalDate.of(2024, 1, 1))
            assertThat(savedFanNote.coverImageUrl).isEqualTo("https://example.com/cover.jpg")
            assertThat(savedFanNote.createdAt).isNotNull()
            assertThat(savedFanNote.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("nullable 필드가 null인 덕질노트를 저장한다")
        fun `nullable 필드가 null인 덕질노트를 저장한다`() {
            // given
            val fanNote = createFanNote(
                subtitle = null,
                content = null,
                coverImageUrl = null
            )

            // when
            val savedFanNote = fanNoteRepository.save(fanNote)

            // then
            assertThat(savedFanNote.id.value).isNotEqualTo(0L)
            assertThat(savedFanNote.title).isEqualTo("테스트 덕질노트")
            assertThat(savedFanNote.subtitle).isNull()
            assertThat(savedFanNote.content).isNull()
            assertThat(savedFanNote.coverImageUrl).isNull()
        }

        @Test
        @DisplayName("기존 덕질노트를 수정한다")
        fun `기존 덕질노트를 수정한다`() {
            // given
            val fanNote = createFanNote()
            val savedFanNote = fanNoteRepository.save(fanNote)

            val updatedFanNote = savedFanNote.updateInfo(
                title = "수정된 제목",
                subtitle = "수정된 서브타이틀",
                content = "수정된 내용",
                productionDate = LocalDate.of(2024, 6, 15),
                coverImageUrl = "https://example.com/updated-cover.jpg"
            )

            // when
            val result = fanNoteRepository.save(updatedFanNote)
            flushAndClear()

            // then
            assertThat(result.id).isEqualTo(savedFanNote.id)
            assertUpdatedFields(result)
            assertUpdatedFields(fanNoteRepository.findById(savedFanNote.id)!!)
        }

        private fun assertUpdatedFields(fanNote: FanNote) {
            assertThat(fanNote.title).isEqualTo("수정된 제목")
            assertThat(fanNote.subtitle).isEqualTo("수정된 서브타이틀")
            assertThat(fanNote.content).isEqualTo("수정된 내용")
            assertThat(fanNote.productionDate).isEqualTo(LocalDate.of(2024, 6, 15))
            assertThat(fanNote.coverImageUrl).isEqualTo("https://example.com/updated-cover.jpg")
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindByIdTest {

        @Test
        @DisplayName("존재하는 덕질노트를 조회한다")
        fun `존재하는 덕질노트를 조회한다`() {
            // given
            val fanNote = createFanNote()
            val savedFanNote = fanNoteRepository.save(fanNote)

            // when
            val foundFanNote = fanNoteRepository.findById(savedFanNote.id)

            // then
            assertThat(foundFanNote).isNotNull
            assertThat(foundFanNote!!.id).isEqualTo(savedFanNote.id)
            assertThat(foundFanNote.title).isEqualTo("테스트 덕질노트")
            assertThat(foundFanNote.subtitle).isEqualTo("서브타이틀")
            assertThat(foundFanNote.content).isEqualTo("내용")
            assertThat(foundFanNote.productionDate).isEqualTo(LocalDate.of(2024, 1, 1))
            assertThat(foundFanNote.coverImageUrl).isEqualTo("https://example.com/cover.jpg")
        }

        @Test
        @DisplayName("존재하지 않는 덕질노트를 조회하면 null을 반환한다")
        fun `존재하지 않는 덕질노트를 조회하면 null을 반환한다`() {
            // given
            val nonExistentId = FanNoteId(999L)

            // when
            val foundFanNote = fanNoteRepository.findById(nonExistentId)

            // then
            assertThat(foundFanNote).isNull()
        }
    }

    @Nested
    @DisplayName("findAll 메서드")
    inner class FindAllTest {

        @Test
        @DisplayName("모든 덕질노트를 페이지네이션하여 조회한다")
        fun `모든 덕질노트를 페이지네이션하여 조회한다`() {
            // given
            val fanNote1 = createFanNote(
                title = "덕질노트 1",
                productionDate = LocalDate.of(2024, 1, 1)
            )
            val fanNote2 = createFanNote(
                title = "덕질노트 2",
                productionDate = LocalDate.of(2024, 1, 2)
            )
            val fanNote3 = createFanNote(
                title = "덕질노트 3",
                productionDate = LocalDate.of(2024, 1, 3)
            )
            fanNoteRepository.save(fanNote1)
            fanNoteRepository.save(fanNote2)
            fanNoteRepository.save(fanNote3)

            val pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "productionDate"))

            // when
            val page = fanNoteRepository.findAll(pageable)

            // then
            assertThat(page.content).hasSize(2)
            assertThat(page.totalElements).isEqualTo(3)
            assertThat(page.totalPages).isEqualTo(2)
            assertThat(page.content[0].title).isEqualTo("덕질노트 3")
            assertThat(page.content[1].title).isEqualTo("덕질노트 2")
        }

        @Test
        @DisplayName("빈 목록을 조회한다")
        fun `빈 목록을 조회한다`() {
            // given
            val pageable = PageRequest.of(0, 10)

            // when
            val page = fanNoteRepository.findAll(pageable)

            // then
            assertThat(page.content).isEmpty()
            assertThat(page.totalElements).isEqualTo(0)
            assertThat(page.totalPages).isEqualTo(0)
        }

        @Test
        @DisplayName("두 번째 페이지를 조회한다")
        fun `두 번째 페이지를 조회한다`() {
            // given
            repeat(5) { index ->
                val fanNote = createFanNote(
                    title = "덕질노트 ${index + 1}",
                    productionDate = LocalDate.of(2024, 1, index + 1)
                )
                fanNoteRepository.save(fanNote)
            }

            val pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "productionDate"))

            // when
            val page = fanNoteRepository.findAll(pageable)

            // then
            assertThat(page.content).hasSize(2)
            assertThat(page.totalElements).isEqualTo(5)
            assertThat(page.number).isEqualTo(1)
            assertThat(page.content[0].title).isEqualTo("덕질노트 3")
            assertThat(page.content[1].title).isEqualTo("덕질노트 2")
        }
    }

    @Nested
    @DisplayName("existsById 메서드")
    inner class ExistsByIdTest {

        @Test
        @DisplayName("존재하는 덕질노트에 대해 true를 반환한다")
        fun `존재하는 덕질노트에 대해 true를 반환한다`() {
            // given
            val fanNote = createFanNote()
            val savedFanNote = fanNoteRepository.save(fanNote)

            // when
            val exists = fanNoteRepository.existsById(savedFanNote.id)

            // then
            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 덕질노트에 대해 false를 반환한다")
        fun `존재하지 않는 덕질노트에 대해 false를 반환한다`() {
            // given
            val nonExistentId = FanNoteId(999L)

            // when
            val exists = fanNoteRepository.existsById(nonExistentId)

            // then
            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("deleteById 메서드")
    inner class DeleteByIdTest {

        @Test
        @DisplayName("존재하는 덕질노트를 삭제한다")
        fun `존재하는 덕질노트를 삭제한다`() {
            // given
            val fanNote = createFanNote()
            val savedFanNote = fanNoteRepository.save(fanNote)

            // when
            fanNoteRepository.deleteById(savedFanNote.id)

            // then
            val deletedFanNote = fanNoteRepository.findById(savedFanNote.id)
            assertThat(deletedFanNote).isNull()
            flushAndClear()
            assertThat(fanNoteRepository.findById(savedFanNote.id)).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 덕질노트 삭제 시도시 예외가 발생하지 않는다")
        fun `존재하지 않는 덕질노트 삭제 시도시 예외가 발생하지 않는다`() {
            // given
            val nonExistentId = FanNoteId(999L)

            // when & then
            fanNoteRepository.deleteById(nonExistentId)
        }
    }
}
