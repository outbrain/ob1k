package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.crud.model.Entities

interface ICrudAsyncDao<T> {
    /**
     * get multi. with support of
     * pagination - from, up to (inclusive) range. from your endpoint you will get something like "[0,5]"
     * sort - pair of property value. from your endpoint you will get something like "["id":"DESC"]"
     * filter - json of property = predicate value (can be substring). from your endpoint you will get something like "{"name":"ame"}"
     */
    fun list(pagination: IntRange = (0..100),
             sort: Pair<String, String> = "id" to "ASC",
             filter: JsonObject = JsonObject()): ComposableFuture<Entities<T>>

    /**
     * return a single entity or null
     */
    fun read(id: Int): ComposableFuture<T?>

    /**
     * create an entity, return it with an auto generated id
     */
    fun create(entity: T): ComposableFuture<T>

    /**
     * update an entity of the given id
     */
    fun update(id: Int, entity: T): ComposableFuture<T>

    /**
     * delete an entity of the given id, return the id
     */
    fun delete(id: Int): ComposableFuture<Int>

    /**
     * return the name of the resource to crud
     */
    fun resourceName(): String

    /**
     * return the type of the resource
     */
    fun type() : Class<T>
}