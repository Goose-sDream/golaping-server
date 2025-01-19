package com.goosesdream.golaping.vote

import com.goosesdream.golaping.common.websocket.WebSocketManager
import com.goosesdream.golaping.session.service.SessionService
import com.goosesdream.golaping.vote.service.VoteService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.http.MediaType
import org.mockito.Mockito.*
import com.goosesdream.golaping.vote.dto.CreateVoteRequest
import com.goosesdream.golaping.common.base.BaseResponse
import com.goosesdream.golaping.vote.dto.CreateVoteResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "websocket.base-url=ws://localhost:8080",
    "websocket.path=/ws"
])
class VoteControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var voteService: VoteService

    @MockitoBean
    private lateinit var sessionService: SessionService

    @MockitoSpyBean
    private lateinit var webSocketManager: WebSocketManager

    private val objectMapper = jacksonObjectMapper()

    @Test
    @DisplayName("투표 생성 API는 투표를 생성하고 WebSocket URL과 Session ID를 반환해야 한다.")
    fun createVoteControllerTest() {
        val voteRequest = CreateVoteRequest(
            nickname = "testUser",
            title = "Test Vote",
            type = "SINGLE",
            timeLimit = 10,
            userVoteLimit = 1,
            link = "http://example.com/vote/12345"
        )

        val sessionId = UUID.randomUUID().toString()
        val voteUuid = "12345"
        val websocketUrl = "ws://localhost:8080/ws/$voteUuid"

        doNothing().`when`(sessionService).saveNicknameToSession(any(), eq("testUser"), eq(10))
        doNothing().`when`(webSocketManager).startWebSocketForVote(any(), eq(10))
        doNothing().`when`(voteService).createVote(any(), eq(sessionId))

        val result = mockMvc.perform(
            post("/api/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voteRequest))
        )
            .andExpect(status().isOk())  // 200 응답을 기대
            .andExpect(jsonPath("$.isSuccess").value(true))

            .andExpect(jsonPath("$.result.websocketUrl").value("ws://localhost:8080/ws/12345"))

            .andExpect(jsonPath("$.result.sessionId").value(Matchers.matchesPattern("^[0-9a-fA-F-]{36}$")))
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, BaseResponse::class.java)
        val createVoteResponse = objectMapper.convertValue(response.result, CreateVoteResponse::class.java)

        assertThat(createVoteResponse.websocketUrl).isEqualTo(websocketUrl)
        assertThat(createVoteResponse.sessionId).matches("^[0-9a-fA-F-]{36}$")

        verify(sessionService).saveNicknameToSession(any(), eq("testUser"), eq(10))
        verify(webSocketManager).startWebSocketForVote(eq(voteUuid), eq(10))
        verify(voteService).createVote(eq(voteRequest), eq(voteUuid))
    }
}
