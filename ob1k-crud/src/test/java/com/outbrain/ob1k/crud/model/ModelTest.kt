package com.outbrain.ob1k.crud.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.outbrain.ob1k.crud.CrudApplication
import com.outbrain.ob1k.crud.example.Job
import com.outbrain.ob1k.crud.example.Person
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class ModelTest {
    @Test
    internal fun `consistent model json format`() {
        val model = CrudApplication()
                .withEntity(Person::class.java)
                .withEntity(Job::class.java)
                .addReference("job", "person").model

        model("person")!!("name")!!
                .withRangeStyle("Yosi", mapOf("color" to "black"))
                .withRangeStyle("Yosi", mapOf("backgroundColor" to "white"))
        model("person")!!("id")!!
                .withRangeStyle(0, 10, mapOf("color" to "black", "backgroundColor" to "red"))
                .withRangeStyle(10.0, null, mapOf("color" to "blue", "backgroundColor" to "green"))

        val modelJson = ObjectMapper().dontWriteNulls().writeValueAsString(model)
        val modelJsonFromFile = "model.json".resource()

        JSONAssert.assertEquals("actual: $modelJson", modelJsonFromFile, modelJson, false)
    }

    private fun String.resource() = ModelTest::class.java.classLoader.getResource(this).readText()
    private fun ObjectMapper.dontWriteNulls(): ObjectMapper {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        return this
    }

}