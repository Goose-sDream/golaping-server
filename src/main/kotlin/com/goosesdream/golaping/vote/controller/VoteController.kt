package com.goosesdream.golaping.vote.controller

import com.goosesdream.golaping.common.base.BaseResponse
import com.goosesdream.golaping.common.constants.RequestURI.Companion.VOTES
import com.goosesdream.golaping.vote.dto.CreateVoteRequest
import com.goosesdream.golaping.vote.service.VoteService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(VOTES)
class VoteController(
    private val voteService: VoteService
) {
    @PostMapping
    fun createVote(@RequestBody request: CreateVoteRequest): BaseResponse<String> {
        return voteService.createVote(request)
    }
}