package com.outbrain.ob1k.crud.model

data class EntityFieldOptions(var style: String? = null,
                              var currency: String? = null,
                              var currencyDisplay: String? = null,
                              var useGrouping: Boolean? = null,
                              var minimumIntegerDigits: String? = null,
                              var minimumFractionDigits: String? = null,
                              var maximumFractionDigits: String? = null,
                              var minimumSignificantDigits: String? = null,
                              var maximumSignificantDigits: String? = null)