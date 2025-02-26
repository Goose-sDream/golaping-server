package com.goosesdream.golaping.websocket.dto.addOption

data class AddVoteOptionResponse(
    val optionId: Long,
    val optionName: String,
    val voteColor: String
)
