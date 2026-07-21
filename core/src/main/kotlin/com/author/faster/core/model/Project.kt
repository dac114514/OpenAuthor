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

