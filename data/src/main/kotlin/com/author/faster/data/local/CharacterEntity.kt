package com.author.faster.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "characters",
    indices = [
        Index("projectId"),
        Index(value = ["projectId", "name"], unique = true),
    ],
)
data class CharacterEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val aliases: String,
    val gender: String,
    val age: String,
    val identity: String,
    val publicRumors: String,
    val factionId: String?,
    val appearance: String,
    val mentionAllowedFromChapter: Int?,
    val firstAppearanceChapter: Int,
    val importance: String,
    val currentStatus: String,
    val thinkingStyle: String,
    val speechStyle: String,
    val behaviorHabits: String,
    val decisionPattern: String,
    val valuesAndLimits: String,
    val relationshipHandling: String,
    val informationHandling: String,
    val background: String,
    val currentGoal: String,
    val longTermGoal: String,
    val coreDesire: String,
    val coreFear: String,
    val secret: String,
    val abilitiesAndResources: String,
    val weaknesses: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "character_relationships",
    indices = [
        Index("projectId"),
        Index("sourceCharacterId"),
        Index("targetCharacterId"),
        Index(
            value = ["projectId", "sourceCharacterId", "targetCharacterId", "type"],
            unique = true,
        ),
    ],
)
data class CharacterRelationshipEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val sourceCharacterId: String,
    val targetCharacterId: String,
    val type: String,
    val summary: String,
    val hiddenDetails: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "character_arcs",
    indices = [
        Index("projectId"),
        Index(value = ["projectId", "characterId"], unique = true),
    ],
)
data class CharacterArcEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val characterId: String,
    val arcType: String,
    val initialState: String,
    val targetState: String,
    val currentStage: String,
    val turningPoints: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)
