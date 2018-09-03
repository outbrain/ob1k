package com.outbrain.ob1k.crud.model

class EntityFields {
    private val fields = mutableListOf<EntityField>()
    fun with(name: String, type: EFieldType): EntityFields {
        fields += EntityField(dbName = "_$name",
                name = name,
                label = name.capitalize(),
                type = type,
                required = name == "id",
                readOnly = name == "id")
        return this
    }

    fun get() = fields.toList()
}