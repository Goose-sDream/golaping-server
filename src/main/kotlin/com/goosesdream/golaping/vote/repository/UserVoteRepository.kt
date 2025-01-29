package com.goosesdream.golaping.vote.repository

import com.goosesdream.golaping.user.entity.Users
import com.goosesdream.golaping.vote.entity.UserVotes
import com.goosesdream.golaping.vote.entity.VoteOptions
import com.goosesdream.golaping.vote.entity.Votes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserVoteRepository : JpaRepository<UserVotes, Long> {
    fun countByVoteOptionAndStatus(voteOptions: VoteOptions, status: String): Int
    fun findByVoteAndUserAndStatus(vote: Votes, user: Users, status: String): List<UserVotes>
    fun countByVoteAndUserAndStatus(vote: Votes, user: Users, status: String): Int
    fun findByVoteAndUserAndVoteOption(vote: Votes, user: Users, voteOption: VoteOptions): UserVotes?
    fun existsByVoteOptionAndUserAndStatus(voteOptions: VoteOptions, user: Users, status: String): Boolean

    @Query("SELECT uv.voteOption.voteOptionIdx, COUNT(uv) FROM UserVotes uv WHERE uv.voteOption IN :voteOptions AND uv.status = :status GROUP BY uv.voteOption.voteOptionIdx")
    fun countVotesByVoteOptionsAndStatus(@Param("voteOptions") voteOptions: List<VoteOptions>, @Param("status") status: String): List<Array<Any>>
}