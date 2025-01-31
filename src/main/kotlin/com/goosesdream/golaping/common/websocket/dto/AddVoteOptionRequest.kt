package com.goosesdream.golaping.common.websocket.dto

data class AddVoteOptionRequest(
    val optionId: Long? = null,
    val optionText: String? = null,
    val optionColor: String? = null
)
