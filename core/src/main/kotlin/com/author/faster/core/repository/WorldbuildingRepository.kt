package com.author.faster.core.repository

import com.author.faster.core.model.WorldCategory
import com.author.faster.core.model.WorldEntry
import kotlinx.coroutines.flow.Flow

interface WorldbuildingRepository {
    fun observeCategories(projectId: String): Flow<List<WorldCategory>>

    fun observeEntries(projectId: String): Flow<List<WorldEntry>>

    suspend fun getCategory(projectId: String, categoryId: String): WorldCategory?

    suspend fun getEntry(projectId: String, entryId: String): WorldEntry?

    suspend fun saveCategory(category: WorldCategory)

    suspend fun saveEntry(entry: WorldEntry)

    suspend fun deleteEntry(projectId: String, entryId: String)

    suspend fun ensureBuiltInCategories(projectId: String)
}
