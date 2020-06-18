package com.coveros.r3z.domainobjects

enum class StatusEnum {SUCCESS, FAILURE, INVALID_PROJECT}

class RecordTimeResult(val id: Long?, val status: StatusEnum) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecordTimeResult

        if (id != other.id) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + status.hashCode()
        return result
    }

    override fun toString(): String {
        return "RecordTimeResult(id=$id, status=$status)"
    }

}