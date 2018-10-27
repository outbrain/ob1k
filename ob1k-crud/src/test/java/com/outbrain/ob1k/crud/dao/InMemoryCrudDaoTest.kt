package com.outbrain.ob1k.crud.dao

import com.outbrain.ob1k.crud.CrudApplication
import com.outbrain.ob1k.crud.example.*
import java.util.concurrent.Executors

class InMemoryCrudDaoTest : CrudDaoTestBase() {

    override fun crudApplication(): CrudApplication {
        val jobDao = JobDao()
        val personDao = PersonDao(jobDao)
        val executor = Executors.newCachedThreadPool()
        return CrudApplication()
                .withEntity(Person::class.java)
                .withEntity(Job::class.java)
                .addReferenceCascaded("person","addresses", Address::class.java)
                .addReference("job", "person")
                .withCustomDao(jobDao, "yyyy-MM-dd", executor)
                .withCustomDao(personDao, "yyyy-MM-dd", executor)
    }
}