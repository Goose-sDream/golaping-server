package com.goosesdream.golaping.common.websocket.dto

data class WebSocketInitialResponse(
    val voteLimit: Int,
    val voteEndTime: Long,
    val webSocketSessionId: String? = null,
    val previousVotes: List<VoteOptionsData>,
    val userVoteOptionIds: List<Long>
)