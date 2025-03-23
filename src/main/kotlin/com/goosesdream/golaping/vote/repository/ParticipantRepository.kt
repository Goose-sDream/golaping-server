package com.goosesdream.golaping.vote.repository

import com.goosesdream.golaping.vote.entity.Participants
import com.goosesdream.golaping.vote.entity.Votes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ParticipantRepository : JpaRepository<Participants, Long> {
    fun existsByVoteAndUserNickname(vote: Votes, nickname: String): Boolean

    @Query("SELECT p FROM Participants p JOIN FETCH p.user WHERE p.vote = :vote AND p.user.nickname = :nickname")
    fun findByVoteAndUserNickname(@Param("vote") vote: Votes, @Param("nickname") nickname: String): Participants?
}