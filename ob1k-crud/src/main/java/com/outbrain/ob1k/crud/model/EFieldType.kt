package com.outbrain.ob1k.crud.model

import com.google.gson.JsonObject

enum class EFieldType {
    BOOLEAN {
        override fun fillJsonObject(obj: JsonObject, property: String, value: String?) = obj.addProperty(property, value == "1")
        override fun toMysqlMatchValue(value: String) = if (value == "true") "=1" else "=0"
        override fun toMysqlValue(value: String) = if (value == "true") "1" else "0"
    },
    STRING {
        override fun toMysqlMatchValue(value: String) = " LIKE \"%$value%\""
        override fun toMysqlValue(value: String) = "\"$value\""
    },
    TEXT {
        override fun toMysqlMatchValue(value: String) = " LIKE \"%$value%\""
        override fun toMysqlValue(value: String) = "\"$value\""
    },
    NUMBER {
        override fun fillJsonObject(obj: JsonObject, property: String, value: String?) {
            if (value == null) {
                return
            }
            try {
                obj.addProperty(property, value.toInt())
            } catch (e: NumberFormatException) {
                try {
                    obj.addProperty(property, value.toDouble())
                } catch (e: NumberFormatException) {
                    throw RuntimeException(e.message, e)
                }
            }
        }
    },
    DATE,
    REFERENCE,
    LIST;

    open fun toMysqlMatchValue(value: String) = "=$value"
    open fun toMysqlValue(value: String) = value
    open fun fillJsonObject(obj: JsonObject, property: String, value: String?) = obj.addProperty(property, value)
    override fun toString() = name.toLowerCase().capitalize()
}