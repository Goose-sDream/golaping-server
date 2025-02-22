package com.goosesdream.golaping.websocket.dto.addOption

data class AddVoteOptionBroadcastResponse(
    val optionId: Long,
    val optionName: String,
    val voteColor: String
)
