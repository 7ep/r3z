package com.coveros.r3z.persistence

import com.coveros.r3z.domainobjects.User
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.Test
import kotlin.test.assertEquals


class DbAccessHelperTests {

    @Test
    fun `happy path - hit a database at all`() {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:h2:./build/db/training;AUTO_SERVER=TRUE;MODE=POSTGRESQL"
        config.username = "sa"
        config.password = ""
        config.addDataSourceProperty( "cachePrepStmts" , "true" )
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" )
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" )
        val ds = HikariDataSource(config)
        DbAccessHelper(ds)
    }

    @Test fun `happy path with flyway`() {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:h2:./build/db/training;AUTO_SERVER=TRUE;MODE=POSTGRESQL"
        config.username = "sa"
        config.password = ""
        config.addDataSourceProperty( "cachePrepStmts" , "true" )
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" )
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" )
        val ds = HikariDataSource(config)
        val dbAccessHelper : IDbAccessHelper = DbAccessHelper(ds)
        dbAccessHelper.cleanDatabase()
        dbAccessHelper.migrateDatabase()
    }

    @Test fun `testing adding a user`() {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:h2:./build/db/training;AUTO_SERVER=TRUE;MODE=POSTGRESQL"
        config.username = "sa"
        config.password = ""
        config.addDataSourceProperty( "cachePrepStmts" , "true" )
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" )
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" )
        val ds = HikariDataSource(config)
        val dbAccessHelper : IDbAccessHelper = DbAccessHelper(ds)
        dbAccessHelper.cleanDatabase()
        dbAccessHelper.migrateDatabase()

        val b = BusinessPersistenceLayer(ds)

        val user : User = b.addUser("this is someone's name")

        assertEquals(User(1, "this is someone's name"), user)
    }



}