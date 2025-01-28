package com.goosesdream.golaping.common.websocket.dto

import java.time.LocalDateTime

data class WebSocketInitialResponse(
    val voteLimit: Int,
    val voteEndTime: LocalDateTime,
    val webSocketSessionId: String? = null,
    val previousVotes: List<VoteOptionsData>
)