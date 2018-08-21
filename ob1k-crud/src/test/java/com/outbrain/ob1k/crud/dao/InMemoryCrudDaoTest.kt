package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.outbrain.ob1k.crud.CrudApplication
import com.outbrain.ob1k.crud.model.EFieldType
import com.outbrain.ob1k.crud.model.EntityFields

class InMemoryCrudDaoTest : CrudDaoTestBase() {


    override fun personDao(): ICrudAsyncDao<JsonObject> {
        val crudApplication = CrudApplication().withEntity("person", EntityFields()
                .with("id", EFieldType.NUMBER)
                .with("name", EFieldType.STRING)
                .with("email", EFieldType.STRING)
                .with("boolean", EFieldType.BOOLEAN)
                .get())
        crudApplication("person", "id")?.readOnly = true
        return InMemoryCrudDao(resourceName = "person")
    }
}