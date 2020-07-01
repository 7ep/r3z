package com.coveros.r3z

import com.coveros.r3z.persistence.getMemoryBasedDatabaseConnectionPool
import com.coveros.r3z.persistence.microorm.FlywayHelper
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener

/**
 * This class exists for Servlet containers like Jetty
 * or Tomcat, which will call into this class expecting to
 * run these start and stop commands when the server stops or starts.
 *
 * Don't be concerned that the IDE shows the class as unused -
 * it's only because nothing else in the codebase is calling it,
 * but it *does* get called by the outside program.
 */
@WebListener
class WebAppListener : ServletContextListener {

    private val db = getMemoryBasedDatabaseConnectionPool()

    override fun contextInitialized(sce: ServletContextEvent?) {
        //  clean the database and configure the schema
        val flywayHelper = FlywayHelper(db)
        flywayHelper.cleanDatabase()
        flywayHelper.migrateDatabase()
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
        db.connection.close()
    }
}