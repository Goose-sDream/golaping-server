package com.goosesdream.golaping.vote.repository

import com.goosesdream.golaping.vote.entity.Votes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VoteRepository : JpaRepository<Votes, Long> {
}