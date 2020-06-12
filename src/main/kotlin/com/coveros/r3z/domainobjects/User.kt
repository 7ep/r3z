package com.coveros.r3z.domainobjects

class User(val id: Long, val name: String) {
    init {
        assert(id < 100_000_000) { "There is no way on earth this company has ever or will " +
                "ever have more than even a million employees" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }


}


