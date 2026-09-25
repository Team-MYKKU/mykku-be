package com.example.mykku.feed.domain.vo

import com.example.mykku.feed.exception.FeedErrorCode
import com.example.mykku.feed.exception.FeedException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("SearchKeyword 값 객체 테스트")
class SearchKeywordTest {

    @Nested
    @DisplayName("of 메서드")
    inner class Of {

        @Test
        @DisplayName("앞뒤 공백과 줄바꿈을 제거한다")
        fun `검색어 생성 - 앞뒤 공백 제거`() {
            val keyword = SearchKeyword.of("  아이브 컴백\n")

            assertThat(keyword.value).isEqualTo("아이브 컴백")
        }

        @Test
        @DisplayName("내부 공백은 유지한다")
        fun `검색어 생성 - 내부 공백 유지`() {
            val keyword = SearchKeyword.of("아이브  컴백")

            assertThat(keyword.value).isEqualTo("아이브  컴백")
        }

        @Test
        @DisplayName("소문자로 변환한다")
        fun `검색어 생성 - 소문자 변환`() {
            val keyword = SearchKeyword.of("IVE Comeback")

            assertThat(keyword.value).isEqualTo("ive comeback")
        }

        @Test
        @DisplayName("1글자 검색어를 허용한다")
        fun `검색어 생성 - 1글자`() {
            val keyword = SearchKeyword.of("a")

            assertThat(keyword.value).isEqualTo("a")
        }

        @Test
        @DisplayName("50자 검색어를 허용한다")
        fun `검색어 생성 - 50자`() {
            val raw = "a".repeat(SearchKeyword.MAX_LENGTH)

            val keyword = SearchKeyword.of(raw)

            assertThat(keyword.value).isEqualTo(raw)
        }

        @Test
        @DisplayName("앞뒤 공백을 제거한 뒤 길이를 검사한다")
        fun `검색어 생성 - 공백 제거 후 50자`() {
            val raw = "  " + "a".repeat(SearchKeyword.MAX_LENGTH) + "  "

            val keyword = SearchKeyword.of(raw)

            assertThat(keyword.value).hasSize(SearchKeyword.MAX_LENGTH)
        }

        @Test
        @DisplayName("빈 문자열이면 예외가 발생한다")
        fun `검색어 생성 - 빈 문자열`() {
            val exception = assertThrows<FeedException> {
                SearchKeyword.of("")
            }

            assertThat(exception.errorCode).isEqualTo(FeedErrorCode.SEARCH_KEYWORD_EMPTY)
        }

        @Test
        @DisplayName("공백만 있으면 예외가 발생한다")
        fun `검색어 생성 - 공백만`() {
            val exception = assertThrows<FeedException> {
                SearchKeyword.of("  \n\t ")
            }

            assertThat(exception.errorCode).isEqualTo(FeedErrorCode.SEARCH_KEYWORD_EMPTY)
        }

        @Test
        @DisplayName("51자면 예외가 발생한다")
        fun `검색어 생성 - 51자`() {
            val exception = assertThrows<FeedException> {
                SearchKeyword.of("a".repeat(SearchKeyword.MAX_LENGTH + 1))
            }

            assertThat(exception.errorCode).isEqualTo(FeedErrorCode.SEARCH_KEYWORD_TOO_LONG)
        }

        @Test
        @DisplayName("길이는 UTF-16 단위로 센다")
        fun `검색어 생성 - 이모지 길이`() {
            val exception = assertThrows<FeedException> {
                SearchKeyword.of("💜".repeat(26))
            }

            assertThat(exception.errorCode).isEqualTo(FeedErrorCode.SEARCH_KEYWORD_TOO_LONG)
        }
    }
}
