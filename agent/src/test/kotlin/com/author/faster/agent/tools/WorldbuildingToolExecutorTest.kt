package com.author.faster.agent.tools

import com.author.faster.agent.runtime.ToolArgumentValidator
import com.author.faster.agent.runtime.ToolCall
import com.author.faster.agent.runtime.ToolRiskLevel
import com.author.faster.core.model.WorldCategory
import com.author.faster.core.model.WorldEntry
import com.author.faster.core.repository.WorldbuildingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldbuildingToolExecutorTest {
    @Test
    fun everyWorldbuildingToolHasAnObjectSchema() {
        assertEquals(8, WorldbuildingTools.definitions.size)
        assertEquals(8, WorldbuildingTools.definitions.map { it.name }.distinct().size)
        WorldbuildingTools.definitions.forEach { definition ->
            assertEquals("object", definition.inputSchema["type"]?.toString()?.trim('"'))
            assertTrue(ToolArgumentValidator.validate(buildJsonObject {}, definition.inputSchema).errors.all {
                "必填" in it
            })
        }
    }

    @Test
    fun createsUpdatesAndDeletesWorldEntry() = runBlocking {
        val repository = FakeWorldbuildingRepository()
        repository.saveCategory(category("foundation", builtIn = true))
        val executor = WorldbuildingToolExecutor(
            projectId = PROJECT_ID,
            repository = repository,
            clock = { 200L },
            newId = { "entry-1" },
        )

        val created = executor.execute(
            call(
                WorldbuildingTools.CREATE_ENTRY,
                ToolRiskLevel.CREATE,
                buildJsonObject {
                    put("category_id", "foundation")
                    put("title", "潮汐历")
                    put("summary", "航行历法")
                    put("content", "星潮决定远航窗口")
                },
            ),
        )
        assertFalse(created.isError)
        assertEquals("潮汐历", repository.entries.value.single().title)

        val updated = executor.execute(
            call(
                WorldbuildingTools.UPDATE_ENTRY,
                ToolRiskLevel.UPDATE,
                buildJsonObject {
                    put("entry_id", "entry-1")
                    put("content", "每九十七天开放一次远航窗口")
                },
            ),
        )
        assertFalse(updated.isError)
        assertEquals("每九十七天开放一次远航窗口", repository.entries.value.single().content)

        val deleted = executor.execute(
            call(
                WorldbuildingTools.DELETE_ENTRY,
                ToolRiskLevel.DELETE,
                buildJsonObject { put("entry_id", "entry-1") },
            ),
        )
        assertFalse(deleted.isError)
        assertTrue(repository.entries.value.isEmpty())
    }

    @Test
    fun builtInCategoryNameCannotBeChanged() = runBlocking {
        val repository = FakeWorldbuildingRepository()
        repository.saveCategory(category("foundation", builtIn = true))
        val executor = WorldbuildingToolExecutor(PROJECT_ID, repository, clock = { 300L })

        val result = executor.execute(
            call(
                WorldbuildingTools.UPDATE_CATEGORY,
                ToolRiskLevel.UPDATE,
                buildJsonObject {
                    put("category_id", "foundation")
                    put("name", "被篡改的名称")
                    put("description", "新的说明")
                },
            ),
        )

        assertFalse(result.isError)
        assertEquals("世界基础", repository.categories.value.single().name)
        assertEquals("新的说明", repository.categories.value.single().description)
    }

    private fun category(id: String, builtIn: Boolean) = WorldCategory(
        id = id,
        projectId = PROJECT_ID,
        name = "世界基础",
        description = "基础规则",
        fieldSchemaJson = "{}",
        isBuiltIn = builtIn,
        sortOrder = 0,
        createdAt = 100L,
        updatedAt = 100L,
    )

    private fun call(name: String, risk: ToolRiskLevel, arguments: kotlinx.serialization.json.JsonObject) = ToolCall(
        id = "call-$name",
        name = name,
        arguments = arguments,
        riskLevel = risk,
    )

    private class FakeWorldbuildingRepository : WorldbuildingRepository {
        val categories = MutableStateFlow<List<WorldCategory>>(emptyList())
        val entries = MutableStateFlow<List<WorldEntry>>(emptyList())

        override fun observeCategories(projectId: String): Flow<List<WorldCategory>> = categories

        override fun observeEntries(projectId: String): Flow<List<WorldEntry>> = entries

        override suspend fun getCategory(projectId: String, categoryId: String): WorldCategory? =
            categories.value.firstOrNull { it.projectId == projectId && it.id == categoryId }

        override suspend fun getEntry(projectId: String, entryId: String): WorldEntry? =
            entries.value.firstOrNull { it.projectId == projectId && it.id == entryId }

        override suspend fun saveCategory(category: WorldCategory) {
            categories.value = categories.value.filterNot { it.id == category.id } + category
        }

        override suspend fun saveEntry(entry: WorldEntry) {
            entries.value = entries.value.filterNot { it.id == entry.id } + entry
        }

        override suspend fun deleteEntry(projectId: String, entryId: String) {
            entries.value = entries.value.filterNot { it.projectId == projectId && it.id == entryId }
        }

        override suspend fun ensureBuiltInCategories(projectId: String) = Unit
    }

    private companion object {
        const val PROJECT_ID = "project-1"
    }
}
