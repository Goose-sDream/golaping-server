package com.goosesdream.golaping.common.base

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity {

    @Column(columnDefinition = "varchar(10) default 'active'")
    var status: String = "active"

    @CreatedDate
    @Column(nullable = false, updatable = false)
    val regTs: LocalDateTime? = null

    @LastModifiedDate
    var updTs: LocalDateTime? = null
}