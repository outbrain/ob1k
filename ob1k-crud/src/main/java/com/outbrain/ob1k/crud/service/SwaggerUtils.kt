package com.outbrain.ob1k.crud.service

import com.outbrain.ob1k.crud.model.EFieldType
import com.outbrain.ob1k.crud.model.EntityDescription
import com.outbrain.ob1k.crud.model.EntityField
import io.swagger.models.*
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.parameters.PathParameter
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.properties.*
import io.swagger.models.utils.PropertyModelConverter

private val className = CrudDispatcher::class.java.name
private val classSimpleName = CrudDispatcher::class.java.simpleName

fun com.outbrain.ob1k.crud.model.Model.registerToSwagger(swagger: Swagger, key: String) {
    val relativePath = key.substringBefore("{resource}")
    swagger.tag(Tag().name(classSimpleName).description(className))

    data.forEach {
        val list = operation(it.resourceName, "list")
                .withRangeQueryParam()
                .withSortQueryParam()
                .withFilterQueryParam(it)
                .returningListOf(it)
        val create = operation(it.resourceName, "create").withBodyParam(it).returning(it)
        val get = operation(it.resourceName, "get").withIdPathParam().returning(it)
        val update = operation(it.resourceName, "update").withIdPathParam().withBodyParam(it).returning(it)
        val delete = operation(it.resourceName, "delete").withIdPathParam().returning200()

        val resourcePath = "$relativePath${it.resourceName}"
        if (it.editable) {
            swagger.path(resourcePath, Path().get(list).post(create))
            swagger.path("$resourcePath/{id}", Path().get(get).put(update).delete(delete))
        } else {
            swagger.path(resourcePath, Path().get(list))
            swagger.path("$resourcePath/{id}", Path().get(get))
        }
    }

}

private fun EntityDescription.asProperty() = fields
        .filter { it.type != EFieldType.REFERENCEMANY }
        .fold(ObjectProperty(), { obj, entity -> obj.property(entity.name, entity.asProperty()) })


private fun Property.toSchema() = PropertyModelConverter().propertyToModel(this)

private fun EntityDescription.asResponse() = Response().responseSchema(asProperty().toSchema())

private fun EntityDescription.asListResponse() = Response().responseSchema(
        ObjectProperty()
                .property("total", IntegerProperty())
                .property("data", ArrayProperty().items(asProperty()))
                .toSchema())

private fun EntityDescription.asBodyParameter() = BodyParameter().name(resourceName).schema(asProperty().toSchema())


private fun EntityField.asProperty(): Property {
    return when (type) {
        EFieldType.NUMBER -> DecimalProperty()
        EFieldType.DATE -> DateProperty()
        EFieldType.BOOLEAN -> BooleanProperty()
        EFieldType.SELECT_BY_IDX, EFieldType.SELECT_BY_STRING -> {
            val stringProperty = StringProperty()
            stringProperty._enum(choices)
            stringProperty
        }
        else -> StringProperty()
    }
}

private fun Operation.withRangeQueryParam() = parameter(QueryParameter()
        .name("range")
        .type("array")
        .example("[0,9]")
        .minmax(2)
        .description("[from,to-inclusive]")
        .items(IntegerProperty())
        .required(false))

private fun Operation.withFilterQueryParam(desc: EntityDescription) = parameter(QueryParameter()
        .name("filter")
        .type("string")
        .description("filter by example of ${desc.resourceName}")
        .example("{\"id\",\"abc\"}")

        .required(false))

private fun Operation.withSortQueryParam() = parameter(QueryParameter()
        .name("sort")
        .type("array")
        .description("[sort-by,ASC|DESC]")
        .example("[\"id\",\"ASC\"]")
        .items(StringProperty())
        .minmax(2)
        .required(false))


private fun QueryParameter.minmax(n: Int): QueryParameter {
    minItems = n
    maxItems = n
    return this

}

private fun Operation.withIdPathParam() = parameter(PathParameter().name("id").type("string"))

private fun Operation.withBodyParam(desc: EntityDescription) = parameter(desc.asBodyParameter())

private fun Operation.returning200() = responseChain(Response())

private fun Operation.returning(desc: EntityDescription) = responseChain(desc.asResponse())

private fun Operation.returningListOf(desc: EntityDescription) = responseChain(desc.asListResponse()
        .header("content-range", StringProperty()
                .example("0-9/150")
                .description("from-to/total")))

private fun Operation.responseChain(response: Response) = response(200, response)
        .response(401, Response().description("Unauthorized"))
        .response(403, Response().description("Forbidden"))
        .response(404, Response().description("Not Found"))

private fun operation(resource: String, type: String) =
        Operation().summary("$className.$type-$resource").tag(classSimpleName).operationId("$className.$type-$resource")
