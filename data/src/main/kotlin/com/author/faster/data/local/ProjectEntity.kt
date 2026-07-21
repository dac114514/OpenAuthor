package com.author.faster.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
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
    val currentPhase: String,
    val lastSequentialCompletedChapter: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

