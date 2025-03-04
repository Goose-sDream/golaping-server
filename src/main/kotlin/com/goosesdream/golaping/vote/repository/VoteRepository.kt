package com.goosesdream.golaping.vote.repository

import com.goosesdream.golaping.vote.entity.Votes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VoteRepository : JpaRepository<Votes, Long> {
    fun findByUuid(uuid: String): Votes?

    @Query("SELECT v FROM Votes v JOIN FETCH v.creator WHERE v.uuid = :uuid")
    fun findWithCreatorByUuid(@Param("uuid") uuid: String): Votes?
}