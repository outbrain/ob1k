package com.outbrain.ob1k.crud.model

data class EntityField(var dbName: String,
                       var name: String,
                       var label: String,
                       var type: EFieldType,
                       var required: Boolean = true,
                       var readOnly: Boolean = false,
                       var reference: String? = null)