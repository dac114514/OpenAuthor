package com.author.faster.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldbuildingDao {
    @Query("SELECT * FROM world_categories WHERE projectId = :projectId ORDER BY sortOrder, name")
    fun observeCategories(projectId: String): Flow<List<WorldCategoryEntity>>

    @Query("SELECT * FROM world_entries WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeEntries(projectId: String): Flow<List<WorldEntryEntity>>

    @Query("SELECT * FROM world_categories WHERE projectId = :projectId AND id = :categoryId LIMIT 1")
    suspend fun getCategory(projectId: String, categoryId: String): WorldCategoryEntity?

    @Query("SELECT * FROM world_categories WHERE projectId = :projectId ORDER BY sortOrder, name")
    suspend fun getCategories(projectId: String): List<WorldCategoryEntity>

    @Query("SELECT * FROM world_entries WHERE projectId = :projectId AND id = :entryId LIMIT 1")
    suspend fun getEntry(projectId: String, entryId: String): WorldEntryEntity?

    @Upsert
    suspend fun upsertCategory(category: WorldCategoryEntity)

    @Upsert
    suspend fun upsertCategories(categories: List<WorldCategoryEntity>)

    @Upsert
    suspend fun upsertEntry(entry: WorldEntryEntity)

    @Query("DELETE FROM world_entries WHERE projectId = :projectId AND id = :entryId")
    suspend fun deleteEntry(projectId: String, entryId: String)
}
