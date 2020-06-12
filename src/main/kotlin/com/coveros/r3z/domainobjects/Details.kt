package com.coveros.r3z.domainobjects

class Details(val value : String = "") {

    init {

        assert(value.length < 500) { "no reason why details for a time entry would ever need to be more than 500 chars. " +
                "if you have more to say than the lord's prayer, you're probably doing it wrong." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Details

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

}