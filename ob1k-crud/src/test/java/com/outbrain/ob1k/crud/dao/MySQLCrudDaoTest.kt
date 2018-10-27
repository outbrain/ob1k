package com.outbrain.ob1k.crud.dao

import com.outbrain.ob1k.crud.CrudApplication
import com.outbrain.ob1k.crud.example.ELiveness
import com.outbrain.ob1k.crud.model.EFieldType
import com.outbrain.ob1k.crud.model.EntityField
import com.outbrain.ob1k.db.BasicDao
import com.outbrain.ob1k.db.MySqlConnectionPoolBuilder

@org.junit.Ignore
class MySQLCrudDaoTest : CrudDaoTestBase() {

    override fun crudApplication() = CrudApplication(BasicDao(MySqlConnectionPoolBuilder
            .newBuilder("host", 3307, "user")
            .forDatabase("outbrain")
            .build()), "obcp_crud_person,obcj_crud_job,obca_crud_address")
            .referenceManyAsList("person", "address")
            .withLiveness()


    private fun CrudApplication.withLiveness(): CrudApplication {
        get("person").let {
            it.fields += EntityField(dbName = "obcp_alive",
                    name = "liveness",
                    label = "Liveness",
                    type = EFieldType.SELECT_BY_IDX,
                    choices = ELiveness.values().map { it.name })
        }
        return this
    }
}