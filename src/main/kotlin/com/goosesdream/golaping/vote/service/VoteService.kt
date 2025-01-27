package com.goosesdream.golaping.vote.service

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.enums.BaseResponseStatus.*
import com.goosesdream.golaping.common.enums.VoteType
import com.goosesdream.golaping.redis.service.RedisService
import com.goosesdream.golaping.user.entity.Users
import com.goosesdream.golaping.vote.dto.CreateVoteRequest
import com.goosesdream.golaping.vote.entity.VoteOptions
import com.goosesdream.golaping.vote.entity.Votes
import com.goosesdream.golaping.vote.repository.ParticipantRepository
import com.goosesdream.golaping.vote.repository.VoteOptionRepository
import com.goosesdream.golaping.vote.repository.VoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class VoteService(
    private val voteRepository: VoteRepository,
    private val redisService: RedisService,
    private val participantRepository: ParticipantRepository,
    private val voteOptionRepository: VoteOptionRepository
) {
    // 투표 생성
    @Transactional(rollbackFor = [Exception::class])
    fun createVote(request: CreateVoteRequest, voteUuid: String, creator: Users) { //TODO: voteType에 따라 다른 로직 구현 필요
        val voteType = parseVoteType(request.type)
        validateTimeLimit(request.timeLimit)

        val vote = buildVote(request, creator, voteType, voteUuid)
        saveVote(vote)
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
            userVoteLimit = request.userVoteLimit.takeIf { it != 0 },
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

    // 투표 종료 시간 조회
    fun getVoteEndTime(voteUuid: String): LocalDateTime? {
        return voteRepository.findByUuid(voteUuid)?.endTime ?: throw BaseException(VOTE_NOT_FOUND)
    }

    private val voteExpirationPrefix = "vote:expiration:"

    fun saveVoteExpirationToRedis(voteUuid: String, timeLimit: Int) {
        val redisKey = voteExpirationPrefix + voteUuid
        val ttlInSeconds = timeLimit * 60L
        redisService.save(redisKey, "active", ttlInSeconds)
    }

    fun getVoteLimit(voteUuid: String): Int {
        return voteRepository.findByUuid(voteUuid)?.userVoteLimit ?: throw BaseException(VOTE_NOT_FOUND)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun addOption(voteUuid: String, nickname: String, optionText: String?, optionColor: String?): VoteOptions {
        if (optionText.isNullOrBlank()) throw BaseException(INVALID_OPTION_TEXT)
        if (optionColor.isNullOrBlank()) throw BaseException(INVALID_OPTION_COLOR)

        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
        val creator = participantRepository.findByVoteAndUserNickname(vote, nickname)?.user ?: throw BaseException(NOT_FOUND_PARTICIPANT)
        val newOption = VoteOptions(
            vote = vote,
            creator = creator,
            optionName = optionText,
            color = optionColor
        )
        return voteOptionRepository.save(newOption)
    }
}
