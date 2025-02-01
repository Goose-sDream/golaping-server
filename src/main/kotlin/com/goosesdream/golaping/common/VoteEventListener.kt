package com.goosesdream.golaping.common

import com.goosesdream.golaping.vote.dto.VoteExpiredEvent
import com.goosesdream.golaping.vote.service.VoteService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class VoteEventListener(private val voteService: VoteService) {

    @EventListener
    fun onVoteExpired(event: VoteExpiredEvent) {
        voteService.expireVote(event.voteUuid)
    }
}