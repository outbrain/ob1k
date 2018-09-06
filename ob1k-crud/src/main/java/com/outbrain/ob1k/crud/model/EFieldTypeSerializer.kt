package com.outbrain.ob1k.crud.model

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class EFieldTypeSerializer : StdSerializer<EFieldType>(EFieldType::class.java) {
    override fun serialize(value: EFieldType?, jgen: JsonGenerator?, provider: SerializerProvider?) {
        value?.let { jgen!!.writeString(it.name.substringBefore("_")) }
    }
}