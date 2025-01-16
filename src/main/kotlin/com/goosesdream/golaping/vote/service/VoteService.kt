package com.goosesdream.golaping.vote.service

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.base.BaseResponseStatus.*
import com.goosesdream.golaping.common.enums.VoteType
import com.goosesdream.golaping.user.entity.Users
import com.goosesdream.golaping.user.repository.UserRepository
import com.goosesdream.golaping.vote.dto.CreateVoteRequest
import com.goosesdream.golaping.vote.entity.Votes
import com.goosesdream.golaping.vote.repository.VoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class VoteService(
    private val voteRepository: VoteRepository,
    private val userRepository: UserRepository
) {
    // 투표 생성
    @Transactional(rollbackFor = [Exception::class])
    fun createVote(request: CreateVoteRequest, voteUuid: String) {
        val creator = findUserByNickname(request.nickname)
        val voteType = parseVoteType(request.type)
        validateTimeLimit(request.timeLimit)

        val vote = buildVote(request, creator, voteType, voteUuid)
        saveVote(vote)
    }

    private fun findUserByNickname(nickname: String): Users {
        return userRepository.findByNickname(nickname) ?: throw BaseException(INVALID_USER)
    }

    private fun parseVoteType(type: String): VoteType {
        return try {
            VoteType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BaseException(INVALID_VOTE_TYPE)
        }
    }

    private fun validateTimeLimit(timeLimit: Int) {
        if (timeLimit <= 0) {
            throw BaseException(INVALID_TIME_LIMIT)
        }
    }

    private fun buildVote(request: CreateVoteRequest, creator: Users, voteType: VoteType, voteUuid: String): Votes {
        val endTime = LocalDateTime.now().plusMinutes(request.timeLimit.toLong())
        return Votes(
            title = request.title,
            creator = creator,
            type = voteType,
            endTime = endTime,
            userVoteLimit = request.userVoteLimit,
            link = request.link,
            uuid = voteUuid
        )
    }

    private fun saveVote(vote: Votes) {
        voteRepository.save(vote)
    }

    // 투표 종료 여부 확인
    fun checkVoteEnded(voteUuid: String): Boolean {
        val vote = voteRepository.findByUuid(voteUuid)
        return vote?.endTime?.isBefore(LocalDateTime.now()) ?: true
    }
}
