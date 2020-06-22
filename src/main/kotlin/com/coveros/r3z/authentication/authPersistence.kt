package com.coveros.r3z.authentication

import com.coveros.r3z.domainobjects.User
import com.coveros.r3z.persistence.microorm.DbAccessHelper
import com.coveros.r3z.persistence.microorm.IDbAccessHelper
import com.coveros.r3z.persistence.microorm.SqlData
import java.sql.ResultSet
import javax.sql.DataSource

class authPersistence(ds: DataSource) {

    private val db: IDbAccessHelper = DbAccessHelper(ds)

    /////////////////////////////
    //       business          //
    /////////////////////////////

    // authentication functions
    fun addUser(username: String): User {
        assert(username.isNotEmpty())
        val newId = db.executeInsert(
            "Creates a new user in the database",
            "INSERT INTO USER.PERSON (name) VALUES (?);", username)
        assert(newId > 0)
        return User(newId, username)
    }

    fun getAllUsers(): List<User>? {
        val extractor: (ResultSet) -> List<User>? = { r ->
            val users : MutableList<User> = mutableListOf()
            while(r.next()) {
                users.add(User(1, r.getString("name")))
            }
            users.toList()
        }

        return db.runQuery(
            SqlData(
                "get all the users in the database",
                "SELECT NAME FROM USER.PERSON",
                extractor,
                arrayOf()
            )
        )
    }
}