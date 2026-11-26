package com.example.mykku.fannote.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.fannote.application.port.output.FanNotePageRepository
import com.example.mykku.fannote.application.port.output.FanNoteRepository
import com.example.mykku.fannote.domain.entity.FanNote
import com.example.mykku.fannote.domain.entity.FanNotePage
import com.example.mykku.fannote.domain.vo.FanNoteId
import com.example.mykku.fannote.domain.vo.FanNotePageId
import com.example.mykku.fannote.exception.FanNoteErrorCode
import com.example.mykku.fannote.exception.FanNoteException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@DisplayName("FanNotePageRepositoryAdapter 통합 테스트")
class FanNotePageRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var fanNotePageRepository: FanNotePageRepository

    @Autowired
    private lateinit var fanNoteRepository: FanNoteRepository

    private lateinit var savedFanNote: FanNote

    @BeforeEach
    fun setUp() {
        val fanNote = FanNote.create(
            title = "테스트 덕질노트",
            subtitle = "서브타이틀",
            content = "내용",
            productionDate = LocalDate.of(2024, 1, 1),
            coverImageUrl = "https://example.com/cover.jpg"
        )
        savedFanNote = fanNoteRepository.save(fanNote)
    }

    private fun createFanNotePage(
        fanNoteId: Long = savedFanNote.id.value,
        pageNumber: Int = 1,
        imageUrl: String = "https://example.com/page.jpg"
    ): FanNotePage {
        return FanNotePage.create(
            fanNoteId = fanNoteId,
            pageNumber = pageNumber,
            imageUrl = imageUrl
        )
    }

    @Nested
    @DisplayName("save 메서드")
    inner class SaveTest {

        @Test
        @DisplayName("새로운 덕질노트 페이지를 저장한다")
        fun `새로운 덕질노트 페이지를 저장한다`() {
            // given
            val page = createFanNotePage()

            // when
            val savedPage = fanNotePageRepository.save(page)

            // then
            assertThat(savedPage.id.value).isNotEqualTo(0L)
            assertThat(savedPage.fanNoteId).isEqualTo(savedFanNote.id.value)
            assertThat(savedPage.pageNumber).isEqualTo(1)
            assertThat(savedPage.imageUrl).isEqualTo("https://example.com/page.jpg")
            assertThat(savedPage.createdAt).isNotNull()
            assertThat(savedPage.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("존재하지 않는 덕질노트에 페이지 저장 시 예외가 발생한다")
        fun `존재하지 않는 덕질노트에 페이지 저장 시 예외가 발생한다`() {
            // given
            val nonExistentFanNoteId = 999L
            val page = createFanNotePage(fanNoteId = nonExistentFanNoteId)

            // when & then
            val exception = assertThrows<FanNoteException> {
                fanNotePageRepository.save(page)
            }
            assertThat(exception.errorCode).isEqualTo(FanNoteErrorCode.FAN_NOTE_NOT_FOUND)
        }

        @Test
        @DisplayName("기존 덕질노트 페이지를 수정한다")
        fun `기존 덕질노트 페이지를 수정한다`() {
            // given
            val savedPage = fanNotePageRepository.save(createFanNotePage())

            val updatedPage = savedPage.updatePage(pageNumber = 2, imageUrl = "https://example.com/updated-page.jpg")

            // when
            val result = fanNotePageRepository.save(updatedPage)
            flushAndClear()

            // then
            assertThat(result.id).isEqualTo(savedPage.id)
            assertThat(result.pageNumber).isEqualTo(2)
            assertThat(result.imageUrl).isEqualTo("https://example.com/updated-page.jpg")
            assertPersistedUpdate(savedPage.id)
        }

        private fun assertPersistedUpdate(id: FanNotePageId) {
            val persisted = fanNotePageRepository.findById(id)!!
            assertThat(persisted.fanNoteId).isEqualTo(savedFanNote.id.value)
            assertThat(persisted.pageNumber).isEqualTo(2)
            assertThat(persisted.imageUrl).isEqualTo("https://example.com/updated-page.jpg")
        }
    }

    @Nested
    @DisplayName("saveAll 메서드")
    inner class SaveAllTest {

        @Test
        @DisplayName("여러 덕질노트 페이지를 저장한다")
        fun `여러 덕질노트 페이지를 저장한다`() {
            // given
            val pages = listOf(
                createFanNotePage(pageNumber = 1, imageUrl = "https://example.com/page1.jpg"),
                createFanNotePage(pageNumber = 2, imageUrl = "https://example.com/page2.jpg"),
                createFanNotePage(pageNumber = 3, imageUrl = "https://example.com/page3.jpg")
            )

            // when
            val savedPages = fanNotePageRepository.saveAll(pages)

            // then
            assertThat(savedPages).hasSize(3)
            assertThat(savedPages[0].pageNumber).isEqualTo(1)
            assertThat(savedPages[0].imageUrl).isEqualTo("https://example.com/page1.jpg")
            assertThat(savedPages[1].pageNumber).isEqualTo(2)
            assertThat(savedPages[1].imageUrl).isEqualTo("https://example.com/page2.jpg")
            assertThat(savedPages[2].pageNumber).isEqualTo(3)
            assertThat(savedPages[2].imageUrl).isEqualTo("https://example.com/page3.jpg")
        }

        @Test
        @DisplayName("빈 리스트를 저장하면 빈 리스트를 반환한다")
        fun `빈 리스트를 저장하면 빈 리스트를 반환한다`() {
            // given
            val pages = emptyList<FanNotePage>()

            // when
            val savedPages = fanNotePageRepository.saveAll(pages)

            // then
            assertThat(savedPages).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 덕질노트에 페이지들 저장 시 예외가 발생한다")
        fun `존재하지 않는 덕질노트에 페이지들 저장 시 예외가 발생한다`() {
            // given
            val nonExistentFanNoteId = 999L
            val pages = listOf(
                createFanNotePage(fanNoteId = nonExistentFanNoteId, pageNumber = 1),
                createFanNotePage(fanNoteId = nonExistentFanNoteId, pageNumber = 2)
            )

            // when & then
            val exception = assertThrows<FanNoteException> {
                fanNotePageRepository.saveAll(pages)
            }
            assertThat(exception.errorCode).isEqualTo(FanNoteErrorCode.FAN_NOTE_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindByIdTest {

        @Test
        @DisplayName("존재하는 덕질노트 페이지를 조회한다")
        fun `존재하는 덕질노트 페이지를 조회한다`() {
            // given
            val page = createFanNotePage()
            val savedPage = fanNotePageRepository.save(page)

            // when
            val foundPage = fanNotePageRepository.findById(savedPage.id)

            // then
            assertThat(foundPage).isNotNull
            assertThat(foundPage!!.id).isEqualTo(savedPage.id)
            assertThat(foundPage.fanNoteId).isEqualTo(savedFanNote.id.value)
            assertThat(foundPage.pageNumber).isEqualTo(1)
            assertThat(foundPage.imageUrl).isEqualTo("https://example.com/page.jpg")
        }

        @Test
        @DisplayName("존재하지 않는 덕질노트 페이지를 조회하면 null을 반환한다")
        fun `존재하지 않는 덕질노트 페이지를 조회하면 null을 반환한다`() {
            // given
            val nonExistentId = FanNotePageId(999L)

            // when
            val foundPage = fanNotePageRepository.findById(nonExistentId)

            // then
            assertThat(foundPage).isNull()
        }
    }

    @Nested
    @DisplayName("findByFanNoteIdOrderByPageNumber 메서드")
    inner class FindByFanNoteIdOrderByPageNumberTest {

        @Test
        @DisplayName("덕질노트 ID로 페이지를 조회하고 페이지 번호순으로 정렬한다")
        fun `덕질노트 ID로 페이지를 조회하고 페이지 번호순으로 정렬한다`() {
            // given
            val page3 = createFanNotePage(pageNumber = 3, imageUrl = "https://example.com/page3.jpg")
            val page1 = createFanNotePage(pageNumber = 1, imageUrl = "https://example.com/page1.jpg")
            val page2 = createFanNotePage(pageNumber = 2, imageUrl = "https://example.com/page2.jpg")
            fanNotePageRepository.save(page3)
            fanNotePageRepository.save(page1)
            fanNotePageRepository.save(page2)

            // when
            val pages = fanNotePageRepository.findByFanNoteIdOrderByPageNumber(savedFanNote.id)

            // then
            assertThat(pages).hasSize(3)
            assertThat(pages[0].pageNumber).isEqualTo(1)
            assertThat(pages[1].pageNumber).isEqualTo(2)
            assertThat(pages[2].pageNumber).isEqualTo(3)
        }

        @Test
        @DisplayName("존재하지 않는 덕질노트 ID로 조회하면 빈 리스트를 반환한다")
        fun `존재하지 않는 덕질노트 ID로 조회하면 빈 리스트를 반환한다`() {
            // given
            val nonExistentFanNoteId = FanNoteId(999L)

            // when
            val pages = fanNotePageRepository.findByFanNoteIdOrderByPageNumber(nonExistentFanNoteId)

            // then
            assertThat(pages).isEmpty()
        }

        @Test
        @DisplayName("페이지가 없는 덕질노트로 조회하면 빈 리스트를 반환한다")
        fun `페이지가 없는 덕질노트로 조회하면 빈 리스트를 반환한다`() {
            // given - savedFanNote has no pages

            // when
            val pages = fanNotePageRepository.findByFanNoteIdOrderByPageNumber(savedFanNote.id)

            // then
            assertThat(pages).isEmpty()
        }
    }

    @Nested
    @DisplayName("deleteByFanNoteId 메서드")
    inner class DeleteByFanNoteIdTest {

        @Test
        @DisplayName("덕질노트 ID로 모든 페이지를 삭제한다")
        fun `덕질노트 ID로 모든 페이지를 삭제한다`() {
            // given
            val page1 = createFanNotePage(pageNumber = 1)
            val page2 = createFanNotePage(pageNumber = 2)
            val page3 = createFanNotePage(pageNumber = 3)
            fanNotePageRepository.save(page1)
            fanNotePageRepository.save(page2)
            fanNotePageRepository.save(page3)

            // when
            fanNotePageRepository.deleteByFanNoteId(savedFanNote.id)

            // then
            val remainingPages = fanNotePageRepository.findByFanNoteIdOrderByPageNumber(savedFanNote.id)
            assertThat(remainingPages).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 덕질노트 ID로 삭제 시도시 예외가 발생하지 않는다")
        fun `존재하지 않는 덕질노트 ID로 삭제 시도시 예외가 발생하지 않는다`() {
            // given
            val nonExistentFanNoteId = FanNoteId(999L)

            // when & then
            fanNotePageRepository.deleteByFanNoteId(nonExistentFanNoteId)
        }

        @Test
        @DisplayName("다른 덕질노트의 페이지는 삭제되지 않는다")
        fun `다른 덕질노트의 페이지는 삭제되지 않는다`() {
            // given
            val page1 = createFanNotePage(pageNumber = 1)
            fanNotePageRepository.save(page1)

            val anotherFanNote = fanNoteRepository.save(
                FanNote.create(
                    title = "다른 덕질노트",
                    subtitle = null,
                    content = null,
                    productionDate = LocalDate.of(2024, 2, 1),
                    coverImageUrl = null
                )
            )
            val anotherPage = FanNotePage.create(
                fanNoteId = anotherFanNote.id.value,
                pageNumber = 1,
                imageUrl = "https://example.com/another-page.jpg"
            )
            fanNotePageRepository.save(anotherPage)

            // when
            fanNotePageRepository.deleteByFanNoteId(savedFanNote.id)

            // then
            val deletedPages = fanNotePageRepository.findByFanNoteIdOrderByPageNumber(savedFanNote.id)
            val remainingPages = fanNotePageRepository.findByFanNoteIdOrderByPageNumber(anotherFanNote.id)

            assertThat(deletedPages).isEmpty()
            assertThat(remainingPages).hasSize(1)
            assertThat(remainingPages[0].imageUrl).isEqualTo("https://example.com/another-page.jpg")
        }
    }

    @Nested
    @DisplayName("deleteAll 메서드")
    inner class DeleteAllTest {

        @Test
        @DisplayName("지정된 페이지들을 삭제한다")
        fun `지정된 페이지들을 삭제한다`() {
            // given
            val page1 = fanNotePageRepository.save(createFanNotePage(pageNumber = 1))
            val page2 = fanNotePageRepository.save(createFanNotePage(pageNumber = 2))
            val page3 = fanNotePageRepository.save(createFanNotePage(pageNumber = 3))

            val pagesToDelete = listOf(page1, page3)

            // when
            fanNotePageRepository.deleteAll(pagesToDelete)

            // then
            val remainingPages = fanNotePageRepository.findByFanNoteIdOrderByPageNumber(savedFanNote.id)
            assertThat(remainingPages).hasSize(1)
            assertThat(remainingPages[0].pageNumber).isEqualTo(2)
        }

        @Test
        @DisplayName("빈 리스트를 삭제해도 예외가 발생하지 않는다")
        fun `빈 리스트를 삭제해도 예외가 발생하지 않는다`() {
            // given
            val emptyList = emptyList<FanNotePage>()

            // when & then
            fanNotePageRepository.deleteAll(emptyList)
        }

        @Test
        @DisplayName("모든 페이지를 삭제한다")
        fun `모든 페이지를 삭제한다`() {
            // given
            val page1 = fanNotePageRepository.save(createFanNotePage(pageNumber = 1))
            val page2 = fanNotePageRepository.save(createFanNotePage(pageNumber = 2))

            val allPages = listOf(page1, page2)

            // when
            fanNotePageRepository.deleteAll(allPages)

            // then
            val remainingPages = fanNotePageRepository.findByFanNoteIdOrderByPageNumber(savedFanNote.id)
            assertThat(remainingPages).isEmpty()
        }
    }
}
