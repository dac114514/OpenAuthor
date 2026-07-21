package com.author.faster.agent.runtime

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ToolArgumentValidation(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
)

object ToolArgumentValidator {
    fun validate(arguments: JsonObject, schema: JsonObject): ToolArgumentValidation {
        if (schema.isEmpty()) return ToolArgumentValidation(isValid = true)

        val errors = mutableListOf<String>()
        validateElement(arguments, schema, "$", errors)
        return ToolArgumentValidation(
            isValid = errors.isEmpty(),
            errors = errors,
        )
    }

    private fun validateElement(
        value: JsonElement,
        schema: JsonObject,
        path: String,
        errors: MutableList<String>,
    ) {
        val expectedTypes = schema["type"].asTypes()
        if (value is JsonNull) {
            if ("null" !in expectedTypes) errors += "$path 不能为空"
            return
        }

        if (expectedTypes.isNotEmpty() && expectedTypes.none { value.matchesType(it) }) {
            errors += "$path 类型应为 ${expectedTypes.joinToString("/")}"
            return
        }

        schema["enum"]?.let { enumElement ->
            val allowed = enumElement as? JsonArray
            if (allowed != null && value !in allowed) {
                errors += "$path 必须是允许值之一"
            }
        }

        when (value) {
            is JsonObject -> validateObject(value, schema, path, errors)
            is JsonArray -> validateArray(value, schema, path, errors)
            is JsonPrimitive -> validatePrimitive(value, schema, path, errors)
            JsonNull -> Unit
        }
    }

    private fun validateObject(
        value: JsonObject,
        schema: JsonObject,
        path: String,
        errors: MutableList<String>,
    ) {
        val properties = schema["properties"] as? JsonObject ?: JsonObject(emptyMap())
        val required = (schema["required"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content }
            .orEmpty()

        required.filterNot(value::containsKey).forEach { name ->
            errors += "$path.$name 为必填参数"
        }

        if (schema["additionalProperties"]?.jsonPrimitive?.booleanOrNull == false) {
            value.keys.filterNot(properties::containsKey).forEach { name ->
                errors += "$path.$name 不是允许的参数"
            }
        }

        value.forEach { (name, child) ->
            val childSchema = properties[name] as? JsonObject ?: return@forEach
            validateElement(child, childSchema, "$path.$name", errors)
        }
    }

    private fun validateArray(
        value: JsonArray,
        schema: JsonObject,
        path: String,
        errors: MutableList<String>,
    ) {
        val minItems = schema["minItems"]?.jsonPrimitive?.intOrNull
        val maxItems = schema["maxItems"]?.jsonPrimitive?.intOrNull
        if (minItems != null && value.size < minItems) errors += "$path 至少需要 $minItems 项"
        if (maxItems != null && value.size > maxItems) errors += "$path 最多允许 $maxItems 项"

        val itemSchema = schema["items"] as? JsonObject ?: return
        value.forEachIndexed { index, child ->
            validateElement(child, itemSchema, "$path[$index]", errors)
        }
    }

    private fun validatePrimitive(
        value: JsonPrimitive,
        schema: JsonObject,
        path: String,
        errors: MutableList<String>,
    ) {
        if (value.isString) {
            val minLength = schema["minLength"]?.jsonPrimitive?.intOrNull
            val maxLength = schema["maxLength"]?.jsonPrimitive?.intOrNull
            if (minLength != null && value.content.length < minLength) errors += "$path 长度不能少于 $minLength"
            if (maxLength != null && value.content.length > maxLength) errors += "$path 长度不能超过 $maxLength"
            return
        }

        val number = value.doubleOrNull ?: return
        val minimum = schema["minimum"]?.jsonPrimitive?.doubleOrNull
        val maximum = schema["maximum"]?.jsonPrimitive?.doubleOrNull
        if (minimum != null && number < minimum) errors += "$path 不能小于 $minimum"
        if (maximum != null && number > maximum) errors += "$path 不能大于 $maximum"
    }

    private fun JsonElement?.asTypes(): Set<String> = when (this) {
        is JsonPrimitive -> setOf(content)
        is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.content }.toSet()
        else -> emptySet()
    }

    private fun JsonElement.matchesType(type: String): Boolean = when (type) {
        "object" -> this is JsonObject
        "array" -> this is JsonArray
        "string" -> this is JsonPrimitive && isString
        "boolean" -> this is JsonPrimitive && booleanOrNull != null
        "integer" -> this is JsonPrimitive && intOrNull != null
        "number" -> this is JsonPrimitive && doubleOrNull != null
        "null" -> this is JsonNull
        else -> false
    }
}
