package com.goosesdream.golaping.user.entity

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
) {
    constructor(name: String) : this(null, name)
}
