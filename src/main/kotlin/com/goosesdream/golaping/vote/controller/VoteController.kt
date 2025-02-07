package com.goosesdream.golaping.vote.controller

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.base.BaseResponse
import com.goosesdream.golaping.common.constants.RequestURI.Companion.VOTES
import com.goosesdream.golaping.common.enums.BaseResponseStatus.*
import com.goosesdream.golaping.websocket.service.WebSocketManager
import com.goosesdream.golaping.session.service.SessionService
import com.goosesdream.golaping.user.service.UserService
import com.goosesdream.golaping.vote.dto.*
import com.goosesdream.golaping.vote.service.VoteService
import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@Hidden
@RestController
@RequestMapping(VOTES)
class VoteController(
    private val voteService: VoteService,
    private val sessionService: SessionService,
    private val webSocketManager: WebSocketManager,
    private val userService: UserService) : VoteControllerInterface {

    @Value("\${websocket.base-url}")
    private lateinit var websocketBaseUrl: String

    @Value("\${websocket.path}")
    private lateinit var websocketPath: String

    @PostMapping
    override fun createVote(
        @RequestBody voteRequest: CreateVoteRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): BaseResponse<CreateVoteResponse> {
        val sessionId = UUID.randomUUID().toString()
        sessionService.saveCreatorNicknameToSession(sessionId, voteRequest.nickname, voteRequest.timeLimit)

        val voteUuid = URI(voteRequest.link).path.split("/").last() // uuid 형식
        val websocketUrl = "$websocketBaseUrl$websocketPath"

        voteService.saveVoteExpirationToRedis(voteUuid, voteRequest.timeLimit)
        webSocketManager.startWebSocketForVote(voteUuid, voteRequest.timeLimit)

        val creator = userService.createUserForVote(voteRequest.nickname)
        val voteIdx = voteService.createVote(voteRequest, voteUuid, creator)
        userService.addParticipant(creator, voteUuid)

        setCookie(sessionId, voteRequest.timeLimit, response)

        return BaseResponse(CreateVoteResponse(websocketUrl, voteIdx, voteUuid)
        )
    }

    @PostMapping("/enter")
    override fun enterVote(
        @RequestBody voteRequest: EnterVoteRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): BaseResponse<EnterVoteResponse> {
        val sessionId = UUID.randomUUID().toString()

        val voteEndTime = voteService.getVoteEndTime(voteRequest.voteUuid)
        val currentTime = LocalDateTime.now()
        val timeLimit = Duration.between(currentTime, voteEndTime).toMinutes().toInt()
        sessionService.saveNicknameToSession(
            sessionId,
            voteRequest.nickname,
            timeLimit
        )

        val user = userService.createUser(voteRequest.nickname, voteRequest.voteUuid)
        userService.addParticipant(user, voteRequest.voteUuid)
        val websocketUrl = "$websocketBaseUrl$websocketPath"

        setCookie(sessionId, timeLimit, response)

        return BaseResponse(EnterVoteResponse(websocketUrl, voteEndTime))
    }

    private fun setCookie(
        sessionId: String,
        timeLimit: Int,
        response: HttpServletResponse
    ) {
        val cookie = Cookie("SESSIONID", sessionId).apply { // TODO: https 설정 후 secure 속성 추가
            isHttpOnly = true
            path = "/"
            maxAge = timeLimit * 60
        }
        response.addCookie(cookie)

    //  response.setHeader( // TODO: https 설정 후 헤더 설정 추가
    //      "Set-Cookie",
    //      "SESSIONID=$sessionId; Path=/; Max-Age=${timeLimit * 60}; HttpOnly; SameSite=None; Secure"
    //  )
    }

    @GetMapping("/{voteIdx}/result")
    override fun getVoteResult(@PathVariable voteIdx: Long): BaseResponse<VoteResultResponse> {
        val vote = voteService.getVoteByVoteIdx(voteIdx) ?: throw BaseException(VOTE_NOT_FOUND)
        val voteResults = voteService.getVoteResults(voteIdx)

        return BaseResponse(VoteResultResponse(vote.title, voteResults))
    }
}

