package com.author.faster.core.model

data class Project(
    val id: String,
    val title: String,
    val author: String,
    val displaySummary: String,
    val creativePremise: String,
    val coverPath: String?,
    val targetChapterCount: Int,
    val defaultWordsPerChapter: Int,
    val genre: String,
    val pov: String,
    val modelConfigId: String?,
    val currentPhase: ProjectPhase,
    val lastSequentialCompletedChapter: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class ProjectPhase {
    SETUP,
    WORLD_BUILDING,
    PLANNING,
    WRITING,
    COMPLETED,
}

data class ModelConfig(
    val id: String,
    val name: String,
    val protocol: ModelProtocol,
    val baseUrl: String,
    val modelId: String,
    val maxContextTokens: Int,
    val maxOutputTokens: Int,
    val temperature: Double,
    val supportsToolCalling: Boolean,
    val supportsStreamingToolCalling: Boolean,
    val extraHeadersJson: String,
    val hasApiKey: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class ModelProtocol(val displayName: String, val defaultBaseUrl: String) {
    OPENAI_COMPATIBLE("OpenAI Compatible", "https://api.openai.com/v1"),
    GEMINI_NATIVE("Gemini Native", "https://generativelanguage.googleapis.com"),
    ANTHROPIC_NATIVE("Anthropic Native", "https://api.anthropic.com"),
}
