package com.author.faster.data.repository

import com.author.faster.core.model.BuiltInWorldCategories
import com.author.faster.data.local.WorldCategoryEntity
import com.author.faster.data.local.WorldEntryEntity
import com.author.faster.data.local.WorldbuildingDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomWorldbuildingRepositoryTest {
    @Test
    fun builtInCategoriesAreCreatedIdempotently() = runBlocking {
        val dao = FakeWorldbuildingDao()
        val repository = RoomWorldbuildingRepository(dao, clock = { 123L })

        repository.ensureBuiltInCategories("project-1")
        repository.ensureBuiltInCategories("project-1")

        assertEquals(BuiltInWorldCategories.all.size, dao.categories.value.size)
        assertTrue(dao.categories.value.all { it.isBuiltIn })
        assertEquals(
            BuiltInWorldCategories.all.map { it.name },
            dao.categories.value.sortedBy { it.sortOrder }.map { it.name },
        )
    }

    private class FakeWorldbuildingDao : WorldbuildingDao {
        val categories = MutableStateFlow<List<WorldCategoryEntity>>(emptyList())
        private val entries = MutableStateFlow<List<WorldEntryEntity>>(emptyList())

        override fun observeCategories(projectId: String): Flow<List<WorldCategoryEntity>> = categories

        override fun observeEntries(projectId: String): Flow<List<WorldEntryEntity>> = entries

        override suspend fun getCategory(projectId: String, categoryId: String): WorldCategoryEntity? =
            categories.value.firstOrNull { it.projectId == projectId && it.id == categoryId }

        override suspend fun getCategories(projectId: String): List<WorldCategoryEntity> =
            categories.value.filter { it.projectId == projectId }

        override suspend fun getEntry(projectId: String, entryId: String): WorldEntryEntity? =
            entries.value.firstOrNull { it.projectId == projectId && it.id == entryId }

        override suspend fun upsertCategory(category: WorldCategoryEntity) {
            categories.value = categories.value.filterNot { it.id == category.id } + category
        }

        override suspend fun upsertCategories(categories: List<WorldCategoryEntity>) {
            categories.forEach { upsertCategory(it) }
        }

        override suspend fun upsertEntry(entry: WorldEntryEntity) {
            entries.value = entries.value.filterNot { it.id == entry.id } + entry
        }

        override suspend fun deleteEntry(projectId: String, entryId: String) {
            entries.value = entries.value.filterNot { it.projectId == projectId && it.id == entryId }
        }
    }
}
