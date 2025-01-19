package com.goosesdream.golaping.vote.controller

import com.goosesdream.golaping.common.base.BaseResponse
import com.goosesdream.golaping.common.constants.RequestURI.Companion.VOTES
import com.goosesdream.golaping.common.websocket.WebSocketManager
import com.goosesdream.golaping.session.service.SessionService
import com.goosesdream.golaping.vote.dto.CreateVoteRequest
import com.goosesdream.golaping.vote.dto.CreateVoteResponse
import com.goosesdream.golaping.vote.service.VoteService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping(VOTES)
@Tag(name = "Vote", description = "투표 관련 API")
class VoteController(
    private val voteService: VoteService,
    private val sessionService: SessionService,
    private val webSocketManager: WebSocketManager
) {
    @Value("\${websocket.base-url}")
    private lateinit var websocketBaseUrl: String

    @Value("\${websocket.path}")
    private lateinit var websocketPath: String

    @PostMapping
    @Operation(summary = "투표 생성", description = "새로운 투표를 생성하고, WebSocket URL과 SessionID를 반환한다.")
    fun createVote(
        @RequestBody voteRequest: CreateVoteRequest,
        request: HttpServletRequest
    ): BaseResponse<CreateVoteResponse> {
        val sessionId = UUID.randomUUID().toString()
        sessionService.saveNicknameToSession(sessionId, voteRequest.nickname, voteRequest.timeLimit)

        val voteUuid = URI(voteRequest.link).path.split("/").last() // uuid 형식

        voteService.saveVoteExpirationToRedis(voteUuid, voteRequest.timeLimit)
        webSocketManager.startWebSocketForVote(voteUuid, voteRequest.timeLimit)

        voteService.createVote(voteRequest, voteUuid)

        val websocketUrl = generateWebSocketUrl(voteUuid)
        return BaseResponse(CreateVoteResponse(websocketUrl, sessionId))
    }

    fun generateWebSocketUrl(voteUuid: String): String {
        return "$websocketBaseUrl$websocketPath/$voteUuid"
    }
}

