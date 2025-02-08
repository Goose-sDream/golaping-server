package com.goosesdream.golaping.websocket.dto

data class VoteResponse(
    val isCreator: Boolean,
    val totalVoteCount: Int, // 해당 user의 총 투표 수
    val voteOptions: List<VoteOptionsData>
)
