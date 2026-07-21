package com.author.faster.agent.tools

import com.author.faster.agent.runtime.ToolArgumentValidator
import com.author.faster.agent.runtime.ToolCall
import com.author.faster.agent.runtime.ToolRiskLevel
import com.author.faster.core.model.Character
import com.author.faster.core.model.CharacterArc
import com.author.faster.core.model.CharacterRelationship
import com.author.faster.core.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterToolExecutorTest {
    @Test
    fun everyCharacterToolHasAnObjectSchema() {
        assertEquals(8, CharacterTools.definitions.size)
        assertEquals(8, CharacterTools.definitions.map { it.name }.distinct().size)
        CharacterTools.definitions.forEach { definition ->
            assertEquals("object", definition.inputSchema["type"]?.toString()?.trim('"'))
            assertTrue(ToolArgumentValidator.validate(buildJsonObject {}, definition.inputSchema).errors.all {
                "必填" in it
            })
        }
    }

    @Test
    fun filtersHiddenCharactersAndRedactsMentionOnlyFields() = runBlocking {
        val repository = FakeCharacterRepository()
        repository.saveCharacter(character("hidden", mention = null, appearance = 8, secret = "隐藏秘密"))
        repository.saveCharacter(character("mention", mention = 3, appearance = 8, secret = "不可泄露"))
        repository.saveCharacter(character("full", mention = 1, appearance = 2, secret = "完整秘密"))
        val executor = CharacterToolExecutor(PROJECT_ID, repository)

        val result = executor.execute(
            call(
                CharacterTools.LIST_AVAILABLE,
                ToolRiskLevel.READ,
                buildJsonObject { put("chapter_number", 4) },
            ),
        )

        assertFalse(result.isError)
        assertFalse(result.content.contains("hidden"))
        assertTrue(result.content.contains("mention"))
        assertTrue(result.content.contains("MENTION_ONLY"))
        assertFalse(result.content.contains("不可泄露"))
        assertTrue(result.content.contains("full"))
    }

    @Test
    fun rejectsReadingCharacterBeforeVisibilityBoundary() = runBlocking {
        val repository = FakeCharacterRepository()
        repository.saveCharacter(character("future", mention = 6, appearance = 9, secret = "未来秘密"))
        val executor = CharacterToolExecutor(PROJECT_ID, repository)

        val result = executor.execute(
            call(
                CharacterTools.GET_CHARACTER,
                ToolRiskLevel.READ,
                buildJsonObject {
                    put("character_id", "future")
                    put("chapter_number", 5)
                },
            ),
        )

        assertTrue(result.isError)
        assertFalse(result.content.contains("未来秘密"))
    }

    @Test
    fun createsCharacterAndArcWithValidAppearanceRules() = runBlocking {
        val repository = FakeCharacterRepository()
        val ids = ArrayDeque(listOf("character-1", "arc-1"))
        val executor = CharacterToolExecutor(
            PROJECT_ID,
            repository,
            clock = { 100L },
            newId = { ids.removeFirst() },
        )

        val result = executor.execute(
            call(
                CharacterTools.CREATE_CHARACTER,
                ToolRiskLevel.CREATE,
                buildJsonObject {
                    put("name", "沈潮")
                    put("identity", "领航员")
                    put("mention_allowed_from_chapter", 2)
                    put("first_appearance_chapter", 5)
                    put("arc", buildJsonObject {
                        put("arc_type", "成长")
                        put("current_stage", "拒绝责任")
                    })
                },
            ),
        )

        assertFalse(result.isError)
        assertEquals("沈潮", repository.characters.value.single().name)
        assertEquals("成长", repository.arcs.value.single().arcType)
    }

    private fun character(id: String, mention: Int?, appearance: Int, secret: String) = Character(
        id = id,
        projectId = PROJECT_ID,
        name = id,
        aliases = "别名",
        gender = "",
        age = "",
        identity = "公开身份",
        publicRumors = "公开传闻",
        factionId = null,
        appearance = "外貌",
        mentionAllowedFromChapter = mention,
        firstAppearanceChapter = appearance,
        importance = "主要",
        currentStatus = "正常",
        thinkingStyle = "思考",
        speechStyle = "说话",
        behaviorHabits = "习惯",
        decisionPattern = "决策",
        valuesAndLimits = "底线",
        relationshipHandling = "关系",
        informationHandling = "信息",
        background = "背景",
        currentGoal = "当前目标",
        longTermGoal = "长期目标",
        coreDesire = "欲望",
        coreFear = "恐惧",
        secret = secret,
        abilitiesAndResources = "能力",
        weaknesses = "弱点",
        isArchived = false,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun call(name: String, risk: ToolRiskLevel, arguments: kotlinx.serialization.json.JsonObject) = ToolCall(
        id = "call-$name",
        name = name,
        arguments = arguments,
        riskLevel = risk,
    )

    private class FakeCharacterRepository : CharacterRepository {
        val characters = MutableStateFlow<List<Character>>(emptyList())
        val relationships = MutableStateFlow<List<CharacterRelationship>>(emptyList())
        val arcs = MutableStateFlow<List<CharacterArc>>(emptyList())

        override fun observeCharacters(projectId: String): Flow<List<Character>> = characters
        override fun observeRelationships(projectId: String): Flow<List<CharacterRelationship>> = relationships
        override fun observeArcs(projectId: String): Flow<List<CharacterArc>> = arcs

        override suspend fun getCharacter(projectId: String, characterId: String): Character? =
            characters.value.firstOrNull { it.projectId == projectId && it.id == characterId }

        override suspend fun getRelationship(projectId: String, relationshipId: String): CharacterRelationship? =
            relationships.value.firstOrNull { it.projectId == projectId && it.id == relationshipId }

        override suspend fun getArc(projectId: String, characterId: String): CharacterArc? =
            arcs.value.firstOrNull { it.projectId == projectId && it.characterId == characterId }

        override suspend fun saveCharacter(character: Character) {
            characters.value = characters.value.filterNot { it.id == character.id } + character
        }

        override suspend fun saveRelationship(relationship: CharacterRelationship) {
            relationships.value = relationships.value.filterNot { it.id == relationship.id } + relationship
        }

        override suspend fun saveArc(arc: CharacterArc) {
            arcs.value = arcs.value.filterNot { it.characterId == arc.characterId } + arc
        }

        override suspend fun deleteCharacter(projectId: String, characterId: String) {
            characters.value = characters.value.filterNot { it.projectId == projectId && it.id == characterId }
            relationships.value = relationships.value.filterNot {
                it.sourceCharacterId == characterId || it.targetCharacterId == characterId
            }
            arcs.value = arcs.value.filterNot { it.characterId == characterId }
        }
    }

    private companion object {
        const val PROJECT_ID = "project-1"
    }
}
