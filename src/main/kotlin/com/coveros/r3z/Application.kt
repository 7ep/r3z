package com.coveros.r3z

import com.coveros.r3z.domainobjects.User
import com.coveros.r3z.authentication.authPersistence
import com.coveros.r3z.persistence.microorm.DbAccessHelper
import com.coveros.r3z.persistence.microorm.IDbAccessHelper
import com.coveros.r3z.persistence.getMemoryBasedDatabaseConnectionPool
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.http.ContentType
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.jetty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    // set up the connection pool for the database
    val ds = getMemoryBasedDatabaseConnectionPool()
    val dbAccessHelper : IDbAccessHelper = DbAccessHelper(ds)
    dbAccessHelper.cleanDatabase()
    dbAccessHelper.migrateDatabase()


    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        get("/login") {
            call.respond(FreeMarkerContent("index.html", mapOf("data" to IndexData(listOf(1, 2, 3, 4))), ""))
        }

        post("/login") {
            val business = authPersistence(ds)
            val parameters = call.receiveParameters()
            val username = parameters["username"]
            var listOfUsers : List<User>? = listOf()
            if (username != null) {
                business.addUser(username)
                listOfUsers = business.getAllUsers()
            }
            call.respondTextWriter {
               appendln(parameters.toString())
                appendln(listOfUsers?.joinToString(";"))
            }
        }
    }
}


data class IndexData(val items: List<Int>)



