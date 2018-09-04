package com.outbrain.ob1k.crud.dao

import com.outbrain.ob1k.crud.CrudApplication
import com.outbrain.ob1k.db.BasicDao
import com.outbrain.ob1k.db.MySqlConnectionPoolBuilder

@org.junit.Ignore
class MySQLCrudDaoTest : CrudDaoTestBase() {

    override fun crudApplication() = CrudApplication(BasicDao(MySqlConnectionPoolBuilder
            .newBuilder("host", 3307, "user")
            .password("password")
            .forDatabase("outbrain")
            .build()), "obcp_crud_person,obcj_crud_job")
}