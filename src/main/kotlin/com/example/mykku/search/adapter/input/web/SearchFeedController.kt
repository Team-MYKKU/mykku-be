package com.example.mykku.search.adapter.input.web

import com.example.mykku.auth.config.CurrentMember
import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.common.util.PageableValidator
import com.example.mykku.feed.adapter.input.web.dto.FeedSearchResponse
import com.example.mykku.feed.adapter.input.web.dto.PagedFeedSearchResponse
import com.example.mykku.feed.application.dto.SearchBoardFeedsQuery
import com.example.mykku.feed.application.dto.SearchFeedsQuery
import com.example.mykku.feed.application.port.input.SearchBoardFeedsUseCase
import com.example.mykku.feed.application.port.input.SearchFeedsUseCase
import com.example.mykku.member.domain.entity.Member
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/search/feeds")
class SearchFeedController(
    private val searchFeedsUseCase: SearchFeedsUseCase,
    private val searchBoardFeedsUseCase: SearchBoardFeedsUseCase
) {

    @GetMapping
    fun searchFeeds(
        @RequestParam keyword: String,
        @CurrentMember(required = false) member: Member?
    ): ResponseEntity<ApiResponse<FeedSearchResponse>> {
        val query = SearchFeedsQuery(keyword = keyword, memberId = member?.id?.value)
        val result = searchFeedsUseCase.execute(query)
        return ResponseEntity.ok(
            ApiResponse(
                message = "피드 검색 결과를 성공적으로 조회했습니다.",
                data = FeedSearchResponse.from(result)
            )
        )
    }

    @GetMapping("/boards/{boardId}")
    fun searchBoardFeeds(
        @PathVariable boardId: Long,
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @CurrentMember(required = false) member: Member?
    ): ResponseEntity<ApiResponse<PagedFeedSearchResponse>> {
        val pageable = PageableValidator.validateAndCreate(page, size)
        val query = SearchBoardFeedsQuery(
            keyword = keyword,
            boardId = boardId,
            memberId = member?.id?.value,
            pageable = pageable
        )
        val result = searchBoardFeedsUseCase.execute(query)
        return ResponseEntity.ok(
            ApiResponse(
                message = "게시판 피드 검색 결과를 성공적으로 조회했습니다.",
                data = PagedFeedSearchResponse.from(result)
            )
        )
    }
}
