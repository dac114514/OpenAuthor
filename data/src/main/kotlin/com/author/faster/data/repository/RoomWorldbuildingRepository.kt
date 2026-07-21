package com.author.faster.data.repository

import com.author.faster.core.model.BuiltInWorldCategories
import com.author.faster.core.model.WorldCategory
import com.author.faster.core.model.WorldEntry
import com.author.faster.core.repository.WorldbuildingRepository
import com.author.faster.data.local.WorldCategoryEntity
import com.author.faster.data.local.WorldEntryEntity
import com.author.faster.data.local.WorldbuildingDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWorldbuildingRepository(
    private val dao: WorldbuildingDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : WorldbuildingRepository {
    override fun observeCategories(projectId: String): Flow<List<WorldCategory>> =
        dao.observeCategories(projectId).map { categories -> categories.map(WorldCategoryEntity::toDomain) }

    override fun observeEntries(projectId: String): Flow<List<WorldEntry>> =
        dao.observeEntries(projectId).map { entries -> entries.map(WorldEntryEntity::toDomain) }

    override suspend fun getCategory(projectId: String, categoryId: String): WorldCategory? =
        dao.getCategory(projectId, categoryId)?.toDomain()

    override suspend fun getEntry(projectId: String, entryId: String): WorldEntry? =
        dao.getEntry(projectId, entryId)?.toDomain()

    override suspend fun saveCategory(category: WorldCategory) {
        dao.upsertCategory(category.toEntity())
    }

    override suspend fun saveEntry(entry: WorldEntry) {
        require(getCategory(entry.projectId, entry.categoryId) != null) { "世界观分类不存在" }
        dao.upsertEntry(entry.toEntity())
    }

    override suspend fun deleteEntry(projectId: String, entryId: String) {
        dao.deleteEntry(projectId, entryId)
    }

    override suspend fun ensureBuiltInCategories(projectId: String) {
        val existingKeys = dao.getCategories(projectId).map(WorldCategoryEntity::id).toSet()
        val now = clock()
        val missing = BuiltInWorldCategories.all.mapIndexedNotNull { index, template ->
            val stableId = "$projectId:${template.key}"
            if (stableId in existingKeys) return@mapIndexedNotNull null
            WorldCategoryEntity(
                id = stableId,
                projectId = projectId,
                name = template.name,
                description = template.description,
                fieldSchemaJson = template.fieldSchemaJson,
                isBuiltIn = true,
                sortOrder = index,
                createdAt = now,
                updatedAt = now,
            )
        }
        if (missing.isNotEmpty()) dao.upsertCategories(missing)
    }
}

private fun WorldCategoryEntity.toDomain() = WorldCategory(
    id = id,
    projectId = projectId,
    name = name,
    description = description,
    fieldSchemaJson = fieldSchemaJson,
    isBuiltIn = isBuiltIn,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun WorldCategory.toEntity() = WorldCategoryEntity(
    id = id,
    projectId = projectId,
    name = name,
    description = description,
    fieldSchemaJson = fieldSchemaJson,
    isBuiltIn = isBuiltIn,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun WorldEntryEntity.toDomain() = WorldEntry(
    id = id,
    projectId = projectId,
    categoryId = categoryId,
    title = title,
    summary = summary,
    content = content,
    structuredFieldsJson = structuredFieldsJson,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun WorldEntry.toEntity() = WorldEntryEntity(
    id = id,
    projectId = projectId,
    categoryId = categoryId,
    title = title,
    summary = summary,
    content = content,
    structuredFieldsJson = structuredFieldsJson,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
