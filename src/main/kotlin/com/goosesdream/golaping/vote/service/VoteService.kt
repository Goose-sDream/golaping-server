package com.goosesdream.golaping.vote.service

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.enums.BaseResponseStatus.*
import com.goosesdream.golaping.common.enums.VoteType
import com.goosesdream.golaping.common.websocket.dto.VoteOptionsData
import com.goosesdream.golaping.redis.service.RedisService
import com.goosesdream.golaping.user.entity.Users
import com.goosesdream.golaping.vote.dto.CreateVoteRequest
import com.goosesdream.golaping.vote.entity.VoteOptions
import com.goosesdream.golaping.vote.entity.Votes
import com.goosesdream.golaping.vote.repository.ParticipantRepository
import com.goosesdream.golaping.vote.repository.UserVoteRepository
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
    private val voteOptionRepository: VoteOptionRepository,
    private val userVotesRepository: UserVoteRepository
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

    // 투표 옵션 추가
    @Transactional(rollbackFor = [Exception::class])
    fun addOption(voteUuid: String, nickname: String, optionText: String?, optionColor: String?): VoteOptions {
        if (optionText.isNullOrBlank()) throw BaseException(INVALID_OPTION_TEXT)
        if (optionColor.isNullOrBlank()) throw BaseException(INVALID_OPTION_COLOR)

        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
        val creator = participantRepository.findByVoteAndUserNickname(vote, nickname)?.user ?: throw BaseException(PARTICIPANT_NOT_FOUND)
        val newOption = VoteOptions(
            vote = vote,
            creator = creator,
            optionName = optionText,
            color = optionColor
        )
        return voteOptionRepository.save(newOption)
    }

    // 특정 투표의 이전 투표 데이터 조회
    fun getPreviousVotes(voteUuid: String): List<VoteOptionsData> {
        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
        val voteOptions = voteOptionRepository.findByVote(vote)

        if (voteOptions.isEmpty()) {
            return emptyList()
        }

        return voteOptions.map { voteOption ->
            val voteCount = userVotesRepository.countByVoteOption(voteOption)
            voteOption.voteOptionIdx?.let {
                VoteOptionsData(
                    optionId = it,
                    optionName = voteOption.optionName,
                    voteCount = voteCount
                )
            } ?: throw BaseException(VOTE_OPTION_NOT_FOUND)
        }
    }

    // 특정 유저가 선택한 투표 옵션 목록 조회
    fun getUserVoteOptionIds(voteUuid: String, nickname: String): List<Long> {
        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
        val participant = participantRepository.findByVoteAndUserNickname(vote, nickname) ?: return emptyList()

        val userVotes = userVotesRepository.findByVoteAndUser(vote, participant.user)

        return userVotes.map { it.voteOption.voteOptionIdx!! }
    }
}
