package com.goosesdream.golaping.vote.repository

import com.goosesdream.golaping.vote.entity.Participants
import com.goosesdream.golaping.vote.entity.Votes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ParticipantRepository : JpaRepository<Participants, Long> {
    fun existsByVoteAndUserNickname(vote: Votes, nickname: String): Boolean
    fun findByVoteAndUserNickname(vote: Votes, nickname: String): Participants?
}