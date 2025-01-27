package com.goosesdream.golaping.vote.repository

import com.goosesdream.golaping.user.entity.Users
import com.goosesdream.golaping.vote.entity.UserVotes
import com.goosesdream.golaping.vote.entity.VoteOptions
import com.goosesdream.golaping.vote.entity.Votes
import org.springframework.data.jpa.repository.JpaRepository

interface UserVoteRepository : JpaRepository<UserVotes, Long> {
    fun countByVoteOption(voteOptions: VoteOptions): Int
    fun findByVoteAndUser(vote: Votes, user: Users): List<UserVotes>
}