package com.goosesdream.golaping.vote.controller

import com.goosesdream.golaping.common.base.BaseResponse
import com.goosesdream.golaping.common.constants.RequestURI.Companion.VOTES
import com.goosesdream.golaping.common.websocket.WebSocketManager
import com.goosesdream.golaping.session.service.SessionService
import com.goosesdream.golaping.user.service.UserService
import com.goosesdream.golaping.vote.dto.CreateVoteRequest
import com.goosesdream.golaping.vote.dto.CreateVoteResponse
import com.goosesdream.golaping.vote.dto.EnterVoteRequest
import com.goosesdream.golaping.vote.dto.EnterVoteResponse
import com.goosesdream.golaping.vote.service.VoteService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping(VOTES)
@Tag(name = "Vote", description = "투표 관련 API")
class VoteController(
    private val voteService: VoteService,
    private val sessionService: SessionService,
    private val webSocketManager: WebSocketManager,
    private val userService: UserService
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
        sessionService.saveCreatorNicknameToSession(sessionId, voteRequest.nickname, voteRequest.timeLimit)

        val voteUuid = URI(voteRequest.link).path.split("/").last() // uuid 형식

        voteService.saveVoteExpirationToRedis(voteUuid, voteRequest.timeLimit)
        webSocketManager.startWebSocketForVote(voteUuid, voteRequest.timeLimit)

        val creator = userService.createUserForVote(voteRequest.nickname)
        voteService.createVote(voteRequest, voteUuid, creator)
        userService.addParticipant(creator, voteUuid)

        val websocketUrl = generateWebSocketUrl(voteUuid)
        return BaseResponse(CreateVoteResponse(websocketUrl, sessionId)) //TODO: sessionId 쿠키에 담아 반환하도록 수정
    }

    fun generateWebSocketUrl(voteUuid: String): String {
        return "$websocketBaseUrl$websocketPath/$voteUuid"
    }

    @PostMapping("/enter")
    @Operation(summary = "닉네임 입력", description = "닉네임을 입력받고, websocketUrl과 SessionID, voteEndTime을 반환한다.")
    fun enterVote(
        @RequestBody voteRequest: EnterVoteRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): BaseResponse<EnterVoteResponse> {
        val sessionId = UUID.randomUUID().toString()

        val voteEndTime = voteService.getVoteEndTime(voteRequest.voteUuid)
        val currentTime = LocalDateTime.now()
        val timeLimit = Duration.between(currentTime, voteEndTime).toMinutes().toInt()
        sessionService.saveNicknameToSession(sessionId, voteRequest.nickname, timeLimit) // 접속한 유저의 sessionId와 nickname 저장

        val user = userService.createUser(voteRequest.nickname, voteRequest.voteUuid)
        userService.addParticipant(user, voteRequest.voteUuid)

        val websocketUrl = generateWebSocketUrl(voteRequest.voteUuid)

        val cookie = Cookie("SESSIONID", sessionId)
        cookie.isHttpOnly = true
        cookie.path = "/"
        cookie.maxAge = timeLimit * 60
        response.addCookie(cookie)

        return BaseResponse(EnterVoteResponse(websocketUrl, voteEndTime))
    }
}

