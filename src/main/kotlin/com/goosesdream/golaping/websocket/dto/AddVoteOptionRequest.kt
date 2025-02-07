package com.goosesdream.golaping.websocket.dto

data class AddVoteOptionRequest(
    val optionId: Long? = null,
    val optionText: String? = null,
    val optionColor: String? = null
)
