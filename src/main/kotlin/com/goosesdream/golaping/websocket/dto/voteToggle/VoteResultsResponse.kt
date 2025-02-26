package com.goosesdream.golaping.websocket.dto.voteToggle

import com.goosesdream.golaping.websocket.dto.VoteOptionsData

data class VoteResultsResponse(
    val isCreator: Boolean,
    val totalVoteCount: Int, // 해당 user의 총 투표 수
    val changedOption: VoteOptionsData
)
