package com.example.mykku.contest.domain

import com.example.mykku.contest.domain.entity.ContestTag
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestTagId
import com.example.mykku.contest.exception.ContestErrorCode
import com.example.mykku.contest.exception.ContestException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("ContestTag 도메인 엔티티 테스트")
class ContestTagTest {

    @Nested
    @DisplayName("create 메서드")
    inner class Create {

        @Test
        @DisplayName("정상적으로 콘테스트 태그를 생성한다")
        fun `콘테스트 태그 생성 - 정상 케이스`() {
            val title = "테스트태그"
            val contestId = ContestId(1L)

            val contestTag = ContestTag.create(
                title = title,
                contestId = contestId
            )

            assertThat(contestTag.id.value).isEqualTo(0L)
            assertThat(contestTag.title).isEqualTo(title)
            assertThat(contestTag.contestId).isEqualTo(contestId)
        }

        @Test
        @DisplayName("콘테스트 태그 생성시 createdAt과 updatedAt이 설정된다")
        fun `콘테스트 태그 생성 - 시간 설정 검증`() {
            val contestTag = createContestTag()

            assertThat(contestTag.createdAt).isNotNull()
            assertThat(contestTag.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("콘테스트 태그 생성시 createdAt과 updatedAt이 동일하다")
        fun `콘테스트 태그 생성 - 생성수정 시간 동일`() {
            val contestTag = createContestTag()

            assertThat(contestTag.createdAt).isEqualTo(contestTag.updatedAt)
        }

        @Test
        @DisplayName("한글 태그를 생성할 수 있다")
        fun `콘테스트 태그 생성 - 한글`() {
            val contestTag = ContestTag.create(
                title = "한글태그",
                contestId = ContestId(1L)
            )

            assertThat(contestTag.title).isEqualTo("한글태그")
        }

        @Test
        @DisplayName("영문 태그를 생성할 수 있다")
        fun `콘테스트 태그 생성 - 영문`() {
            val contestTag = ContestTag.create(
                title = "EnglishTag",
                contestId = ContestId(1L)
            )

            assertThat(contestTag.title).isEqualTo("EnglishTag")
        }

        @Test
        @DisplayName("숫자 태그를 생성할 수 있다")
        fun `콘테스트 태그 생성 - 숫자`() {
            val contestTag = ContestTag.create(
                title = "12345",
                contestId = ContestId(1L)
            )

            assertThat(contestTag.title).isEqualTo("12345")
        }

        @Test
        @DisplayName("한글, 영문, 숫자 혼합 태그를 생성할 수 있다")
        fun `콘테스트 태그 생성 - 혼합`() {
            val contestTag = ContestTag.create(
                title = "태그Tag123",
                contestId = ContestId(1L)
            )

            assertThat(contestTag.title).isEqualTo("태그Tag123")
        }

        @Test
        @DisplayName("최대 길이(20자) 태그를 생성할 수 있다")
        fun `콘테스트 태그 생성 - 최대 길이`() {
            val maxLengthTitle = "가".repeat(ContestTag.TITLE_MAX_LENGTH)

            val contestTag = ContestTag.create(
                title = maxLengthTitle,
                contestId = ContestId(1L)
            )

            assertThat(contestTag.title).hasSize(ContestTag.TITLE_MAX_LENGTH)
        }
    }

    @Nested
    @DisplayName("create 메서드 - 유효성 검증 실패")
    inner class CreateValidationFailure {

        @Test
        @DisplayName("태그 제목이 최대 길이를 초과하면 예외가 발생한다")
        fun `유효성 검증 실패 - 제목 길이 초과`() {
            val tooLongTitle = "가".repeat(ContestTag.TITLE_MAX_LENGTH + 1)

            val exception = assertThrows<ContestException> {
                ContestTag.create(
                    title = tooLongTitle,
                    contestId = ContestId(1L)
                )
            }

            assertThat(exception.errorCode).isEqualTo(ContestErrorCode.TAG_TITLE_TOO_LONG)
        }

        @Test
        @DisplayName("특수문자가 포함된 태그를 생성하면 예외가 발생한다")
        fun `유효성 검증 실패 - 특수문자 포함`() {
            val exception = assertThrows<ContestException> {
                ContestTag.create(
                    title = "태그@특수",
                    contestId = ContestId(1L)
                )
            }

            assertThat(exception.errorCode).isEqualTo(ContestErrorCode.TAG_INVALID_FORMAT)
        }

        @Test
        @DisplayName("공백이 포함된 태그를 생성하면 예외가 발생한다")
        fun `유효성 검증 실패 - 공백 포함`() {
            val exception = assertThrows<ContestException> {
                ContestTag.create(
                    title = "태그 공백",
                    contestId = ContestId(1L)
                )
            }

            assertThat(exception.errorCode).isEqualTo(ContestErrorCode.TAG_INVALID_FORMAT)
        }

        @Test
        @DisplayName("하이픈이 포함된 태그를 생성하면 예외가 발생한다")
        fun `유효성 검증 실패 - 하이픈 포함`() {
            val exception = assertThrows<ContestException> {
                ContestTag.create(
                    title = "태그-하이픈",
                    contestId = ContestId(1L)
                )
            }

            assertThat(exception.errorCode).isEqualTo(ContestErrorCode.TAG_INVALID_FORMAT)
        }

        @Test
        @DisplayName("언더스코어가 포함된 태그를 생성하면 예외가 발생한다")
        fun `유효성 검증 실패 - 언더스코어 포함`() {
            val exception = assertThrows<ContestException> {
                ContestTag.create(
                    title = "태그_언더스코어",
                    contestId = ContestId(1L)
                )
            }

            assertThat(exception.errorCode).isEqualTo(ContestErrorCode.TAG_INVALID_FORMAT)
        }
    }

    @Nested
    @DisplayName("reconstitute 메서드")
    inner class Reconstitute {

        @Test
        @DisplayName("저장된 데이터로 ContestTag를 복원한다")
        fun `복원 - 정상 케이스`() {
            val now = LocalDateTime.now()
            val id = ContestTagId(1L)
            val title = "복원태그"
            val contestId = ContestId(10L)

            val contestTag = ContestTag.reconstitute(
                id = id,
                title = title,
                contestId = contestId,
                createdAt = now.minusDays(1),
                updatedAt = now
            )

            assertThat(contestTag.id).isEqualTo(id)
            assertThat(contestTag.title).isEqualTo(title)
            assertThat(contestTag.contestId).isEqualTo(contestId)
            assertThat(contestTag.createdAt).isEqualTo(now.minusDays(1))
            assertThat(contestTag.updatedAt).isEqualTo(now)
        }

        @Test
        @DisplayName("복원시 createdAt과 updatedAt이 다를 수 있다")
        fun `복원 - 생성수정 시간이 다른 경우`() {
            val createdAt = LocalDateTime.of(2024, 1, 1, 10, 0)
            val updatedAt = LocalDateTime.of(2024, 1, 15, 15, 30)

            val contestTag = ContestTag.reconstitute(
                id = ContestTagId(1L),
                title = "테스트태그",
                contestId = ContestId(1L),
                createdAt = createdAt,
                updatedAt = updatedAt
            )

            assertThat(contestTag.createdAt).isNotEqualTo(contestTag.updatedAt)
            assertThat(contestTag.createdAt).isBefore(contestTag.updatedAt)
        }

        @Test
        @DisplayName("복원시 유효성 검증을 수행하지 않는다")
        fun `복원 - 유효성 검증 없음`() {
            val now = LocalDateTime.now()

            val contestTag = ContestTag.reconstitute(
                id = ContestTagId(1L),
                title = "특수@문자#포함",
                contestId = ContestId(1L),
                createdAt = now,
                updatedAt = now
            )

            assertThat(contestTag.title).isEqualTo("특수@문자#포함")
        }
    }

    @Nested
    @DisplayName("상수 검증")
    inner class Constants {

        @Test
        @DisplayName("TITLE_MAX_LENGTH가 20이다")
        fun `상수 검증 - TITLE_MAX_LENGTH`() {
            assertThat(ContestTag.TITLE_MAX_LENGTH).isEqualTo(20)
        }

        @Test
        @DisplayName("VALID_PATTERN이 한글, 영문, 숫자만 허용한다")
        fun `상수 검증 - VALID_PATTERN`() {
            val pattern = ContestTag.VALID_PATTERN

            assertThat(pattern.matches("한글")).isTrue()
            assertThat(pattern.matches("English")).isTrue()
            assertThat(pattern.matches("12345")).isTrue()
            assertThat(pattern.matches("한글English123")).isTrue()

            assertThat(pattern.matches("특수@문자")).isFalse()
            assertThat(pattern.matches("공백 포함")).isFalse()
            assertThat(pattern.matches("")).isFalse()
        }
    }

    private fun createContestTag(): ContestTag {
        return ContestTag.create(
            title = "테스트태그",
            contestId = ContestId(1L)
        )
    }

    @Nested
    @DisplayName("normalizeAndValidate 메서드")
    inner class NormalizeAndValidate {

        @Test
        @DisplayName("앞뒤 공백을 지우고 빈 값과 중복을 뺀 뒤 입력 순서를 유지한다")
        fun `태그 정규화 - trim, 빈 값 제거, 순서 유지 중복 제거`() {
            val result = ContestTag.normalizeAndValidate(listOf(" 팬아트", "", "일러스트", "팬아트 "))

            assertThat(result).containsExactly("팬아트", "일러스트")
        }

        @Test
        @DisplayName("정규화 후 태그가 없으면 CONTEST_TAG_REQUIRED 예외가 발생한다")
        fun `태그 정규화 - 태그 없음`() {
            val exception = assertThrows<ContestException> {
                ContestTag.normalizeAndValidate(listOf(" ", ""))
            }

            assertThat(exception.errorCode).isEqualTo(ContestErrorCode.CONTEST_TAG_REQUIRED)
        }

        @Test
        @DisplayName("태그가 최대 개수를 넘으면 CONTEST_TAG_LIMIT_EXCEEDED 예외가 발생한다")
        fun `태그 정규화 - 개수 초과`() {
            val titles = (1..8).map { "태그$it" }

            val exception = assertThrows<ContestException> {
                ContestTag.normalizeAndValidate(titles)
            }

            assertThat(exception.errorCode).isEqualTo(ContestErrorCode.CONTEST_TAG_LIMIT_EXCEEDED)
        }

        @Test
        @DisplayName("형식이 잘못된 태그가 있으면 TAG_INVALID_FORMAT 예외가 발생한다")
        fun `태그 정규화 - 형식 오류`() {
            val exception = assertThrows<ContestException> {
                ContestTag.normalizeAndValidate(listOf("#팬아트"))
            }

            assertThat(exception.errorCode).isEqualTo(ContestErrorCode.TAG_INVALID_FORMAT)
        }
    }
}
