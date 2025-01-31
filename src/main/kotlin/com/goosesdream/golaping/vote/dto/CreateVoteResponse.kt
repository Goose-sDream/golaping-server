package com.goosesdream.golaping.vote.dto

data class CreateVoteResponse(
    val websocketUrl: String,
    val sessionId: String,
    val voteIdx: Long?,
    val voteUuid: String
)
