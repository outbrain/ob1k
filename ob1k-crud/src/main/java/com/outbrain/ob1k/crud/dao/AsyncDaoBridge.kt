package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.outbrain.ob1k.concurrent.ComposableFutures
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService

class AsyncDaoBridge<T>(val dao: ICrudDao<T>, val executor: ExecutorService) : ICrudAsyncDao<T> {

    override fun list(pagination: IntRange, sort: Pair<String, String>, filter: JsonObject) = toAsync { dao.list(pagination, sort, filter) }

    override fun read(id: String) = toAsync { dao.read(id) }

    override fun create(entity: T) = toAsync { dao.create(entity) }

    override fun update(id: String, entity: T) = toAsync { dao.update(id, entity) }

    override fun delete(id: String) = toAsync { dao.delete(id) }

    override fun resourceName() = dao.resourceName()

    override fun type() = dao.type()

    private fun <T> toAsync(func: () -> T) = ComposableFutures.submit(executor, Callable { func.invoke() })
}