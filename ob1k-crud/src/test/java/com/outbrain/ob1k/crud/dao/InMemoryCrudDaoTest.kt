package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.outbrain.ob1k.crud.CrudApplication
import com.outbrain.ob1k.crud.example.Job
import com.outbrain.ob1k.crud.example.JobDao
import com.outbrain.ob1k.crud.example.Person
import com.outbrain.ob1k.crud.example.PersonDao

class InMemoryCrudDaoTest : CrudDaoTestBase() {


    override fun getPersonAndJobDaos(): Pair<ICrudAsyncDao<JsonObject>, ICrudAsyncDao<JsonObject>> {
        val jobDaoDelegate = JobDao()
        val personDaoDelegate = PersonDao(jobDaoDelegate)
        val application = CrudApplication()
                .withEntity(Person::class.java)
                .withEntity(Job::class.java)
                .addReference("job", "person")
        val personDao = application.newCustomDao(personDaoDelegate, "yyyy-MM-dd")
        val jobDao = application.newCustomDao(jobDaoDelegate, "yyyy-MM-dd")
        return personDao to jobDao
    }
}