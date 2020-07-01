package com.coveros.r3z.persistence.microorm

import java.sql.ResultSet

/**
 * Provides an interface to some helper methods
 * for running commands on the database, mainly to
 * avoid unnecessary duplication when coding SQL commands.
 */
interface IDbAccessHelper {

    /**
     * Update or insert data in the database
     */
    fun executeUpdate(description: String, preparedStatement: String, vararg params: Any?) : Long

    /**
     * Run a command on the database that may receive a result.
     * Handle the result by providing a SqlData that has an Extractor
     *   An extractor is simply a function that, given a resultset, returns something of type R.
     *   What is R?  That's up to you.
     */
    fun <R : Any> runQuery(description: String,
                           preparedStatement: String,
                           extractor : (ResultSet) -> R?,
                           vararg params: Any?) : R?

}