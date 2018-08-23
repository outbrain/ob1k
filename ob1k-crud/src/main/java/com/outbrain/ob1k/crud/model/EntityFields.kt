package com.outbrain.ob1k.crud.model

class EntityFields {
    private val fields = mutableListOf<EntityField>()
    fun with(name: String, type: EFieldType): EntityFields {
        fields += EntityField("_$name", name, name.capitalize(), type, name == "id", name == "id")
        return this
    }

    fun get() = fields.toList()
}