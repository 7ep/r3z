package com.coveros.r3z.domainobjects

enum class StatusEnum {SUCCESS, FAILURE, INVALID_PROJECT, NULL}

data class RecordTimeResult(val id: Long?, val status: StatusEnum = StatusEnum.NULL)