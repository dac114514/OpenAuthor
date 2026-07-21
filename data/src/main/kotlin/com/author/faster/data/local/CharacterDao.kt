package com.author.faster.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters WHERE projectId = :projectId ORDER BY isArchived, updatedAt DESC")
    fun observeCharacters(projectId: String): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM character_relationships WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeRelationships(projectId: String): Flow<List<CharacterRelationshipEntity>>

    @Query("SELECT * FROM character_arcs WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeArcs(projectId: String): Flow<List<CharacterArcEntity>>

    @Query("SELECT * FROM characters WHERE projectId = :projectId AND id = :characterId LIMIT 1")
    suspend fun getCharacter(projectId: String, characterId: String): CharacterEntity?

    @Query("SELECT * FROM character_relationships WHERE projectId = :projectId AND id = :relationshipId LIMIT 1")
    suspend fun getRelationship(projectId: String, relationshipId: String): CharacterRelationshipEntity?

    @Query("SELECT * FROM character_arcs WHERE projectId = :projectId AND characterId = :characterId LIMIT 1")
    suspend fun getArc(projectId: String, characterId: String): CharacterArcEntity?

    @Upsert
    suspend fun upsertCharacter(character: CharacterEntity)

    @Upsert
    suspend fun upsertRelationship(relationship: CharacterRelationshipEntity)

    @Upsert
    suspend fun upsertArc(arc: CharacterArcEntity)

    @Query("DELETE FROM character_relationships WHERE projectId = :projectId AND (sourceCharacterId = :characterId OR targetCharacterId = :characterId)")
    suspend fun deleteRelationshipsForCharacter(projectId: String, characterId: String)

    @Query("DELETE FROM character_arcs WHERE projectId = :projectId AND characterId = :characterId")
    suspend fun deleteArcForCharacter(projectId: String, characterId: String)

    @Query("DELETE FROM characters WHERE projectId = :projectId AND id = :characterId")
    suspend fun deleteCharacter(projectId: String, characterId: String)
}
