plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.3.72"

    // Apply the application plugin to add support for building a CLI application.
    application
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // server-tests allows us to write tests against the ktor
    // server engine
    testImplementation ("io.ktor:ktor-server-tests:1.3.2")

    // Jetty is a servlet container, but minimal / lightweight,
    // with a fast startup, and enables some nice ability to
    // hot-reload while running the server.
    implementation ("io.ktor:ktor-server-jetty:1.3.2")

    // the logging framework
    implementation ("ch.qos.logback:logback-classic:1.2.")

    // ktor framework essentials
    // ktor is a project by Jetbrains to provide many
    // building blocks to a great web application.  It's
    // a library more than a framework - that is, it doesn't
    // force you to follow patterns, it enables you with
    // well-built tools
    implementation ("io.ktor:ktor-server-core:1.3.2n")
    implementation ("io.ktor:ktor-server-host-common:1.3.2")



    // freemarker is a template engine
    implementation ("io.ktor:ktor-freemarker:1.3.2")

    // https://mvnrepository.com/artifact/com.zaxxer/HikariCP
    // also see https://github.com/brettwooldridge/HikariCP
    // a connection pool for our database.  This should enable
    // a much faster communication to any database that is used.
    implementation ("com.zaxxer:HikariCP:3.4.5")

}

application {
    // Define the main class for the application.
    mainClassName = "r3z.AppKt"
}
