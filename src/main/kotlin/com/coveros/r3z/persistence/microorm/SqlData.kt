package com.coveros.r3z.persistence.microorm

import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * This class encapsulates necessary data for communication
 * with the database.  In particular,
 * 1) the SQL command to run (preparedStatement)
 * 2) an extractor (if necessary), which is code run to convert the database's [ResultSet] to something we want
 * 3) params - the data we are including as part of our SQL command.  This makes the
 *    communication safer (much less likely to see SQL injection).
 *
 * @param R - The generic R is the result type - if we ask for a string, R would be a String.
 *            On the other hand R might be a compound type, like Employee.
 */
 class SqlData<R : Any>(

        /**
         * A summary description of what this SQL is doing.
         * This way, if someone investigates a given SqlData, it's clear
         * what its purpose is.
         */
        private val description: String,

        /**
         * This is the String text of the SQL prepared statement.  We're using PostgreSQL,
         * see https://jdbc.postgresql.org/documentation/81/server-prepare.html
         */
        val preparedStatement: String,

        /**
         * A generic function - takes a [ResultSet] straight from the database,
         * and then carries out actions on it, per the user's intentions, to convert it
         * into something of type [R].
         *
         * Providing a default of {_ -> null} in case the user provides nothing.
         */
        val extractor: (ResultSet) -> R? = {_ -> null},

        /**
         * The data that we will inject to the SQL statement.
         */
        val params : Array<out Any?>

        ) {


    /**
     * Loop through the parameters that have been added and
     * serially add them to the prepared statement.
     *
     * @param st a prepared statement
     */
    fun applyParametersToPreparedStatement(st: PreparedStatement) {
        for (i in 1..params.size) {
            val p: Any? = params[i - 1]
            if (p != null) {
                when (p::class) {
                    String::class -> { st.setString (i, p as String) }
                    Int::class ->    { st.setInt    (i, p as Int)    }
                    Long::class ->   { st.setLong   (i, p as Long)   }
                    Date::class ->   { st.setDate   (i, p as Date)   }
                    else -> {
                        throw Exception("parameter " + p + " had a type of " + p::class + " which isn't recognized as a SQL data type.")
                    }
                }
            }
        }
    }

}