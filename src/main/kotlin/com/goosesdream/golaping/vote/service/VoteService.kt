package com.goosesdream.golaping.vote.service

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.base.BaseResponse
import com.goosesdream.golaping.common.base.BaseResponseStatus.*
import com.goosesdream.golaping.common.enums.VoteType
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
    // 투표 샹성
    @Transactional(rollbackFor = [Exception::class])
    fun createVote(request: CreateVoteRequest): BaseResponse<String> {
        val creator = userRepository.findByNickname(request.nickname) ?: throw BaseException(INVALID_USER)

        val voteType = try {
            VoteType.valueOf(request.type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BaseException(INVALID_VOTE_TYPE)
        }

        if (request.timeLimit <= 0) {
            throw BaseException(INVALID_TIME_LIMIT)
        }
        val endTime = LocalDateTime.now().plusMinutes(request.timeLimit.toLong())

        val vote = Votes(
            title = request.title,
            creator = creator,
            type = voteType,
            endTime = endTime,
            userVoteLimit = request.userVoteLimit,
            link = request.link
        )
        voteRepository.save(vote)

        return BaseResponse(SUCCESS)
    }
}
