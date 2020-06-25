package com.coveros.r3z

import com.coveros.r3z.persistence.getMemoryBasedDatabaseConnectionPool
import com.coveros.r3z.persistence.microorm.DbAccessHelper
import com.coveros.r3z.persistence.microorm.IDbAccessHelper
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener

@WebListener
class WebAppListener : ServletContextListener {

    private val db = getMemoryBasedDatabaseConnectionPool()

    override fun contextInitialized(sce: ServletContextEvent?) {
        //  clean the database and configure the schema
        val dbAccessHelper: IDbAccessHelper =
            DbAccessHelper(db)
        dbAccessHelper.cleanDatabase()
        dbAccessHelper.migrateDatabase()
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
        db.connection.close()
    }
}