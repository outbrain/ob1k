package com.outbrain.ob1k.crud.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class EntityField(@JsonIgnore var dbName: String,
                       var name: String,
                       var label: String,
                       var type: EFieldType,
                       var required: Boolean = true,
                       var readOnly: Boolean = false,
                       @JsonIgnore var autoGenerate: Boolean = false,
                       var reference: String? = null,
                       var target: String? = null,
                       var display: EntityFieldDisplay? = null,
                       var hidden: Boolean = false,
                       var options: EntityFieldOptions? = null) {

    fun nonNullOptions(): EntityFieldOptions {
        if (options == null) {
            options = EntityFieldOptions()
        }
        return options!!
    }
}



