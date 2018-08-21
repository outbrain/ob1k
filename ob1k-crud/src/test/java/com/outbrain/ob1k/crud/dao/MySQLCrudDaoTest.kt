package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.outbrain.ob1k.crud.CrudApplication
import com.outbrain.ob1k.db.BasicDao
import com.outbrain.ob1k.db.MySqlConnectionPoolBuilder
import org.junit.Ignore

@Ignore
class MySQLCrudDaoTest : CrudDaoTestBase() {
    
    override fun personDao(): ICrudAsyncDao<JsonObject> {
        val connectionPool = MySqlConnectionPoolBuilder
                .newBuilder("localhost", 3307, "username")
                .password("password")
                .forDatabase("database")
                .build()
        val dao = BasicDao(connectionPool)
        return CrudApplication(dao, "obcp_crud_person").newMySQLDao(dao, "obcp_crud_person")
    }
}