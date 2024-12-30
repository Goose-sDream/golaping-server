package com.goosesdream.golaping.common.base

class BaseException(
    val status: BaseResponseStatus
) : RuntimeException()