package com.outbrain.ob1k.crud.dao

import com.outbrain.ob1k.crud.model.EntityDescription
import com.outbrain.ob1k.crud.model.EntityField

data class MySQLRefMeta(val fromDesc: EntityDescription,
                        val fromField : EntityField,
                        val toDesc: EntityDescription,
                        val toField: EntityField)