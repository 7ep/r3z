package com.coveros.r3z.persistence

interface IDbAccessHelper {

    /**
     * Update data in the database
     */
    fun executeUpdate(description: String, preparedStatement: String, vararg params: Any?)

    /**
     * Insert new data into the database
     */
    fun executeInsert(description: String, preparedStatement: String, vararg params: Any?): Long

    /**
     * Run a command on the database that may receive a result.
     * Handle the result by providing a SqlData that has an Extractor
     *   An extractor is simply a function that, given a resultset, returns something of type R.
     *   What is R?  That's up to you.
     */
    fun <R : Any> runQuery(sqlData: SqlData<R>): R?

    /**
     * Burns the database down to initial state.  Wipes out everything, pretty much.
     */
    fun cleanDatabase()

    /**
     * Runs the migration scripts to put the database into
     * a state that is expected for a particular version.
     */
    fun migrateDatabase()
}