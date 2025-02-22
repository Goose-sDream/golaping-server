package com.goosesdream.golaping.websocket.dto.voteToggle

data class VoteResultsBroadcastOptionData(
    val optionId: Long,
    val optionName: String,
    val voteCount: Int,
    val voteColor: String
)