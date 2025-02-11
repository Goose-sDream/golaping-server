package com.goosesdream.golaping.vote.dto

import java.time.LocalDateTime

data class CreateVoteResponse(
    val websocketUrl: String,
    val voteIdx: Long?,
    val voteUuid: String,
    val voteEndTime: LocalDateTime?
)
