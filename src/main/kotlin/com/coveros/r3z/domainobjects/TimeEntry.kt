package com.coveros.r3z.domainobjects

data class TimeEntry(val user: User, val project: Project, val time: Time, val details : Details = Details(""))