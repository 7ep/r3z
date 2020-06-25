package com.coveros.r3z.domainobjects

data class Details(val value : String = "") {
    init {
        // This max length is reinforced in V1_Create_person_table.sql
        assert(value.length <= 500) { "no reason why details for a time entry would ever need to be more than 500 chars. " +
                "if you have more to say than the lord's prayer, you're probably doing it wrong." }
    }
}