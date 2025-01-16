package com.goosesdream.golaping.vote.dto

data class CreateVoteRequest(
    val title: String,
    val nickname: String,
    val type: String,
    val timeLimit: Int,
    val userVoteLimit: Int?,
    val link: String
)
