package com.outbrain.ob1k.crud.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.outbrain.ob1k.crud.CrudApplication
import com.outbrain.ob1k.crud.example.Job
import com.outbrain.ob1k.crud.example.Person
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class ModelTest {
    @Test
    internal fun `consistent model json format`() {
        val original = CrudApplication()
                .withEntity(Person::class.java)
                .withEntity(Job::class.java)
                .addReference("job", "person").model
        val model = ObjectMapper().writeValueAsString(original)
        val modelFromFile = "model.json".resource()

        JSONAssert.assertEquals("actual: $model", modelFromFile, model, false)
    }

    private fun String.resource() = ModelTest::class.java.classLoader.getResource(this).readText()

}