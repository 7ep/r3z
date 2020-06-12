package com.coveros.r3z.domainobjects

class TimeEntry(val user: User, val project: Project, val time: Time, val details: Details) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
