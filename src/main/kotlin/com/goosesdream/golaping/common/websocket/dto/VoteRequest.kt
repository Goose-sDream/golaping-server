package com.goosesdream.golaping.common.websocket.dto

data class VoteRequest(
    val voteUuid: String? = null,
    val optionId: Long? = null
)
