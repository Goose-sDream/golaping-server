package com.goosesdream.golaping.vote.dto

import java.time.LocalDateTime

data class EnterVoteResponse(
    val websocketUrl: String,
    val voteEndTime: LocalDateTime?,
    val voteIdx: Long
)
