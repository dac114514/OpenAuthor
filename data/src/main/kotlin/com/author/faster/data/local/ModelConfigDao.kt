package com.author.faster.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelConfigDao {
    @Query("SELECT * FROM model_configs ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ModelConfigEntity>>

    @Query("SELECT * FROM model_configs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ModelConfigEntity?

    @Upsert
    suspend fun upsert(config: ModelConfigEntity)

    @Query("DELETE FROM model_configs WHERE id = :id")
    suspend fun deleteById(id: String)
}

