package com.author.faster.agent.tools

import com.author.faster.agent.runtime.ToolCall
import com.author.faster.agent.runtime.ToolDefinition
import com.author.faster.agent.runtime.ToolExecutionResult
import com.author.faster.agent.runtime.ToolExecutor
import com.author.faster.agent.runtime.ToolRiskLevel
import com.author.faster.core.model.WorldCategory
import com.author.faster.core.model.WorldEntry
import com.author.faster.core.repository.WorldbuildingRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

object WorldbuildingTools {
    const val LIST_CATEGORIES = "list_world_categories"
    const val LIST_ENTRIES = "list_world_entries"
    const val GET_ENTRY = "get_world_entry"
    const val CREATE_ENTRY = "create_world_entry"
    const val UPDATE_ENTRY = "update_world_entry"
    const val DELETE_ENTRY = "delete_world_entry"
    const val CREATE_CATEGORY = "create_world_category"
    const val UPDATE_CATEGORY = "update_world_category"

    val definitions = listOf(
        ToolDefinition(
            LIST_CATEGORIES,
            "列出当前项目的世界观分类",
            objectSchema(),
            ToolRiskLevel.READ,
        ),
        ToolDefinition(
            LIST_ENTRIES,
            "列出世界观条目，可按分类筛选",
            objectSchema(properties = mapOf("category_id" to stringSchema("分类 ID"))),
            ToolRiskLevel.READ,
        ),
        ToolDefinition(
            GET_ENTRY,
            "读取一个世界观条目的完整内容",
            objectSchema(
                required = listOf("entry_id"),
                properties = mapOf("entry_id" to stringSchema("条目 ID")),
            ),
            ToolRiskLevel.READ,
        ),
        ToolDefinition(
            CREATE_ENTRY,
            "在指定分类中创建世界观条目",
            objectSchema(
                required = listOf("category_id", "title", "content"),
                properties = entryProperties(includeId = false),
            ),
            ToolRiskLevel.CREATE,
        ),
        ToolDefinition(
            UPDATE_ENTRY,
            "更新已有世界观条目；只提交需要修改的字段",
            objectSchema(
                required = listOf("entry_id"),
                properties = entryProperties(includeId = true),
            ),
            ToolRiskLevel.UPDATE,
        ),
        ToolDefinition(
            DELETE_ENTRY,
            "删除指定世界观条目",
            objectSchema(
                required = listOf("entry_id"),
                properties = mapOf("entry_id" to stringSchema("条目 ID")),
            ),
            ToolRiskLevel.DELETE,
        ),
        ToolDefinition(
            CREATE_CATEGORY,
            "创建自定义世界观分类",
            objectSchema(
                required = listOf("name", "description"),
                properties = categoryProperties(includeId = false),
            ),
            ToolRiskLevel.CREATE,
        ),
        ToolDefinition(
            UPDATE_CATEGORY,
            "更新世界观分类说明或字段 Schema；内置分类名称不可修改",
            objectSchema(
                required = listOf("category_id"),
                properties = categoryProperties(includeId = true),
            ),
            ToolRiskLevel.UPDATE,
        ),
    )
}

class WorldbuildingToolExecutor(
    private val projectId: String,
    private val repository: WorldbuildingRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ToolExecutor {
    override suspend fun execute(call: ToolCall): ToolExecutionResult = try {
        when (call.name) {
            WorldbuildingTools.LIST_CATEGORIES -> listCategories()
            WorldbuildingTools.LIST_ENTRIES -> listEntries(call.arguments.optionalString("category_id"))
            WorldbuildingTools.GET_ENTRY -> getEntry(call.arguments.requiredString("entry_id"))
            WorldbuildingTools.CREATE_ENTRY -> createEntry(call.arguments)
            WorldbuildingTools.UPDATE_ENTRY -> updateEntry(call.arguments)
            WorldbuildingTools.DELETE_ENTRY -> deleteEntry(call.arguments.requiredString("entry_id"))
            WorldbuildingTools.CREATE_CATEGORY -> createCategory(call.arguments)
            WorldbuildingTools.UPDATE_CATEGORY -> updateCategory(call.arguments)
            else -> ToolExecutionResult("不支持的世界观工具：${call.name}", isError = true)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        ToolExecutionResult(
            content = "世界观操作失败：${error.message ?: "请检查参数后重试"}",
            isError = true,
        )
    }

    private suspend fun listCategories(): ToolExecutionResult {
        val categories = repository.observeCategories(projectId).first()
        return ToolExecutionResult(
            buildJsonArray { categories.forEach { add(it.toJson()) } }.toString(),
        )
    }

    private suspend fun listEntries(categoryId: String?): ToolExecutionResult {
        val entries = repository.observeEntries(projectId).first()
            .filter { categoryId == null || it.categoryId == categoryId }
        return ToolExecutionResult(
            buildJsonArray { entries.forEach { add(it.toSummaryJson()) } }.toString(),
        )
    }

    private suspend fun getEntry(entryId: String): ToolExecutionResult {
        val entry = repository.getEntry(projectId, entryId) ?: error("世界观条目不存在")
        return ToolExecutionResult(entry.toJson().toString())
    }

    private suspend fun createEntry(arguments: JsonObject): ToolExecutionResult {
        val categoryId = arguments.requiredString("category_id")
        require(repository.getCategory(projectId, categoryId) != null) { "世界观分类不存在" }
        val now = clock()
        val entry = WorldEntry(
            id = newId(),
            projectId = projectId,
            categoryId = categoryId,
            title = arguments.requiredString("title").trim(),
            summary = arguments.optionalString("summary").orEmpty().trim(),
            content = arguments.requiredString("content").trim(),
            structuredFieldsJson = arguments.optionalObject("structured_fields")?.toString() ?: "{}",
            createdAt = now,
            updatedAt = now,
        )
        require(entry.title.isNotBlank()) { "条目标题不能为空" }
        require(entry.content.isNotBlank()) { "条目内容不能为空" }
        repository.saveEntry(entry)
        return ToolExecutionResult(entry.toJson().toString())
    }

    private suspend fun updateEntry(arguments: JsonObject): ToolExecutionResult {
        val entryId = arguments.requiredString("entry_id")
        val existing = repository.getEntry(projectId, entryId) ?: error("世界观条目不存在")
        val categoryId = arguments.optionalString("category_id") ?: existing.categoryId
        require(repository.getCategory(projectId, categoryId) != null) { "世界观分类不存在" }
        val updated = existing.copy(
            categoryId = categoryId,
            title = arguments.optionalString("title")?.trim() ?: existing.title,
            summary = arguments.optionalString("summary")?.trim() ?: existing.summary,
            content = arguments.optionalString("content")?.trim() ?: existing.content,
            structuredFieldsJson = arguments.optionalObject("structured_fields")?.toString()
                ?: existing.structuredFieldsJson,
            updatedAt = clock(),
        )
        require(updated.title.isNotBlank()) { "条目标题不能为空" }
        require(updated.content.isNotBlank()) { "条目内容不能为空" }
        repository.saveEntry(updated)
        return ToolExecutionResult(updated.toJson().toString())
    }

    private suspend fun deleteEntry(entryId: String): ToolExecutionResult {
        require(repository.getEntry(projectId, entryId) != null) { "世界观条目不存在" }
        repository.deleteEntry(projectId, entryId)
        return ToolExecutionResult(buildJsonObject { put("deleted_entry_id", entryId) }.toString())
    }

    private suspend fun createCategory(arguments: JsonObject): ToolExecutionResult {
        val categories = repository.observeCategories(projectId).first()
        val name = arguments.requiredString("name").trim()
        require(name.isNotBlank()) { "分类名称不能为空" }
        require(categories.none { it.name == name }) { "同名世界观分类已存在" }
        val now = clock()
        val category = WorldCategory(
            id = newId(),
            projectId = projectId,
            name = name,
            description = arguments.requiredString("description").trim(),
            fieldSchemaJson = arguments.optionalObject("field_schema")?.toString() ?: "{}",
            isBuiltIn = false,
            sortOrder = (categories.maxOfOrNull(WorldCategory::sortOrder) ?: -1) + 1,
            createdAt = now,
            updatedAt = now,
        )
        repository.saveCategory(category)
        return ToolExecutionResult(category.toJson().toString())
    }

    private suspend fun updateCategory(arguments: JsonObject): ToolExecutionResult {
        val categoryId = arguments.requiredString("category_id")
        val existing = repository.getCategory(projectId, categoryId) ?: error("世界观分类不存在")
        val updated = existing.copy(
            name = if (existing.isBuiltIn) existing.name
            else arguments.optionalString("name")?.trim() ?: existing.name,
            description = arguments.optionalString("description")?.trim() ?: existing.description,
            fieldSchemaJson = arguments.optionalObject("field_schema")?.toString() ?: existing.fieldSchemaJson,
            updatedAt = clock(),
        )
        require(updated.name.isNotBlank()) { "分类名称不能为空" }
        repository.saveCategory(updated)
        return ToolExecutionResult(updated.toJson().toString())
    }
}

private fun objectSchema(
    required: List<String> = emptyList(),
    properties: Map<String, JsonObject> = emptyMap(),
): JsonObject = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    put("properties", JsonObject(properties))
    if (required.isNotEmpty()) {
        put("required", JsonArray(required.map(::JsonPrimitive)))
    }
}

private fun stringSchema(description: String): JsonObject = buildJsonObject {
    put("type", "string")
    put("description", description)
    put("minLength", 1)
}

private fun freeStringSchema(description: String): JsonObject = buildJsonObject {
    put("type", "string")
    put("description", description)
}

private fun jsonObjectSchema(description: String): JsonObject = buildJsonObject {
    put("type", "object")
    put("description", description)
}

private fun entryProperties(includeId: Boolean): Map<String, JsonObject> = buildMap {
    if (includeId) put("entry_id", stringSchema("条目 ID"))
    put("category_id", stringSchema("分类 ID"))
    put("title", stringSchema("条目标题"))
    put("summary", freeStringSchema("简短摘要"))
    put("content", stringSchema("完整设定内容"))
    put("structured_fields", jsonObjectSchema("符合分类字段 Schema 的结构化值"))
}

private fun categoryProperties(includeId: Boolean): Map<String, JsonObject> = buildMap {
    if (includeId) put("category_id", stringSchema("分类 ID"))
    put("name", stringSchema("分类名称"))
    put("description", freeStringSchema("分类用途说明"))
    put("field_schema", jsonObjectSchema("分类的 JSON Schema 字段定义"))
}

private fun JsonObject.requiredString(name: String): String =
    optionalString(name) ?: error("缺少参数：$name")

private fun JsonObject.optionalString(name: String): String? =
    (get(name) as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content

private fun JsonObject.optionalObject(name: String): JsonObject? =
    get(name)?.let { it as? JsonObject ?: it.jsonObject }

private fun WorldCategory.toJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("name", name)
    put("description", description)
    put("field_schema", fieldSchemaJson.toJsonObject())
    put("is_built_in", isBuiltIn)
    put("sort_order", sortOrder)
}

private fun WorldEntry.toSummaryJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("category_id", categoryId)
    put("title", title)
    put("summary", summary)
    put("updated_at", updatedAt)
}

private fun WorldEntry.toJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("category_id", categoryId)
    put("title", title)
    put("summary", summary)
    put("content", content)
    put("structured_fields", structuredFieldsJson.toJsonObject())
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun String.toJsonObject(): JsonObject =
    runCatching { Json.parseToJsonElement(this).jsonObject }.getOrDefault(JsonObject(emptyMap()))
