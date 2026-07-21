package com.author.faster.data.repository

import com.author.faster.core.model.ModelConfig
import com.author.faster.core.model.ModelProtocol
import com.author.faster.core.repository.ModelConfigRepository
import com.author.faster.data.local.ModelConfigDao
import com.author.faster.data.local.ModelConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomModelConfigRepository(
    private val modelConfigDao: ModelConfigDao,
) : ModelConfigRepository {
    override fun observeModelConfigs(): Flow<List<ModelConfig>> =
        modelConfigDao.observeAll().map { configs -> configs.map(ModelConfigEntity::toDomain) }

    override suspend fun getModelConfig(id: String): ModelConfig? = modelConfigDao.getById(id)?.toDomain()

    override suspend fun saveModelConfig(config: ModelConfig) {
        modelConfigDao.upsert(config.toEntity())
    }

    override suspend fun deleteModelConfig(id: String) {
        modelConfigDao.deleteById(id)
    }
}

private fun ModelConfigEntity.toDomain(): ModelConfig = ModelConfig(
    id = id,
    name = name,
    protocol = ModelProtocol.valueOf(protocol),
    baseUrl = baseUrl,
    modelId = modelId,
    maxContextTokens = maxContextTokens,
    maxOutputTokens = maxOutputTokens,
    temperature = temperature,
    supportsToolCalling = supportsToolCalling,
    supportsStreamingToolCalling = supportsStreamingToolCalling,
    extraHeadersJson = extraHeadersJson,
    hasApiKey = apiKeyAlias.isNotBlank(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun ModelConfig.toEntity(): ModelConfigEntity = ModelConfigEntity(
    id = id,
    name = name,
    protocol = protocol.name,
    baseUrl = baseUrl,
    apiKeyAlias = id,
    modelId = modelId,
    maxContextTokens = maxContextTokens,
    maxOutputTokens = maxOutputTokens,
    temperature = temperature,
    supportsToolCalling = supportsToolCalling,
    supportsStreamingToolCalling = supportsStreamingToolCalling,
    extraHeadersJson = extraHeadersJson,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

