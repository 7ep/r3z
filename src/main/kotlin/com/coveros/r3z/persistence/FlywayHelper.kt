package com.coveros.r3z.persistence

import org.flywaydb.core.Flyway
import javax.sql.DataSource

/*
  * ==========================================================
  * ==========================================================
  *
  *  Database migration code - using FlywayDb
  *
  * ==========================================================
  * ==========================================================
  */
class FlywayHelper(private val dataSource : DataSource) {

    /**
     * Burns the database down to initial state.  Wipes out everything, pretty much.
     */
    fun cleanDatabase() {
        val flyway: Flyway = configureFlyway()
        flyway.clean()
    }

    /**
     * Runs the migration scripts to put the database into
     * a state that is expected for a particular version.
     */
    fun migrateDatabase() {
        val flyway: Flyway = configureFlyway()
        flyway.migrate()
    }

    fun configureFlyway(): Flyway {
        return Flyway.configure()
                .schemas("ADMINISTRATIVE", "TIMEANDEXPENSES", "AUTH")
                .dataSource(dataSource)
                .load()
    }

}