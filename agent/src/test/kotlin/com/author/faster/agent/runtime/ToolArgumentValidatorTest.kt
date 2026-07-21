package com.author.faster.agent.runtime

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolArgumentValidatorTest {
    private val schema = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        put("required", buildJsonArray { add(JsonPrimitive("name")) })
        put(
            "properties",
            buildJsonObject {
                put(
                    "name",
                    buildJsonObject {
                        put("type", "string")
                        put("minLength", 2)
                    },
                )
                put(
                    "age",
                    buildJsonObject {
                        put("type", "integer")
                        put("minimum", 0)
                    },
                )
            },
        )
    }

    @Test
    fun acceptsArgumentsMatchingSchema() {
        val arguments = buildJsonObject {
            put("name", "林默")
            put("age", 24)
        }

        assertTrue(ToolArgumentValidator.validate(arguments, schema).isValid)
    }

    @Test
    fun rejectsMissingUnknownAndWrongTypeArguments() {
        val arguments = buildJsonObject {
            put("age", "unknown")
            put("shell", "rm -rf")
        }

        val result = ToolArgumentValidator.validate(arguments, schema)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { "name" in it })
        assertTrue(result.errors.any { "shell" in it })
        assertTrue(result.errors.any { "integer" in it })
    }
}
