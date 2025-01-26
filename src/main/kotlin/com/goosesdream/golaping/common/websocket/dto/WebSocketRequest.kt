package com.goosesdream.golaping.common.websocket.dto

data class WebSocketRequest(
    val action: String,
    val voteUuid: String? = null,
    val optionId: Long? = null,
    val optionText: String? = null,
    val optionColor: String? = null
)
