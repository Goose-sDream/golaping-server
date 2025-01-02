package com.goosesdream.golaping.user.entity

import com.goosesdream.golaping.common.base.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert

@Entity
@DynamicInsert
class Users protected constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String
) : BaseEntity() {
    constructor(name: String) : this(null, name)
}
