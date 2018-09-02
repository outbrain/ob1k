package com.outbrain.ob1k.crud.dao

import com.outbrain.ob1k.crud.model.Entities

/**
 * dao interface. to better understand if your implementation is well defined, check the following example test
 *
 * https://github.com/outbrain/ob1k/blob/master/ob1k-crud/src/test/java/com/outbrain/ob1k/crud/dao/CrudDaoTestBase.kt
 */
interface ICrudDao<T> {
    /**
     * get multi. with support of
     * pagination - from, up to (inclusive) range. from your endpoint you will get something like "[0,5]"
     * sort - pair of property value. from your endpoint you will get something like "["id":"DESC"]"
     * filter - when fields are null you should ignore. otherwise you should filter by them
     */
    fun list(pagination: IntRange = (0..100),
             sort: Pair<String, String> = "id" to "ASC",
             filter: T? = null): Entities<T>

    /**
     * get multi by ids
     */
    fun list(ids: List<String>): Entities<T>

    /**
     * return a single entity or null
     */
    fun read(id: String): T?

    /**
     * create an entity, return it with an auto generated id
     */
    fun create(entity: T): T

    /**
     * update an entity of the given id
     */
    fun update(id: String, entity: T): T

    /**
     * delete an entity of the given id, return the id
     */
    fun delete(id: String): Int

    /**
     * return the name of the resource to crud
     */
    fun resourceName(): String

    /**
     * return the type of the resource
     */
    fun type(): Class<T>
}