package com.coveros.r3z.persistence

import com.coveros.r3z.domainobjects.User
import com.zaxxer.hikari.HikariDataSource

class BusinessPersistenceLayer(val ds: HikariDataSource) {

    private val db: IDbAccessHelper = DbAccessHelper(ds)

    /////////////////////////////
    //       business          //
    /////////////////////////////

    // authentication functions
    fun addUser(username: String): User {
        val newId = db.executeInsert(
            "Creates a new user in the database",
            "INSERT INTO USER.PERSON (name) VALUES (?);", username)
        return User(newId, username)
    }
}