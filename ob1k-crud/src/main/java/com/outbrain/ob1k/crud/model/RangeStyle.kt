package com.outbrain.ob1k.crud.model

data class RangeStyle(val style: Map<String, String>,
                      val value: String? = null,
                      val range: List<Int?>? = null)
