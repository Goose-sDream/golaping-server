package com.goosesdream.golaping.vote.entity

import com.goosesdream.golaping.common.base.BaseEntity
import com.goosesdream.golaping.user.entity.Users
import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert

@Entity
@DynamicInsert
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["vote_idx", "user_idx"])]
)
class Participants(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val participantIdx: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_idx", nullable = false)
    val vote: Votes,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_idx", nullable = false)
    val user: Users
) : BaseEntity()
