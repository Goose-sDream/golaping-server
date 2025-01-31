package com.goosesdream.golaping.vote.dto

data class VoteResultData(
    var ranking: Int,
    val optionId: Long,
    val optionName: String,
    val voteCount: Int,
    val voteColor: String
)
