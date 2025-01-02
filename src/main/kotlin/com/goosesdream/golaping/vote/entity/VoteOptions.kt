package com.goosesdream.golaping.vote.entity

import com.goosesdream.golaping.user.entity.Users
import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert

@Entity
@DynamicInsert
class VoteOptions(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val voteOptionIdx: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_idx", nullable = false)
    val vote: Votes,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_idx", nullable = false)
    val creator: Users,

    @Column(nullable = false)
    var optionName: String,

    @Column(nullable = false)
    var color: String
)
