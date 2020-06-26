package com.coveros.r3z.domainobjects

import java.time.LocalDate

class Date(val value : java.sql.Date) {

    constructor(calendarDate: String) : this(java.sql.Date(LocalDate.parse(calendarDate).toEpochDay()))

    constructor(millisecondsSinceEpoch: Long) : this(java.sql.Date(millisecondsSinceEpoch))

}


