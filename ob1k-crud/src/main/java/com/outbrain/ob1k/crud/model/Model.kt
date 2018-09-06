package com.outbrain.ob1k.crud.model

class Model(total: Int = 0, data: List<EntityDescription> = emptyList()) : Entities<EntityDescription>(total, data) {
    fun <T> withEntity(type: Class<T>) = withEntity(type.toDescription(total))
    fun withEntity(it: EntityDescription) = Model(total + 1, data + it)
    operator fun invoke(resourceName: String) = data.find { resourceName == it.resourceName }
    operator fun invoke(resourceName: String, fieldName: String) = this(resourceName)?.invoke(fieldName)
    fun getByTable(table: String) = data.find { table == it.table }
}