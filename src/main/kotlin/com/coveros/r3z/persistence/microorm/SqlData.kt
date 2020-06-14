package com.coveros.r3z.persistence.microorm

import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.reflect.KClass


/**
 * This class encapsulates some of the actions related to
 * injecting data into a prepared SQL statement, so that
 * we are able to summarize what we want done without
 * all the annoying boilerplate.  See examples like PersistenceLayer.saveNewBorrower
 *
 *
 * Was necessary to suppress the nullness warnings on this class due to its
 * use of generics.
 * The generic R is the result type - if we ask for a string, R would be a String.
 * On the other hand if R might be a compound type, like Employee.
 * @param R - the return type of this SqlData
 */
 class SqlData<R : Any>(
        /**
         * A summary description of what this SQL is doing.
         */
        val description: String,
        /**
         * This is the String text of the SQL prepared statement.  We're using PostgreSQL,
         * see https://jdbc.postgresql.org/documentation/81/server-prepare.html
         */
        val preparedStatement: String,
        /**
         * A generic function - takes a [ResultSet] straight from the database,
         * and then carries out actions on it, per the user's intentions, to convert it
         * into something of type [R].
         */
        val extractor: (ResultSet) -> R? = {_ -> null},
        /**
         * The data that we will inject to the SQL statement.
         */
        params : Array<out Any?>,
        /**
         * The data that we will inject to the SQL statement.
         */
        private val parameterList : MutableList<ParameterObject<*>> = mutableListOf()
        ) {

    init {
        if (params.isNotEmpty()) {
            generateParams(params)
        }
    }

    /**
     * Loads the parameters for this SQL
     * @param params
     */
    private fun generateParams(params: Array<out Any?>) {
        for (p in params) {
            if (p != null) {
                addParameter(p, p::class)
            }
        }
    }

    /**
     * A list of the parameters to a particular SQL statement.
     * Add to this list in the order of the statement.
     * For example,
     * for SELECT * FROM USERS WHERE a = ? and b = ?
     *
     *
     * first add the parameter for a, then for b.
     *
     * @param data  a particular item of data.  Any object will do.  Look at [.applyParametersToPreparedStatement]
     * to see what we can process.
     * @param clazz the class of the thing.  I would rather not use reflection, let's keep it above board for now.
     */
    private fun <T : Any> addParameter(data: Any, clazz: KClass<out T>) {
        parameterList.add(ParameterObject(data, clazz))
    }

    /**
     * Loop through the parameters that have been added and
     * serially add them to the prepared statement.
     *
     * @param st a prepared statement
     */
    fun applyParametersToPreparedStatement(st: PreparedStatement) {
        try {
            for (i in 1..parameterList.size) {
                val p: ParameterObject<*> = parameterList[i - 1]
                when {
                    p.type === String::class -> {
                        st.setString(i, p.data as String)
                    }
                    p.type === Int::class -> {
                        st.setInt(i, (p.data as Int))
                    }
                    p.type === Long::class -> {
                        st.setLong(i, (p.data as Long))
                    }
                    p.type === Date::class -> {
                        st.setDate(i, p.data as Date)
                    }
                }
            }
        } catch (e: SQLException) {
            throw SqlRuntimeException(e)
        }
    }

}