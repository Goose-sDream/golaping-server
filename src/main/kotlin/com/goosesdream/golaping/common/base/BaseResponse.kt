package com.goosesdream.golaping.common.base

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.goosesdream.golaping.common.base.BaseResponseStatus.SUCCESS

@JsonPropertyOrder("isSuccess", "code", "message", "result")
data class BaseResponse<T>(
    @JsonProperty("isSuccess")
    val isSuccess: Boolean,
    val message: String,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val result: T? = null
) {

    constructor(result: T) : this(
        isSuccess = SUCCESS.isSuccess,
        message = SUCCESS.message,
        result = result
    )

    constructor(status: BaseResponseStatus) : this(
        isSuccess = status.isSuccess,
        message = status.message,
    )
}