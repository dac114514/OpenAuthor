package com.author.faster.agent.tools

import com.author.faster.agent.runtime.ToolCall
import com.author.faster.agent.runtime.ToolDefinition
import com.author.faster.agent.runtime.ToolExecutionResult
import com.author.faster.agent.runtime.ToolExecutor
import com.author.faster.agent.runtime.ToolRiskLevel
import com.author.faster.core.model.Character
import com.author.faster.core.model.CharacterArc
import com.author.faster.core.model.CharacterContextLevel
import com.author.faster.core.model.CharacterRelationship
import com.author.faster.core.model.contextLevelAt
import com.author.faster.core.repository.CharacterRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

object CharacterTools {
    const val LIST_AVAILABLE = "list_available_characters"
    const val GET_CHARACTER = "get_character"
    const val CREATE_CHARACTER = "create_character"
    const val UPDATE_CHARACTER = "update_character"
    const val ARCHIVE_CHARACTER = "archive_character"
    const val DELETE_CHARACTER = "delete_character"
    const val LIST_RELATIONSHIPS = "list_relationships"
    const val UPDATE_RELATIONSHIP = "update_relationship"

    val definitions = listOf(
        ToolDefinition(
            LIST_AVAILABLE,
            "列出指定章节可使用的人物；自动隐藏尚未允许出场的人物",
            objectSchema(
                required = listOf("chapter_number"),
                properties = mapOf("chapter_number" to integerSchema("目标章节号", minimum = 1)),
            ),
            ToolRiskLevel.READ,
        ),
        ToolDefinition(
            GET_CHARACTER,
            "按章节权限读取人物；提前提及时只返回公开信息",
            objectSchema(
                required = listOf("character_id", "chapter_number"),
                properties = mapOf(
                    "character_id" to stringSchema("人物 ID"),
                    "chapter_number" to integerSchema("目标章节号", minimum = 1),
                ),
            ),
            ToolRiskLevel.READ,
        ),
        ToolDefinition(
            CREATE_CHARACTER,
            "创建人物卡，可同时设置人物弧光",
            objectSchema(
                required = listOf("name", "identity", "first_appearance_chapter"),
                properties = characterProperties(includeId = false),
            ),
            ToolRiskLevel.CREATE,
        ),
        ToolDefinition(
            UPDATE_CHARACTER,
            "更新人物卡或人物弧光；只提交需要修改的字段",
            objectSchema(
                required = listOf("character_id"),
                properties = characterProperties(includeId = true),
            ),
            ToolRiskLevel.UPDATE,
        ),
        ToolDefinition(
            ARCHIVE_CHARACTER,
            "归档人物，归档后默认不再进入可用人物列表",
            idSchema("character_id", "人物 ID"),
            ToolRiskLevel.UPDATE,
        ),
        ToolDefinition(
            DELETE_CHARACTER,
            "删除人物及其关系和弧光",
            idSchema("character_id", "人物 ID"),
            ToolRiskLevel.DELETE,
        ),
        ToolDefinition(
            LIST_RELATIONSHIPS,
            "列出指定章节可见的人物关系，并过滤未出场人物和隐藏关系信息",
            objectSchema(
                required = listOf("chapter_number"),
                properties = mapOf(
                    "chapter_number" to integerSchema("目标章节号", minimum = 1),
                    "character_id" to stringSchema("可选的人物 ID 筛选"),
                ),
            ),
            ToolRiskLevel.READ,
        ),
        ToolDefinition(
            UPDATE_RELATIONSHIP,
            "创建或更新两个人物之间的关系",
            objectSchema(
                required = listOf("source_character_id", "target_character_id", "type", "summary"),
                properties = mapOf(
                    "relationship_id" to stringSchema("已有关系 ID；新建时省略"),
                    "source_character_id" to stringSchema("关系起点人物 ID"),
                    "target_character_id" to stringSchema("关系终点人物 ID"),
                    "type" to stringSchema("关系类型"),
                    "summary" to freeStringSchema("公开关系摘要"),
                    "hidden_details" to freeStringSchema("仅完整上下文可读取的隐藏信息"),
                ),
            ),
            ToolRiskLevel.UPDATE,
        ),
    )
}

class CharacterToolExecutor(
    private val projectId: String,
    private val repository: CharacterRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ToolExecutor {
    override suspend fun execute(call: ToolCall): ToolExecutionResult = try {
        when (call.name) {
            CharacterTools.LIST_AVAILABLE -> listAvailable(call.arguments.requiredInt("chapter_number"))
            CharacterTools.GET_CHARACTER -> getCharacter(
                call.arguments.requiredString("character_id"),
                call.arguments.requiredInt("chapter_number"),
            )
            CharacterTools.CREATE_CHARACTER -> createCharacter(call.arguments)
            CharacterTools.UPDATE_CHARACTER -> updateCharacter(call.arguments)
            CharacterTools.ARCHIVE_CHARACTER -> archiveCharacter(call.arguments.requiredString("character_id"))
            CharacterTools.DELETE_CHARACTER -> deleteCharacter(call.arguments.requiredString("character_id"))
            CharacterTools.LIST_RELATIONSHIPS -> listRelationships(
                call.arguments.requiredInt("chapter_number"),
                call.arguments.optionalString("character_id"),
            )
            CharacterTools.UPDATE_RELATIONSHIP -> updateRelationship(call.arguments)
            else -> ToolExecutionResult("不支持的人物工具：${call.name}", isError = true)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        ToolExecutionResult(
            content = "人物操作失败：${error.message ?: "请检查参数后重试"}",
            isError = true,
        )
    }

    private suspend fun listAvailable(chapter: Int): ToolExecutionResult {
        require(chapter >= 1) { "章节号必须大于 0" }
        val characters = mutableListOf<JsonObject>()
        repository.observeCharacters(projectId).first()
            .filterNot(Character::isArchived)
            .forEach { character ->
                when (val level = character.contextLevelAt(chapter)) {
                    CharacterContextLevel.HIDDEN -> Unit
                    CharacterContextLevel.MENTION_ONLY -> characters += character.toMentionJson(level)
                    CharacterContextLevel.FULL -> characters += character.toFullJson(
                        level,
                        repository.getArc(projectId, character.id),
                    )
                }
            }
        return ToolExecutionResult(buildJsonArray { characters.forEach { add(it) } }.toString())
    }

    private suspend fun getCharacter(characterId: String, chapter: Int): ToolExecutionResult {
        require(chapter >= 1) { "章节号必须大于 0" }
        val character = repository.getCharacter(projectId, characterId) ?: error("人物不存在")
        require(!character.isArchived) { "人物已归档" }
        val level = character.contextLevelAt(chapter)
        require(level != CharacterContextLevel.HIDDEN) { "该人物在目标章节尚不可用" }
        val value = when (level) {
            CharacterContextLevel.MENTION_ONLY -> character.toMentionJson(level)
            CharacterContextLevel.FULL -> character.toFullJson(level, repository.getArc(projectId, characterId))
            CharacterContextLevel.HIDDEN -> error("该人物在目标章节尚不可用")
        }
        return ToolExecutionResult(value.toString())
    }

    private suspend fun createCharacter(arguments: JsonObject): ToolExecutionResult {
        val existing = repository.observeCharacters(projectId).first()
        val name = arguments.requiredString("name").trim()
        require(name.isNotBlank()) { "人物姓名不能为空" }
        require(existing.none { it.name == name }) { "同名人物已存在" }
        val now = clock()
        val character = Character(
            id = newId(),
            projectId = projectId,
            name = name,
            aliases = arguments.optionalString("aliases").orEmpty().trim(),
            gender = arguments.optionalString("gender").orEmpty().trim(),
            age = arguments.optionalString("age").orEmpty().trim(),
            identity = arguments.requiredString("identity").trim(),
            publicRumors = arguments.optionalString("public_rumors").orEmpty().trim(),
            factionId = arguments.optionalString("faction_id")?.trim()?.ifBlank { null },
            appearance = arguments.optionalString("appearance").orEmpty().trim(),
            mentionAllowedFromChapter = arguments.optionalInt("mention_allowed_from_chapter"),
            firstAppearanceChapter = arguments.requiredInt("first_appearance_chapter"),
            importance = arguments.optionalString("importance").orEmpty().trim(),
            currentStatus = arguments.optionalString("current_status").orEmpty().trim(),
            thinkingStyle = arguments.optionalString("thinking_style").orEmpty().trim(),
            speechStyle = arguments.optionalString("speech_style").orEmpty().trim(),
            behaviorHabits = arguments.optionalString("behavior_habits").orEmpty().trim(),
            decisionPattern = arguments.optionalString("decision_pattern").orEmpty().trim(),
            valuesAndLimits = arguments.optionalString("values_and_limits").orEmpty().trim(),
            relationshipHandling = arguments.optionalString("relationship_handling").orEmpty().trim(),
            informationHandling = arguments.optionalString("information_handling").orEmpty().trim(),
            background = arguments.optionalString("background").orEmpty().trim(),
            currentGoal = arguments.optionalString("current_goal").orEmpty().trim(),
            longTermGoal = arguments.optionalString("long_term_goal").orEmpty().trim(),
            coreDesire = arguments.optionalString("core_desire").orEmpty().trim(),
            coreFear = arguments.optionalString("core_fear").orEmpty().trim(),
            secret = arguments.optionalString("secret").orEmpty().trim(),
            abilitiesAndResources = arguments.optionalString("abilities_and_resources").orEmpty().trim(),
            weaknesses = arguments.optionalString("weaknesses").orEmpty().trim(),
            isArchived = false,
            createdAt = now,
            updatedAt = now,
        )
        validateAppearanceRules(character)
        repository.saveCharacter(character)
        saveArcIfPresent(character, arguments.optionalObject("arc"), now)
        return ToolExecutionResult(
            character.toFullJson(CharacterContextLevel.FULL, repository.getArc(projectId, character.id)).toString(),
        )
    }

    private suspend fun updateCharacter(arguments: JsonObject): ToolExecutionResult {
        val characterId = arguments.requiredString("character_id")
        val existing = repository.getCharacter(projectId, characterId) ?: error("人物不存在")
        val updated = existing.copy(
            name = arguments.optionalString("name")?.trim() ?: existing.name,
            aliases = arguments.optionalString("aliases")?.trim() ?: existing.aliases,
            gender = arguments.optionalString("gender")?.trim() ?: existing.gender,
            age = arguments.optionalString("age")?.trim() ?: existing.age,
            identity = arguments.optionalString("identity")?.trim() ?: existing.identity,
            publicRumors = arguments.optionalString("public_rumors")?.trim() ?: existing.publicRumors,
            factionId = if (arguments.containsKey("faction_id")) {
                arguments.optionalString("faction_id")?.trim()?.ifBlank { null }
            } else existing.factionId,
            appearance = arguments.optionalString("appearance")?.trim() ?: existing.appearance,
            mentionAllowedFromChapter = if (arguments.containsKey("mention_allowed_from_chapter")) {
                arguments.optionalInt("mention_allowed_from_chapter")
            } else existing.mentionAllowedFromChapter,
            firstAppearanceChapter = arguments.optionalInt("first_appearance_chapter")
                ?: existing.firstAppearanceChapter,
            importance = arguments.optionalString("importance")?.trim() ?: existing.importance,
            currentStatus = arguments.optionalString("current_status")?.trim() ?: existing.currentStatus,
            thinkingStyle = arguments.optionalString("thinking_style")?.trim() ?: existing.thinkingStyle,
            speechStyle = arguments.optionalString("speech_style")?.trim() ?: existing.speechStyle,
            behaviorHabits = arguments.optionalString("behavior_habits")?.trim() ?: existing.behaviorHabits,
            decisionPattern = arguments.optionalString("decision_pattern")?.trim() ?: existing.decisionPattern,
            valuesAndLimits = arguments.optionalString("values_and_limits")?.trim() ?: existing.valuesAndLimits,
            relationshipHandling = arguments.optionalString("relationship_handling")?.trim()
                ?: existing.relationshipHandling,
            informationHandling = arguments.optionalString("information_handling")?.trim()
                ?: existing.informationHandling,
            background = arguments.optionalString("background")?.trim() ?: existing.background,
            currentGoal = arguments.optionalString("current_goal")?.trim() ?: existing.currentGoal,
            longTermGoal = arguments.optionalString("long_term_goal")?.trim() ?: existing.longTermGoal,
            coreDesire = arguments.optionalString("core_desire")?.trim() ?: existing.coreDesire,
            coreFear = arguments.optionalString("core_fear")?.trim() ?: existing.coreFear,
            secret = arguments.optionalString("secret")?.trim() ?: existing.secret,
            abilitiesAndResources = arguments.optionalString("abilities_and_resources")?.trim()
                ?: existing.abilitiesAndResources,
            weaknesses = arguments.optionalString("weaknesses")?.trim() ?: existing.weaknesses,
            updatedAt = clock(),
        )
        require(updated.name.isNotBlank()) { "人物姓名不能为空" }
        validateAppearanceRules(updated)
        repository.saveCharacter(updated)
        saveArcIfPresent(updated, arguments.optionalObject("arc"), updated.updatedAt)
        return ToolExecutionResult(
            updated.toFullJson(CharacterContextLevel.FULL, repository.getArc(projectId, characterId)).toString(),
        )
    }

    private suspend fun archiveCharacter(characterId: String): ToolExecutionResult {
        val character = repository.getCharacter(projectId, characterId) ?: error("人物不存在")
        repository.saveCharacter(character.copy(isArchived = true, updatedAt = clock()))
        return ToolExecutionResult(buildJsonObject { put("archived_character_id", characterId) }.toString())
    }

    private suspend fun deleteCharacter(characterId: String): ToolExecutionResult {
        require(repository.getCharacter(projectId, characterId) != null) { "人物不存在" }
        repository.deleteCharacter(projectId, characterId)
        return ToolExecutionResult(buildJsonObject { put("deleted_character_id", characterId) }.toString())
    }

    private suspend fun listRelationships(chapter: Int, characterId: String?): ToolExecutionResult {
        require(chapter >= 1) { "章节号必须大于 0" }
        val characters = repository.observeCharacters(projectId).first().associateBy(Character::id)
        val visible = repository.observeRelationships(projectId).first().mapNotNull { relationship ->
            if (characterId != null &&
                relationship.sourceCharacterId != characterId && relationship.targetCharacterId != characterId
            ) return@mapNotNull null
            val source = characters[relationship.sourceCharacterId] ?: return@mapNotNull null
            val target = characters[relationship.targetCharacterId] ?: return@mapNotNull null
            val sourceLevel = source.contextLevelAt(chapter)
            val targetLevel = target.contextLevelAt(chapter)
            if (source.isArchived || target.isArchived ||
                sourceLevel == CharacterContextLevel.HIDDEN || targetLevel == CharacterContextLevel.HIDDEN
            ) return@mapNotNull null
            relationship.toJson(
                source,
                target,
                includeHidden = sourceLevel == CharacterContextLevel.FULL &&
                    targetLevel == CharacterContextLevel.FULL,
            )
        }
        return ToolExecutionResult(buildJsonArray { visible.forEach { add(it) } }.toString())
    }

    private suspend fun updateRelationship(arguments: JsonObject): ToolExecutionResult {
        val sourceId = arguments.requiredString("source_character_id")
        val targetId = arguments.requiredString("target_character_id")
        require(sourceId != targetId) { "人物不能与自己建立关系" }
        require(repository.getCharacter(projectId, sourceId) != null) { "关系起点人物不存在" }
        require(repository.getCharacter(projectId, targetId) != null) { "关系终点人物不存在" }
        val relationshipId = arguments.optionalString("relationship_id") ?: newId()
        val existing = repository.getRelationship(projectId, relationshipId)
        val now = clock()
        val relationship = CharacterRelationship(
            id = relationshipId,
            projectId = projectId,
            sourceCharacterId = sourceId,
            targetCharacterId = targetId,
            type = arguments.requiredString("type").trim(),
            summary = arguments.requiredString("summary").trim(),
            hiddenDetails = arguments.optionalString("hidden_details")?.trim()
                ?: existing?.hiddenDetails.orEmpty(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        require(relationship.type.isNotBlank()) { "关系类型不能为空" }
        repository.saveRelationship(relationship)
        return ToolExecutionResult(relationship.toJson(null, null, includeHidden = true).toString())
    }

    private suspend fun saveArcIfPresent(character: Character, value: JsonObject?, now: Long) {
        if (value == null) return
        val existing = repository.getArc(projectId, character.id)
        repository.saveArc(
            CharacterArc(
                id = existing?.id ?: newId(),
                projectId = projectId,
                characterId = character.id,
                arcType = value.optionalString("arc_type")?.trim() ?: existing?.arcType.orEmpty(),
                initialState = value.optionalString("initial_state")?.trim() ?: existing?.initialState.orEmpty(),
                targetState = value.optionalString("target_state")?.trim() ?: existing?.targetState.orEmpty(),
                currentStage = value.optionalString("current_stage")?.trim() ?: existing?.currentStage.orEmpty(),
                turningPoints = value.optionalString("turning_points")?.trim() ?: existing?.turningPoints.orEmpty(),
                notes = value.optionalString("notes")?.trim() ?: existing?.notes.orEmpty(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }
}

private fun validateAppearanceRules(character: Character) {
    require(character.firstAppearanceChapter >= 1) { "正式出场章节必须大于 0" }
    character.mentionAllowedFromChapter?.let { mentionChapter ->
        require(mentionChapter >= 1) { "允许提及章节必须大于 0" }
        require(mentionChapter <= character.firstAppearanceChapter) { "允许提及章节不能晚于正式出场章节" }
    }
}

private fun objectSchema(
    required: List<String> = emptyList(),
    properties: Map<String, JsonObject> = emptyMap(),
): JsonObject = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    put("properties", JsonObject(properties))
    if (required.isNotEmpty()) put("required", JsonArray(required.map(::JsonPrimitive)))
}

private fun idSchema(name: String, description: String) = objectSchema(
    required = listOf(name),
    properties = mapOf(name to stringSchema(description)),
)

private fun stringSchema(description: String): JsonObject = buildJsonObject {
    put("type", "string")
    put("description", description)
    put("minLength", 1)
}

private fun freeStringSchema(description: String): JsonObject = buildJsonObject {
    put("type", "string")
    put("description", description)
}

private fun integerSchema(description: String, minimum: Int): JsonObject = buildJsonObject {
    put("type", "integer")
    put("description", description)
    put("minimum", minimum)
}

private fun arcSchema(): JsonObject = objectSchema(
    properties = mapOf(
        "arc_type" to freeStringSchema("弧光类型"),
        "initial_state" to freeStringSchema("初始状态"),
        "target_state" to freeStringSchema("目标状态"),
        "current_stage" to freeStringSchema("当前弧光阶段"),
        "turning_points" to freeStringSchema("关键转折点"),
        "notes" to freeStringSchema("弧光备注"),
    ),
)

private fun characterProperties(includeId: Boolean): Map<String, JsonObject> = buildMap {
    if (includeId) put("character_id", stringSchema("人物 ID"))
    put("name", stringSchema("姓名"))
    put("aliases", freeStringSchema("别名"))
    put("gender", freeStringSchema("性别"))
    put("age", freeStringSchema("年龄"))
    put("identity", stringSchema("公开身份"))
    put("public_rumors", freeStringSchema("可提前提及的公开传闻"))
    put("faction_id", freeStringSchema("所属势力 ID"))
    put("appearance", freeStringSchema("外貌"))
    put("mention_allowed_from_chapter", integerSchema("允许被提及章节", 1))
    put("first_appearance_chapter", integerSchema("正式出场章节", 1))
    put("importance", freeStringSchema("重要程度"))
    put("current_status", freeStringSchema("当前状态"))
    put("thinking_style", freeStringSchema("思考方式"))
    put("speech_style", freeStringSchema("说话方式"))
    put("behavior_habits", freeStringSchema("行为习惯"))
    put("decision_pattern", freeStringSchema("决策模式"))
    put("values_and_limits", freeStringSchema("价值观与底线"))
    put("relationship_handling", freeStringSchema("关系处理"))
    put("information_handling", freeStringSchema("信息处理"))
    put("background", freeStringSchema("人物背景"))
    put("current_goal", freeStringSchema("当前目标"))
    put("long_term_goal", freeStringSchema("长期目标"))
    put("core_desire", freeStringSchema("核心欲望"))
    put("core_fear", freeStringSchema("核心恐惧"))
    put("secret", freeStringSchema("秘密"))
    put("abilities_and_resources", freeStringSchema("能力与资源"))
    put("weaknesses", freeStringSchema("弱点"))
    put("arc", arcSchema())
}

private fun JsonObject.requiredString(name: String): String =
    optionalString(name) ?: error("缺少参数：$name")

private fun JsonObject.optionalString(name: String): String? =
    (get(name) as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content

private fun JsonObject.requiredInt(name: String): Int = optionalInt(name) ?: error("缺少参数：$name")

private fun JsonObject.optionalInt(name: String): Int? = (get(name) as? JsonPrimitive)?.intOrNull

private fun JsonObject.optionalObject(name: String): JsonObject? = get(name) as? JsonObject

private fun Character.toMentionJson(level: CharacterContextLevel): JsonObject = buildJsonObject {
    put("id", id)
    put("name", name)
    put("aliases", aliases)
    put("identity", identity)
    put("public_rumors", publicRumors)
    put("context_level", level.name)
}

private fun Character.toFullJson(
    level: CharacterContextLevel,
    arc: CharacterArc?,
): JsonObject = buildJsonObject {
    put("id", id)
    put("name", name)
    put("aliases", aliases)
    put("identity", identity)
    put("importance", importance)
    put("current_status", currentStatus)
    put("first_appearance_chapter", firstAppearanceChapter)
    mentionAllowedFromChapter?.let { put("mention_allowed_from_chapter", it) }
    put("context_level", level.name)
    put("gender", gender)
    put("age", age)
    put("public_rumors", publicRumors)
    factionId?.let { put("faction_id", it) }
    put("appearance", appearance)
    put("thinking_style", thinkingStyle)
    put("speech_style", speechStyle)
    put("behavior_habits", behaviorHabits)
    put("decision_pattern", decisionPattern)
    put("values_and_limits", valuesAndLimits)
    put("relationship_handling", relationshipHandling)
    put("information_handling", informationHandling)
    put("background", background)
    put("current_goal", currentGoal)
    put("long_term_goal", longTermGoal)
    put("core_desire", coreDesire)
    put("core_fear", coreFear)
    put("secret", secret)
    put("abilities_and_resources", abilitiesAndResources)
    put("weaknesses", weaknesses)
    arc?.let { put("arc", it.toJson()) }
}

private fun CharacterArc.toJson(): JsonObject = buildJsonObject {
    put("arc_type", arcType)
    put("initial_state", initialState)
    put("target_state", targetState)
    put("current_stage", currentStage)
    put("turning_points", turningPoints)
    put("notes", notes)
}

private fun CharacterRelationship.toJson(
    source: Character?,
    target: Character?,
    includeHidden: Boolean,
): JsonObject = buildJsonObject {
    put("id", id)
    put("source_character_id", sourceCharacterId)
    source?.let { put("source_name", it.name) }
    put("target_character_id", targetCharacterId)
    target?.let { put("target_name", it.name) }
    put("type", type)
    put("summary", summary)
    if (includeHidden) put("hidden_details", hiddenDetails)
}
