package com.goosesdream.golaping.vote.repository

import com.goosesdream.golaping.vote.entity.VoteOptions
import com.goosesdream.golaping.vote.entity.Votes
import org.springframework.data.jpa.repository.JpaRepository

interface VoteOptionRepository : JpaRepository<VoteOptions, Long> {
    fun findByVote(vote: Votes): List<VoteOptions>
}