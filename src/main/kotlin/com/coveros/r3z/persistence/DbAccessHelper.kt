package com.coveros.r3z.persistence

import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
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
    override fun executeUpdate(description: String, preparedStatement: String, vararg params: Any?) {
        val sqlData: SqlData<Any> = SqlData(description, preparedStatement, params=params)
        try {
            dataSource.connection.use {
                connection -> prepareStatementWithKeys(sqlData, connection)
                    .use { st -> executeUpdateOnPreparedStatement(sqlData, st) } }
        } catch (ex: SQLException) {
            throw SqlRuntimeException(ex)
        }
    }


    override fun executeInsert(
            description: String,
            preparedStatement: String,
            vararg params: Any?): Long {
        val sqlData: SqlData<Any> = SqlData(description, preparedStatement, params=params)
        try {
            dataSource.connection.use {
                connection -> prepareStatementWithKeys(sqlData, connection)
                    .use { st -> return executeInsertOnPreparedStatement(sqlData, st) } }
        } catch (ex: SQLException) {
            throw SqlRuntimeException(ex)
        }
    }


    private fun <T : Any> executeInsertOnPreparedStatement(sqlData: SqlData<T>, st: PreparedStatement): Long {
        sqlData.applyParametersToPreparedStatement(st)
        st.executeUpdate()
        st.generatedKeys.use { generatedKeys ->
            val newId: Long
            if (generatedKeys.next()) {
                newId = generatedKeys.getLong(1)
                assert(newId > 0)
            } else {
                throw SqlRuntimeException("failed Sql.  Description: " +
                        sqlData.description + " SQL code: " +
                        sqlData.preparedStatement)
            }
            return newId
        }
    }


    private fun <T : Any> executeUpdateOnPreparedStatement(sqlData: SqlData<T>, st: PreparedStatement) {
        sqlData.applyParametersToPreparedStatement(st)
        st.executeUpdate()
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
                sqlData.preparedStatement,
                Statement.RETURN_GENERATED_KEYS)
    }


    override fun <R : Any> runQuery(sqlData: SqlData<R>): R? {
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement(sqlData.preparedStatement).use { st ->
                    sqlData.applyParametersToPreparedStatement(st)
                    st.executeQuery().use { resultSet -> return sqlData.extractor(resultSet) }
                }
            }
        } catch (ex: SQLException) {
            throw SqlRuntimeException(ex)
        }
    }


    /*
     * ==========================================================
     * ==========================================================
     *
     *  Database migration code - using FlywayDb
     *
     * ==========================================================
     * ==========================================================
     */

    override fun cleanDatabase() {
        val flyway: Flyway = configureFlyway()
        flyway.clean()
    }

    override fun migrateDatabase() {
        val flyway: Flyway = configureFlyway()
        flyway.migrate()
    }

    private fun configureFlyway(): Flyway {
        return Flyway.configure()
                .schemas("ADMINISTRATIVE", "USER", "AUTH")
                .dataSource(dataSource)
                .load()
    }


}