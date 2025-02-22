package com.goosesdream.golaping.websocket.dto

data class VoteOptionDataBroadcast(
    val optionId: Long,
    val optionName: String,
    val voteCount: Int,
    val voteColor: String
)