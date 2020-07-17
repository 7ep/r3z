package coverosR3z.persistence

import org.h2.jdbcx.JdbcConnectionPool
import javax.sql.DataSource

/**
 * H2 provides this connection pool
 */
fun getMemoryBasedDatabaseConnectionPool(): DataSource {
       return JdbcConnectionPool.create(
      "jdbc:h2:mem:training;MODE=POSTGRESQL", "", "") as DataSource
}
