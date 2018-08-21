package com.outbrain.ob1k.crud.service

import kotlin.test.assertEquals

class EndpointUtilsTest {
    @org.junit.Test
    internal fun `parse range`() {
        val range = EndpointUtils().range("[20,50]")
        assertEquals(20, range.first)
        assertEquals(50, range.last)
    }

    @org.junit.Test
    internal fun `parse sort`() {
        val sortString = EndpointUtils().sort("[\"name\",\"abcd\"]")
        val sortInt = EndpointUtils().sort("[\"num\",5]")
        assertEquals("name", sortString.first)
        assertEquals("abcd", sortString.second)
        assertEquals("num", sortInt.first)
        assertEquals("5", sortInt.second)
    }

    @org.junit.Test
    internal fun `parse json`() {
        val json = EndpointUtils().asJson("{\"name\":\"abcd\",\"num\":5}")
        assertEquals("abcd", json.get("name").asString)
        assertEquals(5, json.get("num").asInt)
    }
}