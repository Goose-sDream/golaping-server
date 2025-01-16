package com.goosesdream.golaping.vote.entity

import com.goosesdream.golaping.common.base.BaseEntity
import com.goosesdream.golaping.common.enums.VoteType
import com.goosesdream.golaping.user.entity.Users
import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert
import java.time.LocalDateTime
import java.util.*

@Entity
@DynamicInsert
class Votes(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val voteIdx: Long? = null,

    @Column(nullable = false)
    var title: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_idx", nullable = false)
    var creator: Users,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: VoteType,

    @Column(nullable = false)
    var endTime: LocalDateTime,

    @Column(nullable = true)
    var userVoteLimit: Int? = null,

    @Column(nullable = false)
    var link: String = "",

    @Column(nullable = false, unique = true)
    var uuid: String = UUID.randomUUID().toString()
) : BaseEntity()