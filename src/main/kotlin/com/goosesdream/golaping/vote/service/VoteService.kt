package com.goosesdream.golaping.vote.service

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.constants.Status.Companion.ACTIVE
import com.goosesdream.golaping.common.constants.Status.Companion.INACTIVE
import com.goosesdream.golaping.common.enums.BaseResponseStatus.*
import com.goosesdream.golaping.common.enums.VoteType
import com.goosesdream.golaping.redis.service.RedisService
import com.goosesdream.golaping.user.entity.Users
import com.goosesdream.golaping.vote.dto.CreateVoteRequest
import com.goosesdream.golaping.vote.dto.VoteResultData
import com.goosesdream.golaping.vote.entity.Participants
import com.goosesdream.golaping.vote.entity.UserVotes
import com.goosesdream.golaping.vote.entity.VoteOptions
import com.goosesdream.golaping.vote.entity.Votes
import com.goosesdream.golaping.vote.repository.ParticipantRepository
import com.goosesdream.golaping.vote.repository.UserVoteRepository
import com.goosesdream.golaping.vote.repository.VoteOptionRepository
import com.goosesdream.golaping.vote.repository.VoteRepository
import com.goosesdream.golaping.websocket.dto.*
import com.goosesdream.golaping.websocket.dto.addOption.AddVoteOptionResponse
import com.goosesdream.golaping.websocket.dto.voteToggle.VoteResultsBroadcastOptionData
import com.goosesdream.golaping.websocket.dto.voteToggle.VoteResultsBroadcastResponse
import com.goosesdream.golaping.websocket.dto.voteToggle.VoteResultsResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

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
    fun createVote(request: CreateVoteRequest, voteUuid: String, creator: Users) : Long? { //TODO: voteType에 따라 다른 로직 구현 필요
        val voteType = parseVoteType(request.type)
        validateTimeLimit(request.timeLimit)

        val vote = buildVote(request, creator, voteType, voteUuid)
        saveVote(vote)
        return vote.voteIdx
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
            userVoteLimit = if (request.userVoteLimit == 0) null else request.userVoteLimit,
            link = request.link,
            uuid = voteUuid
        )
    }

    private fun saveVote(vote: Votes) {
        voteRepository.save(vote)
    }

    // 투표 종료 여부 확인
    fun checkVoteEnded(voteUuid: String?): Boolean {
        if (voteUuid.isNullOrBlank()) return true

        val vote = voteRepository.findByUuid(voteUuid) ?: return true
        return vote.status == INACTIVE || vote.endTime.isBefore(LocalDateTime.now())
    }

    // 투표 종료 시간 조회
    fun getVoteEndTime(voteUuid: String): LocalDateTime? {
        return voteRepository.findByUuid(voteUuid)?.endTime ?: throw BaseException(VOTE_NOT_FOUND)
    }

    private val voteExpirationPrefix = "vote:expiration:"

    fun saveVoteExpirationToRedis(voteUuid: String, timeLimit: Int) {
        val redisKey = voteExpirationPrefix + voteUuid
        val ttlInSeconds = timeLimit * 60L
        redisService.save(redisKey, ACTIVE, ttlInSeconds)
    }

    fun getVoteLimit(voteUuid: String): Int? {
        return voteRepository.findByUuid(voteUuid)?.userVoteLimit
    }

    // 투표 옵션 추가
    @Transactional(rollbackFor = [Exception::class])
    fun addOption(voteUuid: String, nickname: String, optionText: String?, optionColor: String?): AddVoteOptionResponse {
        if (optionText.isNullOrBlank()) throw BaseException(INVALID_OPTION_TEXT)
        if (optionColor.isNullOrBlank()) throw BaseException(INVALID_OPTION_COLOR)

        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
        val creator = participantRepository.findByVoteAndUserNickname(vote, nickname)?.user ?: throw BaseException(PARTICIPANT_NOT_FOUND)
        val newOption = voteOptionRepository.save(
            VoteOptions(
                vote = vote,
                creator = creator,
                optionName = optionText,
                color = optionColor
            )
        )

        return AddVoteOptionResponse(
            optionId = newOption.voteOptionIdx!!,
            optionName = newOption.optionName,
            voteColor = newOption.color
        )
    }

    // 특정 투표의 투표 데이터 조회
    fun getPreviousVoteData(voteUuid: String, nickname: String): List<VoteOptionsData> {
        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
        val voteOptions = voteOptionRepository.findByVote(vote)
        val participant = participantRepository.findByVoteAndUserNickname(vote, nickname) ?: throw BaseException(PARTICIPANT_NOT_FOUND)

        if (voteOptions.isEmpty()) return emptyList()

        return getVoteOptionsData(voteOptions, participant)
    }

    private fun getVoteOptionsData(
        voteOptions: List<VoteOptions>,
        participant: Participants
    ): List<VoteOptionsData> {
        return voteOptions.map { voteOption ->
            val voteCount = userVotesRepository.countByVoteOptionAndStatus(voteOption, ACTIVE)
            val isVotedByUser =
                userVotesRepository.existsByVoteOptionAndUserAndStatus(voteOption, participant.user, ACTIVE)
            voteOption.voteOptionIdx?.let {
                VoteOptionsData(
                    optionId = it,
                    optionName = voteOption.optionName,
                    voteCount = voteCount,
                    voteColor = voteOption.color,
                    isVotedByUser
                )
            } ?: throw BaseException(VOTE_OPTION_NOT_FOUND)
        }
    }

    private fun getVoteOptionsDataForBroadcast(
        voteOptions: List<VoteOptions>
    ): List<VoteResultsBroadcastOptionData> {
        return voteOptions.map { voteOption ->
            voteOption.voteOptionIdx?.let {
                VoteResultsBroadcastOptionData(
                    optionId = it,
                    optionName = voteOption.optionName,
                    voteCount = userVotesRepository.countByVoteOptionAndStatus(voteOption, ACTIVE),
                    voteColor = voteOption.color
                )
            } ?: throw BaseException(VOTE_OPTION_NOT_FOUND)
        }
    }

    // 개인별 투표 데이터 조회
    fun getCurrentVoteCounts(voteUuid: String, nickname: String): VoteResultsResponse {
        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
        val voteOptions = voteOptionRepository.findByVote(vote)
        val participant = participantRepository.findByVoteAndUserNickname(vote, nickname) ?: throw BaseException(PARTICIPANT_NOT_FOUND)

        if (voteOptions.isEmpty()) {// 투표 옵션이 없는 경우
            return VoteResultsResponse(
                isCreator = vote.creator.nickname == nickname,
                totalVoteCount = 0,
                voteOptions = emptyList()
            )
        }
        return VoteResultsResponse(
            isCreator = vote.creator.nickname == nickname,
            totalVoteCount = userVotesRepository.countByVoteAndUserAndStatus(vote, participant.user, ACTIVE),
            voteOptions = getVoteOptionsData(voteOptions, participant)
        )
    }

    fun getVoteOptionCount(optionId: Long): Int {
        return userVotesRepository.countByVoteOptionAndStatus(voteOptionRepository.findById(optionId).orElseThrow(), ACTIVE)
    }

    fun isUserVotedForOption(voteUuid: String, nickname: String, optionId: Long): Boolean {
        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
        val participant = participantRepository.findByVoteAndUserNickname(vote, nickname) ?: throw BaseException(PARTICIPANT_NOT_FOUND)

        return userVotesRepository.existsByVoteOptionAndUserAndStatus(
            voteOptionRepository.findById(optionId).orElseThrow(), participant.user, ACTIVE
        )
    }

    // 브로드캐스트용 투표 데이터 조회
    fun getVoteResultsForBroadcast(voteUuid: String): VoteResultsBroadcastResponse {
        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
        val voteOptions = voteOptionRepository.findByVote(vote)

        return VoteResultsBroadcastResponse(getVoteOptionsDataForBroadcast(voteOptions))
    }

    // 특정 유저가 선택한 투표 옵션 목록 조회
    fun getUserVoteOptionIds(voteUuid: String, nickname: String): List<Long> {
        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
        val participant = participantRepository.findByVoteAndUserNickname(vote, nickname) ?: return emptyList()

        val userVotes = userVotesRepository.findByVoteAndUserAndStatus(vote, participant.user, ACTIVE)

        return userVotes.map { it.voteOption.voteOptionIdx!! }
    }

    fun getVote(voteUuid: String): Votes? {
        return voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
    }

    fun getVoteByVoteIdx(voteIdx: Long): Votes? {
        return voteRepository.findById(voteIdx).orElse(null)
    }

    fun getVoteOption(selectedOptionId: Long): Optional<VoteOptions> {
        return voteOptionRepository.findById(selectedOptionId)
    }

    fun getUserVote(voteUuid: String, nickname: String, selectedOptionId: Long): UserVotes? {
        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)
        val participant = participantRepository.findByVoteAndUserNickname(vote, nickname) ?: throw BaseException(PARTICIPANT_NOT_FOUND)
        val selectedOption = voteOptionRepository.findById(selectedOptionId).orElse(null) ?: throw BaseException(VOTE_OPTION_NOT_FOUND)

        return userVotesRepository.findByVoteAndUserAndVoteOption(vote, participant.user, selectedOption)
    }

    fun deactivateVote(userVote: UserVotes) {
        userVote.status = INACTIVE
        userVotesRepository.save(userVote)
    }

    fun activateVote(userVote: UserVotes) {
        userVote.status = ACTIVE
        userVotesRepository.save(userVote)
    }

    // 투표하기
    fun vote(vote: Votes, nickname: String, voteOption: VoteOptions) {
        val participant = participantRepository.findByVoteAndUserNickname(vote, nickname) ?: throw BaseException(PARTICIPANT_NOT_FOUND)
        val userVote = buildUserVote(vote, participant.user, voteOption)
        userVotesRepository.save(userVote)
    }

    private fun buildUserVote(vote: Votes, user: Users, voteOption: VoteOptions): UserVotes {
        return UserVotes(
            vote = vote,
            user = user,
            voteOption = voteOption
        )
    }

    // 투표 결과 조회
    fun getVoteResults(voteIdx: Long): List<VoteResultData> {
        val vote = voteRepository.findById(voteIdx).orElseThrow { BaseException(VOTE_NOT_FOUND) }
        val voteOptions = getVoteOptions(vote)

        if (voteOptions.isEmpty()) return emptyList()

        val voteCounts = getVoteCounts(voteOptions)

        var currentRank = 1
        var previousCount: Int? = null

        val sortedResults = voteOptions.map { option ->
            VoteResultData(
                ranking = 0,
                optionId = option.voteOptionIdx!!,
                optionName = option.optionName,
                voteCount = voteCounts[option.voteOptionIdx] ?: 0,
                voteColor = option.color
            )
        }.sortedByDescending { it.voteCount }
            .onEachIndexed { index, result ->
                if (previousCount == null || result.voteCount != previousCount) {
                    currentRank = index + 1
                }
                previousCount = result.voteCount
                result.ranking = currentRank
            }
        return sortedResults
    }

    // 투표 종료(투표 제한 시간 도달 전)
    fun closeVote(vote: Votes, nickname: String): List<VoteResultData> {
        val user = participantRepository.findByVoteAndUserNickname(vote, nickname)?.user ?: throw BaseException(PARTICIPANT_NOT_FOUND)
        if (user != vote.creator) throw BaseException(NOT_CREATOR)

        if (vote.status == INACTIVE) throw BaseException(EXPIRED_VOTE)
        vote.status = INACTIVE
        voteRepository.save(vote)

        return getVoteResults(vote.voteIdx!!)
    }

    private fun getVoteOptions(vote: Votes): List<VoteOptions> {
        return voteOptionRepository.findByVote(vote)
    }

    private fun getVoteCounts(voteOptions: List<VoteOptions>): Map<Long, Int> {
        return userVotesRepository.countVotesByVoteOptionsAndStatus(voteOptions, ACTIVE)
            .associate { (optionId, count) -> (optionId as Long) to (count as Int) }
    }

    // 타이머 만료로 인한 투표 종료 처리
    fun expireVote(voteUuid: String) {
        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(VOTE_NOT_FOUND)

        if (vote.status != INACTIVE) {
            vote.status = INACTIVE
            voteRepository.save(vote)
        }
    }

    fun getVoteIdxByVoteUuid(voteUuid: String): Long {
        return voteRepository.findByUuid(voteUuid)?.voteIdx ?: throw BaseException(VOTE_NOT_FOUND)
    }
}
