package com.author.faster.core.repository

import com.author.faster.core.model.ModelConfig
import kotlinx.coroutines.flow.Flow

interface ModelConfigRepository {
    fun observeModelConfigs(): Flow<List<ModelConfig>>

    suspend fun getModelConfig(id: String): ModelConfig?

    suspend fun saveModelConfig(config: ModelConfig)

    suspend fun deleteModelConfig(id: String)
}

