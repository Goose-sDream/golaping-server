package com.goosesdream.golaping.vote

import com.goosesdream.golaping.websocket.service.WebSocketManager
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
import com.goosesdream.golaping.user.entity.Users
import com.goosesdream.golaping.user.service.UserService
import org.assertj.core.api.Assertions.assertThat
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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "websocket.base-url=http://localhost:8080",
    "websocket.path=/ws/votes"
])
class VoteControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var voteService: VoteService

    @MockitoBean
    private lateinit var sessionService: SessionService

    @MockitoBean
    private lateinit var userService: UserService

    @MockitoSpyBean
    private lateinit var webSocketManager: WebSocketManager

    private val objectMapper = jacksonObjectMapper()

    @Test
    @DisplayName("투표 생성 API는 투표를 생성하고 WebSocket URL을 반환하며, 세션 ID를 쿠키에 저장해야 한다.")
    fun createVoteControllerTest() {
        val voteRequest = CreateVoteRequest(
            nickname = "testUser",
            title = "Test Vote",
            type = "MAJORITY",
            timeLimit = 10,
            userVoteLimit = 1,
            link = "http://example.com/vote/12345"
        )

        val voteUuid = "12345"
        val websocketBaseUrl = "http://localhost:8080"
        val websocketPath = "/ws/votes"
        val websocketUrl = "$websocketBaseUrl$websocketPath"

        val creator = Users(nickname = "testUser")

        doNothing().`when`(sessionService).saveCreatorNicknameToSession(any(), eq("testUser"), eq(10))
        doNothing().`when`(webSocketManager).startWebSocketForVote(any(), eq(10))

        `when`(userService.createUserForVote(eq("testUser"))).thenReturn(creator)
        `when`(voteService.createVote(any(), eq(voteUuid), eq(creator))).thenReturn(1L)

        val result = mockMvc.perform(
            post("/api/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voteRequest))
        )
            .andExpect(status().isOk())  // 200 응답을 기대
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.result.websocketUrl").value(websocketUrl))
            .andReturn()

        val cookieHeader = result.response.getHeader("Set-Cookie")
        assertThat(cookieHeader).isNotNull()
        assertThat(cookieHeader).contains("sessionId=")
        assertThat(cookieHeader).contains("Path=/")
        assertThat(cookieHeader).contains("HttpOnly")
        assertThat(cookieHeader).contains("Max-Age=${voteRequest.timeLimit * 60}")

        val response = objectMapper.readValue(result.response.contentAsString, BaseResponse::class.java)
        val createVoteResponse = objectMapper.convertValue(response.result, CreateVoteResponse::class.java)

        assertThat(createVoteResponse.websocketUrl).isEqualTo(websocketUrl)

        verify(sessionService).saveCreatorNicknameToSession(any(), eq("testUser"), eq(10))
        verify(webSocketManager).startWebSocketForVote(eq(voteUuid), eq(10))
        verify(userService).createUserForVote(eq("testUser"))
        verify(voteService).createVote(eq(voteRequest), eq(voteUuid), eq(creator))
    }
}
