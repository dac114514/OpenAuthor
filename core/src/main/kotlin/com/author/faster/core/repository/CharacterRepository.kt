package com.author.faster.core.repository

import com.author.faster.core.model.Character
import com.author.faster.core.model.CharacterArc
import com.author.faster.core.model.CharacterRelationship
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun observeCharacters(projectId: String): Flow<List<Character>>

    fun observeRelationships(projectId: String): Flow<List<CharacterRelationship>>

    fun observeArcs(projectId: String): Flow<List<CharacterArc>>

    suspend fun getCharacter(projectId: String, characterId: String): Character?

    suspend fun getRelationship(projectId: String, relationshipId: String): CharacterRelationship?

    suspend fun getArc(projectId: String, characterId: String): CharacterArc?

    suspend fun saveCharacter(character: Character)

    suspend fun saveRelationship(relationship: CharacterRelationship)

    suspend fun saveArc(arc: CharacterArc)

    suspend fun deleteCharacter(projectId: String, characterId: String)
}
