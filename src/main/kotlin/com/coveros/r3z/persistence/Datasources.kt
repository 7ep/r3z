package com.coveros.r3z.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.h2.jdbcx.JdbcConnectionPool
import javax.sql.DataSource

/**
 * H2 provides this connection pool
 */
fun getFileBasedDatabaseConnectionPool(): DataSource {
    return JdbcConnectionPool.create(
        "jdbc:h2:./build/db/training;AUTO_SERVER=TRUE;MODE=POSTGRESQL", "", "") as DataSource
}

/**
 * H2 provides this connection pool
 */
fun getMemoryBasedDatabaseConnectionPool(): DataSource {
    return JdbcConnectionPool.create(
        "jdbc:h2:mem:training;MODE=POSTGRESQL", "", "") as DataSource
}

/**
 * One of the fastest connection pools, file-based
 */
fun getHikariFileBasedConnectionPool(): DataSource {
    val config = HikariConfig()
    config.jdbcUrl = "jdbc:h2:./build/db/training;AUTO_SERVER=TRUE;MODE=POSTGRESQL"
    config.username = ""
    config.password = ""
    setDefaultHikariConfig(config)
    return HikariDataSource(config)
}

/**
 * One of the fastest connection pools, memory-based
 * TODO: Note - this isn't current used.  When using this
 * in Tomcat 9, it was noted that the first call would
 * fail.  It is something happening in Hikari, because when
 * I switched to H2's connection pool the problem went
 * away.  Solve that problem if you want to use this.
 */
fun getHikariMemoryBasedConnectionPool(): DataSource {
    val config = HikariConfig()
    config.jdbcUrl = "jdbc:h2:mem:training;MODE=POSTGRESQL"
    config.username = ""
    config.password = ""
    setDefaultHikariConfig(config)
    return HikariDataSource(config)
}

private fun setDefaultHikariConfig(config: HikariConfig) {
    config.addDataSourceProperty("cachePrepStmts", "true")
    config.addDataSourceProperty("prepStmtCacheSize", "250")
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
}