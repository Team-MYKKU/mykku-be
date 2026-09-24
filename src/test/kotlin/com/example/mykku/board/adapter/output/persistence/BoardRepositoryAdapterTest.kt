package com.example.mykku.board.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.entity.Board
import com.example.mykku.board.domain.vo.BoardId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("BoardRepositoryAdapter 통합 테스트")
class BoardRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var boardRepository: BoardRepository

    private fun createTestBoard(
        title: String = "테스트 게시판",
        logo: String = "test_logo.png"
    ): Board {
        return Board.create(
            title = title,
            logo = logo
        )
    }

    @Nested
    @DisplayName("save 메서드는")
    inner class SaveMethod {

        @Test
        @DisplayName("새로운 게시판을 저장하고 ID가 부여된 게시판을 반환한다")
        fun saveNewBoard() {
            val board = createTestBoard()

            val savedBoard = boardRepository.save(board)

            assertThat(savedBoard.id.value).isGreaterThan(0L)
            assertThat(savedBoard.title).isEqualTo(board.title)
            assertThat(savedBoard.logo).isEqualTo(board.logo)
            assertThat(savedBoard.createdAt).isNotNull()
            assertThat(savedBoard.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("여러 게시판을 저장하면 각각 다른 ID가 부여된다")
        fun saveMultipleBoardsWithDifferentIds() {
            val board1 = createTestBoard(title = "게시판1", logo = "logo1.png")
            val board2 = createTestBoard(title = "게시판2", logo = "logo2.png")

            val savedBoard1 = boardRepository.save(board1)
            val savedBoard2 = boardRepository.save(board2)

            assertThat(savedBoard1.id.value).isNotEqualTo(savedBoard2.id.value)
            assertThat(savedBoard1.title).isEqualTo("게시판1")
            assertThat(savedBoard2.title).isEqualTo("게시판2")
        }

        @Test
        @DisplayName("기존 게시판을 저장하면 제목이 갱신되고 createdAt이 보존된다")
        fun saveExistingBoardUpdatesInPlace() {
            val saved = boardRepository.save(createTestBoard(title = "원본"))
            boardJpaRepository.flush()
            Thread.sleep(5)
            val changed = Board.reconstitute(
                id = saved.id,
                title = "변경됨",
                logo = saved.logo,
                createdAt = saved.createdAt,
                updatedAt = saved.updatedAt
            )

            val updated = boardRepository.save(changed)

            assertThat(updated.id).isEqualTo(saved.id)
            assertThat(updated.title).isEqualTo("변경됨")
            assertThat(updated.createdAt).isEqualTo(saved.createdAt)
            assertThat(boardJpaRepository.count()).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("findById 메서드는")
    inner class FindByIdMethod {

        @Test
        @DisplayName("존재하는 게시판 ID로 조회하면 게시판을 반환한다")
        fun findExistingBoardById() {
            val board = createTestBoard(title = "조회 테스트", logo = "find_logo.png")
            val savedBoard = boardRepository.save(board)

            val foundBoard = boardRepository.findById(savedBoard.id)

            assertThat(foundBoard).isNotNull
            assertThat(foundBoard!!.id.value).isEqualTo(savedBoard.id.value)
            assertThat(foundBoard.title).isEqualTo("조회 테스트")
            assertThat(foundBoard.logo).isEqualTo("find_logo.png")
        }

        @Test
        @DisplayName("존재하지 않는 게시판 ID로 조회하면 null을 반환한다")
        fun findNonExistingBoardById() {
            val foundBoard = boardRepository.findById(BoardId.of(99999L))

            assertThat(foundBoard).isNull()
        }
    }

    @Nested
    @DisplayName("findAll 메서드는")
    inner class FindAllMethod {

        @Test
        @DisplayName("저장된 모든 게시판을 반환한다")
        fun findAllBoards() {
            val board1 = createTestBoard(title = "게시판A", logo = "logoA.png")
            val board2 = createTestBoard(title = "게시판B", logo = "logoB.png")
            val board3 = createTestBoard(title = "게시판C", logo = "logoC.png")
            boardRepository.save(board1)
            boardRepository.save(board2)
            boardRepository.save(board3)

            val allBoards = boardRepository.findAll()

            assertThat(allBoards).hasSize(3)
            assertThat(allBoards.map { it.title }).containsExactlyInAnyOrder("게시판A", "게시판B", "게시판C")
        }

        @Test
        @DisplayName("저장된 게시판이 없으면 빈 리스트를 반환한다")
        fun findAllReturnsEmptyListWhenNoBoards() {
            val allBoards = boardRepository.findAll()

            assertThat(allBoards).isEmpty()
        }
    }

    @Nested
    @DisplayName("delete 메서드는")
    inner class DeleteMethod {

        @Test
        @DisplayName("게시판을 삭제하면 더 이상 조회되지 않는다")
        fun deleteBoard() {
            val board = createTestBoard()
            val savedBoard = boardRepository.save(board)

            boardRepository.delete(savedBoard)

            val foundBoard = boardRepository.findById(savedBoard.id)
            assertThat(foundBoard).isNull()
        }

        @Test
        @DisplayName("게시판을 삭제하면 전체 조회 시에도 포함되지 않는다")
        fun deleteBoardNotInFindAll() {
            val board1 = createTestBoard(title = "남은 게시판", logo = "remain.png")
            val board2 = createTestBoard(title = "삭제할 게시판", logo = "delete.png")
            val savedBoard1 = boardRepository.save(board1)
            val savedBoard2 = boardRepository.save(board2)

            boardRepository.delete(savedBoard2)

            val allBoards = boardRepository.findAll()
            assertThat(allBoards).hasSize(1)
            assertThat(allBoards[0].title).isEqualTo("남은 게시판")
        }
    }

    @Nested
    @DisplayName("existsById 메서드는")
    inner class ExistsByIdMethod {

        @Test
        @DisplayName("존재하는 게시판 ID이면 true를 반환한다")
        fun existingBoardIdReturnsTrue() {
            val board = createTestBoard()
            val savedBoard = boardRepository.save(board)

            val exists = boardRepository.existsById(savedBoard.id)

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 게시판 ID이면 false를 반환한다")
        fun nonExistingBoardIdReturnsFalse() {
            val exists = boardRepository.existsById(BoardId.of(99999L))

            assertThat(exists).isFalse()
        }

        @Test
        @DisplayName("삭제된 게시판 ID이면 false를 반환한다")
        fun deletedBoardIdReturnsFalse() {
            val board = createTestBoard()
            val savedBoard = boardRepository.save(board)
            boardRepository.delete(savedBoard)

            val exists = boardRepository.existsById(savedBoard.id)

            assertThat(exists).isFalse()
        }
    }
}
