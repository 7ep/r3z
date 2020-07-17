diff --git a/src/main/kotlin/com/coveros/r3z/timerecording/TimeEntryPersistence.kt b/src/main/kotlin/com/coveros/r3z/timerecording/TimeEntryPersistence.kt
deleted file mode 100644
index 294811f..0000000
--- a/src/main/kotlin/com/coveros/r3z/timerecording/TimeEntryPersistence.kt
+++ /dev/null
@@ -1,85 +0,0 @@
-package com.coveros.r3z.timerecording
-
-import com.coveros.r3z.domainobjects.*
-import com.coveros.r3z.persistence.microorm.DbAccessHelper
-import org.slf4j.Logger
-import org.slf4j.LoggerFactory
-import java.sql.ResultSet
-
-class TimeEntryPersistence(private val dbHelper: DbAccessHelper) {
-
-    companion object {
-        val log : Logger = LoggerFactory.getLogger(TimeEntryPersistence::class.java)
-    }
-
-    fun persistNewTimeEntry(entry: TimeEntry): Long {
-        log.info("persisting a new timeEntry, $entry")
-        return dbHelper.executeUpdate(
-                "INSERT INTO TIMEANDEXPENSES.TIMEENTRY (user, project, time_in_minutes, date, details) VALUES (?, ?,?, ?, ?);",
-                entry.user.id, entry.project.id, entry.time.numberOfMinutes, entry.date.sqlDate, entry.details.value)
-    }
-
-    fun persistNewProject(projectName: ProjectName) : Project {
-        assert(projectName.value.isNotEmpty()) {"Project name cannot be empty"}
-        log.info("Recording a new project, ${projectName.value}, to the database")
-
-        val id = dbHelper.executeUpdate(
-                "INSERT INTO TIMEANDEXPENSES.PROJECT (name) VALUES (?);",
-                projectName.value)
-
-        assert(id > 0) {"A valid project will receive a positive id"}
-        return Project(id, projectName.value)
-    }
-
-    fun persistNewUser(username: String): User {
-        assert(username.isNotEmpty())
-        log.info("Recording a new user, $username, to the database")
-
-        val newId = dbHelper.executeUpdate(
-            "INSERT INTO TIMEANDEXPENSES.PERSON (name) VALUES (?);", username)
-
-        assert(newId > 0) {"A valid user will receive a positive id"}
-        return User(newId, username)
-    }
-
-    /**
-     * Provided a user and date, give the number of minutes they worked on that date
-     */
-    fun queryMinutesRecorded(user: User, date: Date): Long? {
-        return dbHelper.runQuery(
-                "SELECT SUM (TIME_IN_MINUTES) AS total FROM TIMEANDEXPENSES.TIMEENTRY WHERE user=(?) AND date=(?);",
-                { r : ResultSet -> r.getLong("total")},
-                user.id, date.sqlDate)
-    }
-
-    fun readTimeEntries(user: User): List<TimeEntry>? {
-
-
-        val extractor: (ResultSet) -> List<TimeEntry> = { r ->
-            val myList: MutableList<TimeEntry> = mutableListOf()
-            do {
-                myList.add(
-                    TimeEntry(
-                        User(r.getLong("userid"), r.getString("username")),
-                        Project(r.getLong("projectid"), r.getString("projectname")),
-                        Time(r.getInt("time")),
-                        Date.convertSqlDateToOurDate(r.getDate("date")),
-                        Details(r.getString("details"))
-                    )
-                )
-            } while (r.next())
-            myList.toList()
-        }
-        return dbHelper.runQuery(
-            """
-        SELECT te.user as userid, p.name as username, te.project as projectid, pj.name as projectname, 
-            te.time_in_minutes as time, te.date, te.details 
-                FROM TIMEANDEXPENSES.TIMEENTRY as te 
-                JOIN timeandexpenses.person as p on te.user = p.id
-                JOIN timeandexpenses.project as pj on te.project = pj.id
-                WHERE user=(?);
-            """,
-            extractor,
-            user.id)
-    }
-}
\ No newline at end of file
diff --git a/src/main/kotlin/coverosR3z/timerecording/ITimeEntryPersistence.kt b/src/main/kotlin/coverosR3z/timerecording/ITimeEntryPersistence.kt
new file mode 100644
index 0000000..87d25d6
--- /dev/null
+++ b/src/main/kotlin/coverosR3z/timerecording/ITimeEntryPersistence.kt
@@ -0,0 +1,16 @@
+package coverosR3z.timerecording
+
+import coverosR3z.domainobjects.*
+
+interface ITimeEntryPersistence {
+    fun persistNewTimeEntry(entry: TimeEntryPreDatabase)
+    fun persistNewProject(projectName: ProjectName) : Project
+    fun persistNewUser(username: UserName): User
+
+    /**
+     * Provided a user and date, give the number of minutes they worked on that date
+     */
+    fun queryMinutesRecorded(user: User, date: Date): Long
+    fun readTimeEntries(user: User): List<TimeEntry>?
+
+}
\ No newline at end of file
diff --git a/src/main/kotlin/coverosR3z/timerecording/TimeEntryPersistence.kt b/src/main/kotlin/coverosR3z/timerecording/TimeEntryPersistence.kt
new file mode 100644
index 0000000..20464e1
--- /dev/null
+++ b/src/main/kotlin/coverosR3z/timerecording/TimeEntryPersistence.kt
@@ -0,0 +1,50 @@
+package coverosR3z.timerecording
+
+import coverosR3z.domainobjects.*
+import coverosR3z.logging.logInfo
+import coverosR3z.persistence.ProjectIntegrityViolationException
+import coverosR3z.persistence.PureMemoryDatabase
+import coverosR3z.persistence.UserIntegrityViolationException
+
+class TimeEntryPersistence(val pmd : PureMemoryDatabase) : ITimeEntryPersistence {
+
+    override fun persistNewTimeEntry(entry: TimeEntryPreDatabase) {
+        logInfo("persisting a new timeEntry, $entry")
+        isEntryValid(entry)
+        pmd.addTimeEntry(entry)
+    }
+
+    /**
+     * This will throw an exception if the project or user in
+     * this timeentry don't exist in the list of projects / users
+     */
+    private fun isEntryValid(entry: TimeEntryPreDatabase) {
+        pmd.getProjectById(entry.project.id) ?: throw ProjectIntegrityViolationException()
+        pmd.getUserById(entry.project.id) ?: throw UserIntegrityViolationException()
+    }
+
+    override fun persistNewProject(projectName: ProjectName): Project {
+        logInfo("Recording a new project, ${projectName.value}, to the database")
+        val newId = pmd.addNewProject(projectName)
+        assert(newId > 0) {"A valid project will receive a positive id"}
+        return Project(newId, projectName.value)
+    }
+
+    override fun persistNewUser(username: UserName): User {
+        logInfo("Recording a new user, ${username.value}, to the database")
+
+        val newId = pmd.addNewUser(username)
+
+        assert(newId > 0) {"A valid user will receive a positive id"}
+        return User(newId, username.value)
+    }
+
+    override fun queryMinutesRecorded(user: User, date: Date): Long {
+        val minutes = pmd.getMinutesRecordedOnDate(user, date)
+        return minutes.toLong()
+    }
+
+    override fun readTimeEntries(user: User): List<TimeEntry>? {
+        return pmd.getAllTimeEntriesForUser(user)
+    }
+}
\ No newline at end of file
diff --git a/src/test/kotlin/coverosR3z/persistence/FakeTimeEntryPersistence.kt b/src/test/kotlin/coverosR3z/persistence/FakeTimeEntryPersistence.kt
new file mode 100644
index 0000000..5767508
--- /dev/null
+++ b/src/test/kotlin/coverosR3z/persistence/FakeTimeEntryPersistence.kt
@@ -0,0 +1,36 @@
+package coverosR3z.persistence
+
+import coverosR3z.DEFAULT_PROJECT
+import coverosR3z.DEFAULT_USER
+import coverosR3z.domainobjects.*
+import coverosR3z.timerecording.ITimeEntryPersistence
+
+
+class FakeTimeEntryPersistence(
+        val minutesRecorded: Long = 0L,
+        val persistNewTimeEntryBehavior : () -> Unit = {},
+        val persistNewProjectBehavior : () -> Project = { DEFAULT_PROJECT }) : ITimeEntryPersistence {
+
+
+    override fun persistNewTimeEntry(entry: TimeEntryPreDatabase) {
+        persistNewTimeEntryBehavior()
+    }
+
+    override fun persistNewProject(projectName: ProjectName): Project {
+        return persistNewProjectBehavior()
+    }
+
+    override fun persistNewUser(username: UserName): User {
+        return DEFAULT_USER
+    }
+
+
+    override fun queryMinutesRecorded(user: User, date: Date): Long {
+        return minutesRecorded
+    }
+
+    override fun readTimeEntries(user: User): List<TimeEntry>? {
+        return listOf()
+    }
+
+}
\ No newline at end of file
