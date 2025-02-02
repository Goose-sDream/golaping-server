package com.goosesdream.golaping.vote.dto

data class CreateVoteResponse(
    val websocketUrl: String,
    val voteIdx: Long?,
    val voteUuid: String
)
