package com.goosesdream.golaping.websocket

//@Component
//class GlobalWebSocketHandler(
//    private val webSocketManager: WebSocketManager,
//    private val objectMapper: ObjectMapper,
//    private val voteService: VoteService
//) : WebSocketHandler{
//
//    override fun afterConnectionEstablished(session: WebSocketSession) { // WebSocket 연결 성립된 후 실행
//        val voteUuid = session.attributes["voteUuid"] as? String
//            ?: return handleErrorResponse(session, INVALID_VOTE_UUID)
//
//        val expirationTime = webSocketManager.getChannelExpirationTime(voteUuid)
//            ?: return handleErrorResponse(session, EXPIRED_VOTE)
//
//        if (expirationTime <= System.currentTimeMillis()) { // 종료된 투표 또는 유효하지 않는 투표면
//            handleErrorResponse(session, EXPIRED_VOTE)
//            webSocketManager.stopWebSocketForVote(voteUuid)
//            return
//        }
//
//        val remainingTimeMillis = expirationTime - System.currentTimeMillis()
//        if (webSocketManager.restoreWebSocketSession(voteUuid) == null) { // 기존 세션이 없는 경우(첫 접속 유저) 타이머 설정 및 채널 초기화
//            webSocketManager.setWebSocketTimer(voteUuid, remainingTimeMillis)
//            webSocketManager.startWebSocketForVote(voteUuid, (remainingTimeMillis / 1000 / 60).toInt())
//        }
//
//        webSocketManager.saveWebSocketSession(voteUuid, session)
//
//        val voteLimit = voteService.getVoteLimit(voteUuid)
//        val initialWebSocketResponse = WebSocketInitialResponse(
//            voteLimit = voteLimit,
//            voteEndTime = expirationTime
//        )
//        sendResponse(session, WebSocketResponse("연결에 성공했습니다.", initialWebSocketResponse))
//    }
//
//    private fun sendResponse(session: WebSocketSession, response: Any) {
//        val jsonResponse = objectMapper.writeValueAsString(response)
//        session.sendMessage(TextMessage(jsonResponse))
//    }
//
//    private fun handleErrorResponse(session: WebSocketSession, errorStatus: WebSocketResponseStatus) {
//        sendResponse(session, WebSocketErrorResponse.fromStatus(errorStatus))
//        session.close()
//    }
//
//    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
//        try {
//            val payload = objectMapper.readValue(message.payload.toString(), WebSocketRequest::class.java)
//            when (payload.action) {
//                "ADD_OPTION" -> handleAddOption(session, payload)
//                else -> handleErrorResponse(session, UNKNOWN_ACTION)
//            }
//        } catch (e: Exception) {
//            handleErrorResponse(session, INVALID_REQUEST_FORMAT)
//        }
//    }
//
//    private fun handleAddOption(session: WebSocketSession, payload: WebSocketRequest) {
//        try {
//            val nickname = session.attributes["nickname"] as? String ?: return handleErrorResponse(session, NO_NICKNAME)
//            val voteUuid = session.attributes["voteUuid"] as? String ?: return handleErrorResponse(session, MISSING_VOTE_UUID)
//
//            val newOption = voteService.addOption(voteUuid, nickname, payload.optionText, payload.optionColor)
//            val response = WebSocketResponse("새로운 옵션이 추가되었습니다.", newOption)
//            webSocketManager.broadcastToAll(payload.voteUuid, response)
//        } catch (e: Exception) {
//            handleErrorResponse(session, GENERAL_ERROR)
//        }
//    }
//
//    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
//        val voteUuid = session.attributes["voteUuid"] as? String
//        if (voteUuid != null) {
//            webSocketManager.stopWebSocketForVote(voteUuid)
//        }
//    }
//
//    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
//        val voteUuid = session.attributes["voteUuid"] as? String
//        if (voteUuid != null) {
//            webSocketManager.stopWebSocketForVote(voteUuid)
//        }
//    }
//
//    override fun supportsPartialMessages(): Boolean = false
//}
