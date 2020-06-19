package com.coveros.r3z.domainobjects

data class User(val id: Long, val name: String) {
    init {
        assert(id < 100_000_000) { "There is no way on earth this company has ever or will " +
                "ever have more than even a million employees" }
    }
}


