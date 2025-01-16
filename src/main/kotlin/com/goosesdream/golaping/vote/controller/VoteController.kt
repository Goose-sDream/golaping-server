package com.goosesdream.golaping.vote.controller

import com.goosesdream.golaping.common.base.BaseResponse
import com.goosesdream.golaping.common.constants.RequestURI.Companion.VOTES
import com.goosesdream.golaping.common.websocket.WebSocketManager
import com.goosesdream.golaping.session.service.SessionService
import com.goosesdream.golaping.vote.dto.CreateVoteRequest
import com.goosesdream.golaping.vote.dto.CreateVoteResponse
import com.goosesdream.golaping.vote.service.VoteService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*


@RestController
@RequestMapping(VOTES)
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
    fun createVote(
        @RequestBody voteRequest: CreateVoteRequest,
        request: HttpServletRequest
    ): BaseResponse<CreateVoteResponse> {
        val sessionId = UUID.randomUUID().toString()
        sessionService.saveNicknameToSession(sessionId, voteRequest.nickname, voteRequest.timeLimit)

        val voteUuid = URI(voteRequest.link).path.split("/").last() // uuid 형식
        webSocketManager.startWebSocketForVote(voteUuid, voteRequest.timeLimit)

        voteService.createVote(voteRequest, voteUuid)

        val websocketUrl = generateWebSocketUrl(voteUuid)
        return BaseResponse(CreateVoteResponse(websocketUrl, sessionId))
    }

    fun generateWebSocketUrl(voteUuid: String): String {
        return "$websocketBaseUrl$websocketPath/$voteUuid"
    }

    // 투표 접속
//    @GetMapping("/vote")
//    fun votePage(request: HttpServletRequest): String {
//        val session = request.getSession(false)
//        if (session?.getAttribute("nickname") == null) {
//            return "redirect:/nickname-input" // 닉네임 입력 페이지로 리디렉션
//        }
//        return "vote" // 투표 화면으로 이동
//    }

}

