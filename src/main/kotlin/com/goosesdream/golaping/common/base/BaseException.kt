package com.goosesdream.golaping.common.base

import com.goosesdream.golaping.common.enums.BaseResponseStatus

class BaseException(
    val status: BaseResponseStatus
) : RuntimeException()