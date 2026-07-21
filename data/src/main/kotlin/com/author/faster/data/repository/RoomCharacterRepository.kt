package com.author.faster.data.repository

import com.author.faster.core.model.Character
import com.author.faster.core.model.CharacterArc
import com.author.faster.core.model.CharacterRelationship
import com.author.faster.core.repository.CharacterRepository
import com.author.faster.data.local.CharacterArcEntity
import com.author.faster.data.local.CharacterDao
import com.author.faster.data.local.CharacterEntity
import com.author.faster.data.local.CharacterRelationshipEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCharacterRepository(
    private val dao: CharacterDao,
) : CharacterRepository {
    override fun observeCharacters(projectId: String): Flow<List<Character>> =
        dao.observeCharacters(projectId).map { values -> values.map(CharacterEntity::toDomain) }

    override fun observeRelationships(projectId: String): Flow<List<CharacterRelationship>> =
        dao.observeRelationships(projectId).map { values -> values.map(CharacterRelationshipEntity::toDomain) }

    override fun observeArcs(projectId: String): Flow<List<CharacterArc>> =
        dao.observeArcs(projectId).map { values -> values.map(CharacterArcEntity::toDomain) }

    override suspend fun getCharacter(projectId: String, characterId: String): Character? =
        dao.getCharacter(projectId, characterId)?.toDomain()

    override suspend fun getRelationship(
        projectId: String,
        relationshipId: String,
    ): CharacterRelationship? = dao.getRelationship(projectId, relationshipId)?.toDomain()

    override suspend fun getArc(projectId: String, characterId: String): CharacterArc? =
        dao.getArc(projectId, characterId)?.toDomain()

    override suspend fun saveCharacter(character: Character) {
        dao.upsertCharacter(character.toEntity())
    }

    override suspend fun saveRelationship(relationship: CharacterRelationship) {
        require(getCharacter(relationship.projectId, relationship.sourceCharacterId) != null) {
            "关系起点人物不存在"
        }
        require(getCharacter(relationship.projectId, relationship.targetCharacterId) != null) {
            "关系终点人物不存在"
        }
        dao.upsertRelationship(relationship.toEntity())
    }

    override suspend fun saveArc(arc: CharacterArc) {
        require(getCharacter(arc.projectId, arc.characterId) != null) { "人物不存在" }
        dao.upsertArc(arc.toEntity())
    }

    override suspend fun deleteCharacter(projectId: String, characterId: String) {
        dao.deleteRelationshipsForCharacter(projectId, characterId)
        dao.deleteArcForCharacter(projectId, characterId)
        dao.deleteCharacter(projectId, characterId)
    }
}

private fun CharacterEntity.toDomain() = Character(
    id, projectId, name, aliases, gender, age, identity, publicRumors, factionId, appearance,
    mentionAllowedFromChapter, firstAppearanceChapter, importance, currentStatus, thinkingStyle,
    speechStyle, behaviorHabits, decisionPattern, valuesAndLimits, relationshipHandling,
    informationHandling, background, currentGoal, longTermGoal, coreDesire, coreFear, secret,
    abilitiesAndResources, weaknesses, isArchived, createdAt, updatedAt,
)

private fun Character.toEntity() = CharacterEntity(
    id, projectId, name, aliases, gender, age, identity, publicRumors, factionId, appearance,
    mentionAllowedFromChapter, firstAppearanceChapter, importance, currentStatus, thinkingStyle,
    speechStyle, behaviorHabits, decisionPattern, valuesAndLimits, relationshipHandling,
    informationHandling, background, currentGoal, longTermGoal, coreDesire, coreFear, secret,
    abilitiesAndResources, weaknesses, isArchived, createdAt, updatedAt,
)

private fun CharacterRelationshipEntity.toDomain() = CharacterRelationship(
    id, projectId, sourceCharacterId, targetCharacterId, type, summary, hiddenDetails, createdAt, updatedAt,
)

private fun CharacterRelationship.toEntity() = CharacterRelationshipEntity(
    id, projectId, sourceCharacterId, targetCharacterId, type, summary, hiddenDetails, createdAt, updatedAt,
)

private fun CharacterArcEntity.toDomain() = CharacterArc(
    id, projectId, characterId, arcType, initialState, targetState, currentStage, turningPoints, notes,
    createdAt, updatedAt,
)

private fun CharacterArc.toEntity() = CharacterArcEntity(
    id, projectId, characterId, arcType, initialState, targetState, currentStage, turningPoints, notes,
    createdAt, updatedAt,
)
