package com.coveros.r3z.persistence

import com.coveros.r3z.authentication.authPersistence
import com.coveros.r3z.domainobjects.User
import com.coveros.r3z.persistence.microorm.DbAccessHelper
import com.coveros.r3z.persistence.microorm.IDbAccessHelper
import org.junit.Assert.assertEquals
import org.junit.Test


class DbAccessHelperTests {

    @Test
    fun `happy path - hit a database at all`() {
        val ds = getMemoryBasedDatabaseConnectionPool()
        DbAccessHelper(ds)
    }

    @Test fun `happy path with flyway`() {
        val ds = getMemoryBasedDatabaseConnectionPool()
        val dbAccessHelper : IDbAccessHelper =
            DbAccessHelper(ds)
        dbAccessHelper.cleanDatabase()
        dbAccessHelper.migrateDatabase()
    }

    @Test fun `testing adding a user`() {
        val ds = getMemoryBasedDatabaseConnectionPool()
        val dbAccessHelper : IDbAccessHelper =
            DbAccessHelper(ds)
        dbAccessHelper.cleanDatabase()
        dbAccessHelper.migrateDatabase()

        val b = authPersistence(ds)

        val user : User = b.addUser("this is someone's name")

        assertEquals(User(1, "this is someone's name"), user)
    }

    @Test fun `testing adding a user and retrieving it`() {
        val ds = getMemoryBasedDatabaseConnectionPool()
        val dbAccessHelper : IDbAccessHelper =
            DbAccessHelper(ds)
        dbAccessHelper.cleanDatabase()
        dbAccessHelper.migrateDatabase()
        val expectedUsers : List<User>? = listOf(User(1, "this is someone's name"))
        val b = authPersistence(ds)

        b.addUser("this is someone's name")

        val allUsers = b.getAllUsers()
        assertEquals(expectedUsers, allUsers)
    }



}