package com.outbrain.ob1k.crud.dao

import com.outbrain.ob1k.db.ResultSetMapper
import com.outbrain.ob1k.db.TypedRowData

class IntIntMapper : ResultSetMapper<Pair<Int, Int>> {
    override fun map(row: TypedRowData?, columnNames: MutableList<String>?): Pair<Int, Int> {
        return row!!.getInt(0) to row.getInt(1)
    }
}