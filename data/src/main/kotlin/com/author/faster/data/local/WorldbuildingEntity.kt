package com.author.faster.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "world_categories",
    indices = [
        Index("projectId"),
        Index(value = ["projectId", "name"], unique = true),
    ],
)
data class WorldCategoryEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val description: String,
    val fieldSchemaJson: String,
    val isBuiltIn: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "world_entries",
    indices = [
        Index("projectId"),
        Index("categoryId"),
        Index(value = ["projectId", "categoryId", "title"], unique = true),
    ],
)
data class WorldEntryEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val categoryId: String,
    val title: String,
    val summary: String,
    val content: String,
    val structuredFieldsJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)
