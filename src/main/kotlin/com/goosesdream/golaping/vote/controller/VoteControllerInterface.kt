package com.goosesdream.golaping.vote.controller

import com.goosesdream.golaping.common.base.BaseResponse
import com.goosesdream.golaping.common.exception.HttpErrorResponse
import com.goosesdream.golaping.vote.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.*

@Tag(name = "Vote", description = "투표 관련 API")
interface VoteControllerInterface {

    @Operation(
        summary = "투표 생성",
        description = "새로운 투표를 생성하고, WebSocket URL, SessionID, voteIdx, voteUuid, voteEndTime을 반환한다.",
        responses = [
            ApiResponse(responseCode = "200", description = "투표 생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청", content = [Content(schema = Schema(implementation = HttpErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "서버 내부 오류", content = [Content(schema = Schema(implementation = HttpErrorResponse::class))])
        ]
    )
    @PostMapping
    fun createVote(
        @RequestBody voteRequest: CreateVoteRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): BaseResponse<CreateVoteResponse>


    @Operation(
        summary = "닉네임 입력",
        description = "닉네임을 입력받고, WebSocket URL과 SessionID, voteEndTime, voteIdx를 반환한다.",
        responses = [
            ApiResponse(responseCode = "200", description = "닉네임 입력 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청", content = [Content(schema = Schema(implementation = HttpErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "투표를 찾을 수 없음", content = [Content(schema = Schema(implementation = HttpErrorResponse::class))])
        ]
    )
    @PostMapping("/enter")
    fun enterVote(
        @RequestBody voteRequest: EnterVoteRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): BaseResponse<EnterVoteResponse>


    @Operation(
        summary = "투표 결과 조회",
        description = "투표 결과를 조회한다.",
        responses = [
            ApiResponse(responseCode = "200", description = "투표 결과 조회 성공"),
            ApiResponse(responseCode = "404", description = "투표를 찾을 수 없음", content = [Content(schema = Schema(implementation = HttpErrorResponse::class))])
        ]
    )
    @GetMapping("/{voteIdx}/result")
    fun getVoteResult(@PathVariable voteIdx: Long): BaseResponse<VoteResultResponse>
}
