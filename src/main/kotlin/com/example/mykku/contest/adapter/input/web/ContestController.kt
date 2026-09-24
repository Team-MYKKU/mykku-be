package com.example.mykku.contest.adapter.input.web

import com.example.mykku.auth.config.CurrentMember
import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.contest.application.dto.ContestListQuery
import com.example.mykku.contest.application.port.input.GetContestUseCase
import com.example.mykku.contest.application.port.input.ListContestsUseCase
import com.example.mykku.contest.domain.vo.ContestSortType
import com.example.mykku.contest.domain.vo.ContestStatusFilter
import com.example.mykku.member.domain.entity.Member
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/contests")
class ContestController(
    private val getContestUseCase: GetContestUseCase,
    private val listContestsUseCase: ListContestsUseCase
) {

    @GetMapping
    fun getContests(
        @RequestParam(defaultValue = "ACTIVE") status: ContestStatusFilter,
        @RequestParam(defaultValue = "LATEST") sortType: ContestSortType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<PagedContestsResponse>> {
        val query = ContestListQuery(
            filter = status.listFilter,
            sortType = sortType,
            page = page,
            size = size
        )
        val result = listContestsUseCase.execute(query)
        return ResponseEntity.ok(
            ApiResponse(
                message = "공모전 목록을 성공적으로 조회했습니다.",
                data = PagedContestsResponse.from(result)
            )
        )
    }

    @GetMapping("/{contestId}")
    fun getContestDetail(
        @PathVariable contestId: Long,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<ContestDetailResponse>> {
        val result = getContestUseCase.execute(contestId, member.id.value)
        return ResponseEntity.ok(
            ApiResponse(
                message = "공모전 상세 정보를 성공적으로 조회했습니다.",
                data = ContestDetailResponse.from(result)
            )
        )
    }
}
