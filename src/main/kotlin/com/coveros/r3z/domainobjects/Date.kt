package com.coveros.r3z.domainobjects

import java.time.LocalDate

class Date() {
    constructor(date: java.sql.Date) {
        val value = date
    }
    constructor(calendarDate: String) {
        val value = java.sql.Date(LocalDate.parse(calendarDate).toEpochDay())
    }
    constructor(longDate: Long) {
        val value = java.sql.Date(longDate)
    }

}


