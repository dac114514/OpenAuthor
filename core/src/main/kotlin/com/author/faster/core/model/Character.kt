package com.author.faster.core.model

data class Character(
    val id: String,
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

data class CharacterRelationship(
    val id: String,
    val projectId: String,
    val sourceCharacterId: String,
    val targetCharacterId: String,
    val type: String,
    val summary: String,
    val hiddenDetails: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CharacterArc(
    val id: String,
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

enum class CharacterContextLevel {
    HIDDEN,
    MENTION_ONLY,
    FULL,
}

fun Character.contextLevelAt(chapter: Int): CharacterContextLevel = when {
    chapter >= firstAppearanceChapter -> CharacterContextLevel.FULL
    mentionAllowedFromChapter != null && chapter >= mentionAllowedFromChapter ->
        CharacterContextLevel.MENTION_ONLY
    else -> CharacterContextLevel.HIDDEN
}
