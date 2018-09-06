package com.outbrain.ob1k.crud.model

import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize(using = EFieldTypeSerializer::class)
enum class EFieldType {
    BOOLEAN,
    STRING,
    TEXT,
    URL,
    NUMBER,
    DATE,
    REFERENCE,
    REFERENCEMANY,
    SELECT_BY_IDX,
    SELECT_BY_STRING,
    LIST
}