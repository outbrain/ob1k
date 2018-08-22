package com.outbrain.ob1k.crud.model

data class EntityDescription(val table: String,
                             val id: Int,
                             val resourceName: String = table.substringAfterLast("_"),
                             var title: String = resourceName.capitalize(),
                             val endpoint: String = "../${resourceName}s",
                             var fields: List<EntityField> = emptyList()) {
    val references = mutableListOf<EntityDescription>()

    operator fun invoke(name: String): EntityField? = fields.find { name == it.name }
    fun getByColumn(dbName: String): EntityField? = fields.find { dbName == it.dbName }

    fun editableFields() = fields.filter { !it.readOnly }

    fun idDBFieldName() = idField().dbName

    fun idFieldName() = idField().name

    fun idField() = this("id")!!

    fun referenceTo(resource: String) = fields.find { (it.type == EFieldType.REFERENCE || it.type == EFieldType.REFERENCEMANY) && it.reference == resource }
}