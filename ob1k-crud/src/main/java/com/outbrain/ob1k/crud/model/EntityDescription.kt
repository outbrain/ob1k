package com.outbrain.ob1k.crud.model

data class EntityDescription(val table: String,
                             val id: Int,
                             val resourceName: String = table.substringAfterLast("_"),
                             var title: String = resourceName.capitalize(),
                             val endpoint: String = "../${resourceName}s",
                             val fields: List<EntityField> = emptyList()) {

    operator fun invoke(name: String): EntityField? = fields.find { name == it.name }

    fun editableFields() = fields.filter { !it.readOnly }

    fun idFieldName() = this("id")!!.dbName
}