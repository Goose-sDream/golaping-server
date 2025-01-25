package com.goosesdream.golaping.common.websocket

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.goosesdream.golaping.common.enums.WebSocketResponseStatus.*

@JsonPropertyOrder("isSuccess", "message", "result")
data class WebSocketResponse<T>(
    @JsonProperty("isSuccess")
    val isSuccess: Boolean,
    val message: String,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val result: T? = null
) {
    constructor() : this(
        isSuccess = true,
        message = SUCCESS.message,
        result = null
    )

    constructor(result: T) : this(
        isSuccess = true,
        message = SUCCESS.message,
        result = result
    )
}