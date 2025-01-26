package com.goosesdream.golaping.vote.repository

import com.goosesdream.golaping.vote.entity.VoteOptions
import org.springframework.data.jpa.repository.JpaRepository

interface VoteOptionRepository : JpaRepository<VoteOptions, Long> {
}