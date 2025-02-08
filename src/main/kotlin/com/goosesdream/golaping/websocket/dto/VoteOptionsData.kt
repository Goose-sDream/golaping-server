package com.goosesdream.golaping.websocket.dto

data class VoteOptionsData(
    val optionId: Long,
    val optionName: String,
    val voteCount: Int,
    val voteColor: String,
    val isVotedByUser: Boolean
)