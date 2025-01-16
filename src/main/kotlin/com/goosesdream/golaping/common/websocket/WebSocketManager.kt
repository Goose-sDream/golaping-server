package com.goosesdream.golaping.common.websocket

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.base.BaseResponseStatus.ALREADY_EXIST_CHANNEL
import org.springframework.stereotype.Service
import java.util.*

@Service
class WebSocketManager { // 웹소켓 채널 관리

    private val activeChannels = mutableMapOf<String, Timer>()

    // 채널 생성
    fun startWebSocketForVote(voteUuid: String, timeLimit: Int) {
        if (activeChannels.containsKey(voteUuid)) {
            throw BaseException(ALREADY_EXIST_CHANNEL)
        }

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                stopWebSocketForVote(voteUuid)
            }
        }, timeLimit * 60 * 1000L)

        activeChannels[voteUuid] = timer // uuid 형식
    }

    // 채널 종료
    fun stopWebSocketForVote(voteUuid: String) {
        activeChannels[voteUuid]?.cancel()
        activeChannels.remove(voteUuid)
    }
}
