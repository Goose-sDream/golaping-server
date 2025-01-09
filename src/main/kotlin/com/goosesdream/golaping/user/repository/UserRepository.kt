package com.goosesdream.golaping.user.repository

import com.goosesdream.golaping.user.entity.Users
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<Users, Long> {
    fun findByNickname(nickname: String): Users?
}