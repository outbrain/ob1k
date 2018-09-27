package com.outbrain.ob1k.crud.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.gson.JsonObject

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
                       var options: EntityFieldOptions? = null,
                       var choices: List<String>? = null,
                       var rangeStyles: MutableList<RangeStyle>? = null) {

    fun withRangeStyle(value: String?, style: Map<String, String>) = withRangeStyle(RangeStyle(style = style, value = value))

    fun withRangeStyle(start: Int?, end: Int?, style: Map<String, String>) = withRangeStyle(RangeStyle(style = style, range = listOf(start, end)))

    private fun withRangeStyle(rangeStyle: RangeStyle): EntityField {
        if (rangeStyles == null) {
            rangeStyles = mutableListOf()
        }
        rangeStyles?.let { it += rangeStyle }
        return this
    }


    fun nonNullOptions(): EntityFieldOptions {
        if (options == null) {
            options = EntityFieldOptions()
        }
        return options!!
    }

    fun setChoices(cls: Class<*>) {
        choices = cls.enumChoices()
    }


    internal fun toMysqlMatchValue(value: String): String {
        return when (type) {
            EFieldType.BOOLEAN -> if (value == "true") "=1" else "=0"
            EFieldType.STRING, EFieldType.URL, EFieldType.TEXT, EFieldType.SELECT_BY_STRING -> " LIKE \"%$value%\""
            EFieldType.NUMBER, EFieldType.REFERENCE -> {
                val cleaned = value.removePrefix("[").removeSuffix("]").replace("\"", "")
                val split = cleaned.split(",")
                return if (split.size == 1) "=$cleaned" else " IN ($cleaned)"
            }
            EFieldType.DATE -> "=\"$value\""
            EFieldType.SELECT_BY_IDX -> "=${choices!!.indexOf(value)}"
            else -> "=$value"
        }
    }

    internal fun toMysqlValue(value: String): String {
        return when (type) {
            EFieldType.BOOLEAN -> if (value == "true") "1" else "0"
            EFieldType.STRING, EFieldType.DATE, EFieldType.URL, EFieldType.TEXT, EFieldType.SELECT_BY_STRING -> "\"$value\""
            EFieldType.SELECT_BY_IDX -> "${choices!!.indexOf(value)}"
            else -> value
        }
    }


    internal fun fillJsonObject(obj: JsonObject, property: String, value: String?) {
        when (type) {
            EFieldType.BOOLEAN -> obj.addProperty(property, value == "1")
            EFieldType.NUMBER, EFieldType.REFERENCE -> {
                value?.let {
                    try {
                        obj.addProperty(property, it.toInt())
                    } catch (e: NumberFormatException) {
                        try {
                            obj.addProperty(property, it.toDouble())
                        } catch (e: NumberFormatException) {
                            throw RuntimeException(e.message, e)
                        }
                    }
                }
            }
            EFieldType.REFERENCEMANY -> throw UnsupportedOperationException()
            EFieldType.SELECT_BY_IDX -> value?.let { obj.addProperty(property, choices!![it.toInt()]) }
            else -> obj.addProperty(property, value)
        }
    }

    private fun Class<*>.enumChoices() = enumConstants?.map { it.toString() }
}



