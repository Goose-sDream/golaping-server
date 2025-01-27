package com.goosesdream.golaping.common.websocket.dto

data class VoteOptionsData(
    val optionId: Long,
    val optionName: String,
    val voteCount: Int
)