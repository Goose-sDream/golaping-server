package com.goosesdream.golaping.user.service

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.user.entity.Users
import com.goosesdream.golaping.user.repository.UserRepository
import com.goosesdream.golaping.vote.repository.VoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.goosesdream.golaping.common.base.BaseResponseStatus.*
import com.goosesdream.golaping.vote.entity.Participants
import com.goosesdream.golaping.vote.entity.Votes
import com.goosesdream.golaping.vote.repository.ParticipantRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val voteRepository: VoteRepository,
    private val participantRepository: ParticipantRepository
) {
    @Transactional(rollbackFor = [Exception::class])
    fun createUser(nickname: String, voteUuid: String) : Users {
        validateNickname(nickname)

        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(INVALID_VOTE_UUID)

        val isNicknameExistsInVote = participantRepository.existsByVoteAndUserNickname(vote, nickname)
        if (isNicknameExistsInVote) throw BaseException(NICKNAME_ALREADY_EXISTS_IN_VOTE)

        val newUser = buildUser(nickname)
        saveUser(newUser)
        return newUser
    }

    fun findOrCreateUser(nickname : String): Users {
        return userRepository.findByNickname(nickname) ?: buildAndSaveUser(nickname)
    }

    private fun buildAndSaveUser(nickname: String): Users {
        validateNickname(nickname)
        val newUser = buildUser(nickname)
        saveUser(newUser)
        return newUser
    }

    private fun validateNickname(nickname: String) {
        if (nickname.isBlank()) {
            throw BaseException(INVALID_NICKNAME)
        }
    }

    private fun buildUser(nickname: String): Users {
        return Users(
            nickname = nickname
        )
    }

    private fun saveUser(user: Users) {
        userRepository.save(user)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun addParticipant(user : Users, voteUuid: String) {
        val vote = voteRepository.findByUuid(voteUuid) ?: throw BaseException(INVALID_VOTE_UUID)

        val isAlreadyParticipant = participantRepository.existsByVoteAndUserNickname(vote, user.nickname)
        if (isAlreadyParticipant) throw BaseException(USER_ALREADY_PARTICIPANT)

        val participant = buildParticipant(user, vote)
        saveParticipant(participant)
    }

    private fun buildParticipant(user: Users, vote: Votes) : Participants {
        return Participants(
            user = user,
            vote = vote
        )
    }

    private fun saveParticipant(participant: Participants) {
        participantRepository.save(participant)
    }
}