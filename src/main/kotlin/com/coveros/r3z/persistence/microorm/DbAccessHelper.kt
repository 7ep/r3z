package com.coveros.r3z.persistence.microorm

import java.sql.*
import javax.sql.DataSource

/**
 * ==========================================================
 * ==========================================================
 *
 *  Access helper
 *
 *      These functions help to remove some of the duplication
 *      typically necessary when sending requests to the
 *      database.
 *
 * ==========================================================
 * ==========================================================
 */
class DbAccessHelper(private val dataSource : DataSource) : IDbAccessHelper {

    /**
     * This command provides a template to execute updates (including inserts) on the database
     */
    override fun executeUpdate(description: String, preparedStatement: String, vararg params: Any?) : Long {
        val sqlData: SqlData<Any> =
            SqlData(
                description,
                preparedStatement,
                params = params
            )
            return dataSource.connection.use {
                connection -> prepareStatementWithKeys(sqlData, connection)
                    .use { st -> executeUpdateOnPreparedStatement(sqlData, st) } }
    }

    override fun <R : Any> runQuery(description: String,
                                    preparedStatement: String,
                                    extractor : (ResultSet) -> R?,
                                    vararg params: Any?): R? {
        val sqlData: SqlData<R> =
                SqlData(
                        description,
                        preparedStatement,
                        extractor,
                        params
                )
        dataSource.connection.use { connection ->
            connection.prepareStatement(sqlData.sqlCommand).use { st ->
                applyParametersToPreparedStatement(st, sqlData.params)
                st.executeQuery().use { resultSet ->
                    // we need to move forward to the first row of data,
                    // unless there is no data, in which case we just return null
                    return if (resultSet.next()) {
                        sqlData.extractor(resultSet)
                    } else {
                        null
                    }
                }
            }
        }
    }

    /**
     * Some functions associated with this class that don't access instance state.
     * That is, all these methods do their work solely with the parameters they are given, making them
     * easier to test and evaluate correctness.
     */
    companion object {
        private fun <T : Any> executeUpdateOnPreparedStatement(sqlData: SqlData<T>, st: PreparedStatement): Long {
            applyParametersToPreparedStatement(st, sqlData.params)
            st.executeUpdate()
            return obtainNewKey(st)
        }

        /**
         * used to obtain the new key for an entry to a table
         * in the database.  That is, for example, if we added
         * a new user to tht user table with a generated id of
         * 1 and a name of "bob", we return 1.
         */
        private fun obtainNewKey(st: PreparedStatement) : Long {
            st.generatedKeys.use { generatedKeys ->
                generatedKeys.next()
                val newId: Long = generatedKeys.getLong(1)
                assert(newId > 0)
                return newId
            }
        }

        /**
         * Loop through the parameters that have been added and
         * serially add them to the prepared statement.
         *
         * @param st a prepared statement
         */
        fun applyParametersToPreparedStatement(st: PreparedStatement, params : Array<out Any?>) {
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

        /**
         * A helper method.  Simply creates a prepared statement that
         * always returns the generated keys from the database, like
         * when you insert a new row of data in a table with auto-generating primary key.
         *
         * @param sqlData    see [SqlData]
         * @param connection a typical [Connection]
         */
        private fun <T : Any> prepareStatementWithKeys(sqlData: SqlData<T>, connection: Connection): PreparedStatement {
            return connection.prepareStatement(
                    sqlData.sqlCommand,
                    Statement.RETURN_GENERATED_KEYS)
        }
    }

}