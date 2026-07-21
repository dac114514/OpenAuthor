package com.author.faster.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_configs")
data class ModelConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val protocol: String,
    val baseUrl: String,
    val apiKeyAlias: String,
    val modelId: String,
    val maxContextTokens: Int,
    val maxOutputTokens: Int,
    val temperature: Double,
    val supportsToolCalling: Boolean,
    val supportsStreamingToolCalling: Boolean,
    val extraHeadersJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

