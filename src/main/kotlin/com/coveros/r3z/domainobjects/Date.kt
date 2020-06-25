package com.coveros.r3z.domainobjects

import java.time.LocalDate

class Date(calendarDate: String) {

    val value = java.sql.Date(LocalDate.parse(calendarDate).toEpochDay())
}
