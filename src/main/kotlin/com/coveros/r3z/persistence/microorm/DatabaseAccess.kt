package com.coveros.r3z.persistence.microorm

import com.coveros.r3z.domainobjects.TimeEntry

interface DatabaseAccess {
    /**
     * Adds a new time entry to the database.
     * @return the id of the new TimeEntry
     */
    fun createTimeEntry(entry : TimeEntry) : Int

    /**
     * gets the data of a particular time entry from
     * the database
     * @param id the identifier of a particular time entry
     * @return the time entry data
     */
    fun readTimeEntry(id : Int) : TimeEntry

}