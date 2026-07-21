package com.author.faster

import com.author.faster.agent.provider.OpenAiCompatibleModelProvider
import com.author.faster.agent.runtime.AgentExecutionMode
import com.author.faster.agent.runtime.AgentMessage
import com.author.faster.agent.runtime.AgentResult
import com.author.faster.agent.runtime.AgentRunStore
import com.author.faster.agent.runtime.AgentRuntime
import com.author.faster.agent.runtime.AgentSession
import com.author.faster.agent.runtime.PendingToolCallStore
import com.author.faster.agent.runtime.ToolCall
import com.author.faster.agent.tools.CharacterToolExecutor
import com.author.faster.agent.tools.CharacterTools
import com.author.faster.core.model.Character
import com.author.faster.core.model.CharacterArc
import com.author.faster.core.model.CharacterRelationship
import com.author.faster.core.model.ProjectPhase
import com.author.faster.core.repository.CharacterRepository
import com.author.faster.core.repository.ModelConfigRepository
import com.author.faster.core.repository.ProjectRepository
import com.author.faster.core.security.ApiKeyStore
import com.author.faster.features.CharacterAgentMode
import com.author.faster.features.CharacterArcCardState
import com.author.faster.features.CharacterCardState
import com.author.faster.features.CharacterDraft
import com.author.faster.features.CharacterRelationshipCardState
import com.author.faster.features.CharacterRelationshipDraft
import com.author.faster.features.CharacterUiState
import com.author.faster.features.PendingWorldToolCallState
import com.author.faster.features.WorldAgentMessageState
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class CharacterOperationState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val agentMessages: List<WorldAgentMessageState> = emptyList(),
    val isAgentRunning: Boolean = false,
    val pendingToolCall: PendingWorldToolCallState? = null,
)

class CharacterFeatureController(
    private val scope: CoroutineScope,
    private val activeProjectId: StateFlow<String?>,
    private val projectRepository: ProjectRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val apiKeyStore: ApiKeyStore,
    private val characterRepository: CharacterRepository,
    private val agentRunStore: AgentRunStore,
    private val pendingToolCallStore: PendingToolCallStore,
) {
    private val operationState = MutableStateFlow(CharacterOperationState())
    private val agentMessages = mutableListOf<AgentMessage>()
    private var pendingSession: AgentSession? = null
    private var pendingCall: ToolCall? = null
    private var pendingRuntime: AgentRuntime? = null

    private val content = activeProjectId.flatMapLatest { projectId ->
        if (projectId == null) {
            flowOf(CharacterUiState())
        } else {
            combine(
                characterRepository.observeCharacters(projectId),
                characterRepository.observeRelationships(projectId),
                characterRepository.observeArcs(projectId),
            ) { characters, relationships, arcs ->
                toUiState(characters, relationships, arcs)
            }
        }
    }

    val uiState: StateFlow<CharacterUiState> = combine(content, operationState) { content, operation ->
        content.copy(
            isSaving = operation.isSaving,
            errorMessage = operation.errorMessage,
            agentMessages = operation.agentMessages,
            isAgentRunning = operation.isAgentRunning,
            pendingToolCall = operation.pendingToolCall,
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), CharacterUiState())

    fun reset() {
        operationState.value = CharacterOperationState()
        agentMessages.clear()
        clearPending()
    }

    fun saveCharacter(draft: CharacterDraft) {
        val projectId = activeProjectId.value ?: return
        val firstAppearance = draft.firstAppearanceChapter.toIntOrNull()
        val mentionAllowed = draft.mentionAllowedFromChapter.trim().takeIf(String::isNotEmpty)?.toIntOrNull()
        if (firstAppearance == null || firstAppearance < 1) {
            operationState.update { it.copy(errorMessage = "正式出场章节必须是大于 0 的整数") }
            return
        }
        if (draft.mentionAllowedFromChapter.isNotBlank() && mentionAllowed == null) {
            operationState.update { it.copy(errorMessage = "允许提及章节必须是整数或留空") }
            return
        }
        if (mentionAllowed != null && (mentionAllowed < 1 || mentionAllowed > firstAppearance)) {
            operationState.update { it.copy(errorMessage = "允许提及章节必须大于 0，且不能晚于正式出场章节") }
            return
        }
        launchMutation {
            require(draft.name.isNotBlank()) { "人物姓名不能为空" }
            require(draft.identity.isNotBlank()) { "人物公开身份不能为空" }
            val existing = draft.id?.let { characterRepository.getCharacter(projectId, it) }
            val characters = characterRepository.observeCharacters(projectId).first()
            require(characters.none { it.id != existing?.id && it.name == draft.name.trim() }) { "同名人物已存在" }
            val now = System.currentTimeMillis()
            val character = Character(
                id = existing?.id ?: UUID.randomUUID().toString(),
                projectId = projectId,
                name = draft.name.trim(),
                aliases = draft.aliases.trim(),
                gender = draft.gender.trim(),
                age = draft.age.trim(),
                identity = draft.identity.trim(),
                publicRumors = draft.publicRumors.trim(),
                factionId = draft.factionId.trim().ifBlank { null },
                appearance = draft.appearance.trim(),
                mentionAllowedFromChapter = mentionAllowed,
                firstAppearanceChapter = firstAppearance,
                importance = draft.importance.trim(),
                currentStatus = draft.currentStatus.trim(),
                thinkingStyle = draft.thinkingStyle.trim(),
                speechStyle = draft.speechStyle.trim(),
                behaviorHabits = draft.behaviorHabits.trim(),
                decisionPattern = draft.decisionPattern.trim(),
                valuesAndLimits = draft.valuesAndLimits.trim(),
                relationshipHandling = draft.relationshipHandling.trim(),
                informationHandling = draft.informationHandling.trim(),
                background = draft.background.trim(),
                currentGoal = draft.currentGoal.trim(),
                longTermGoal = draft.longTermGoal.trim(),
                coreDesire = draft.coreDesire.trim(),
                coreFear = draft.coreFear.trim(),
                secret = draft.secret.trim(),
                abilitiesAndResources = draft.abilitiesAndResources.trim(),
                weaknesses = draft.weaknesses.trim(),
                isArchived = existing?.isArchived ?: false,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            characterRepository.saveCharacter(character)
            val existingArc = characterRepository.getArc(projectId, character.id)
            if (draft.hasArcContent() || existingArc != null) {
                characterRepository.saveArc(
                    CharacterArc(
                        id = existingArc?.id ?: UUID.randomUUID().toString(),
                        projectId = projectId,
                        characterId = character.id,
                        arcType = draft.arcType.trim(),
                        initialState = draft.arcInitialState.trim(),
                        targetState = draft.arcTargetState.trim(),
                        currentStage = draft.arcCurrentStage.trim(),
                        turningPoints = draft.arcTurningPoints.trim(),
                        notes = draft.arcNotes.trim(),
                        createdAt = existingArc?.createdAt ?: now,
                        updatedAt = now,
                    ),
                )
            }
            markProjectWorldbuilding(projectId)
        }
    }

    fun archiveCharacter(characterId: String) {
        val projectId = activeProjectId.value ?: return
        launchMutation {
            val character = characterRepository.getCharacter(projectId, characterId) ?: error("人物不存在")
            characterRepository.saveCharacter(
                character.copy(isArchived = true, updatedAt = System.currentTimeMillis()),
            )
        }
    }

    fun deleteCharacter(characterId: String) {
        val projectId = activeProjectId.value ?: return
        launchMutation {
            require(characterRepository.getCharacter(projectId, characterId) != null) { "人物不存在" }
            characterRepository.deleteCharacter(projectId, characterId)
        }
    }

    fun saveRelationship(draft: CharacterRelationshipDraft) {
        val projectId = activeProjectId.value ?: return
        launchMutation {
            require(draft.sourceCharacterId.isNotBlank() && draft.targetCharacterId.isNotBlank()) {
                "请选择关系两端的人物"
            }
            require(draft.sourceCharacterId != draft.targetCharacterId) { "人物不能与自己建立关系" }
            require(draft.type.isNotBlank()) { "关系类型不能为空" }
            require(characterRepository.getCharacter(projectId, draft.sourceCharacterId) != null) {
                "关系起点人物不存在"
            }
            require(characterRepository.getCharacter(projectId, draft.targetCharacterId) != null) {
                "关系终点人物不存在"
            }
            val existing = draft.id?.let { characterRepository.getRelationship(projectId, it) }
            val now = System.currentTimeMillis()
            characterRepository.saveRelationship(
                CharacterRelationship(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    projectId = projectId,
                    sourceCharacterId = draft.sourceCharacterId,
                    targetCharacterId = draft.targetCharacterId,
                    type = draft.type.trim(),
                    summary = draft.summary.trim(),
                    hiddenDetails = draft.hiddenDetails.trim(),
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            markProjectWorldbuilding(projectId)
        }
    }

    fun runAgent(prompt: String, mode: CharacterAgentMode) {
        val projectId = activeProjectId.value ?: return
        if (prompt.isBlank() || operationState.value.isAgentRunning) return
        val userText = prompt.trim()
        operationState.update {
            it.copy(
                agentMessages = it.agentMessages + WorldAgentMessageState(
                    UUID.randomUUID().toString(),
                    isUser = true,
                    content = userText,
                ),
                isAgentRunning = true,
                errorMessage = null,
            )
        }
        scope.launch {
            try {
                val project = projectRepository.getProject(projectId) ?: error("项目不存在")
                val configId = project.modelConfigId ?: error("项目尚未绑定模型")
                val config = modelConfigRepository.getModelConfig(configId) ?: error("模型配置不存在")
                require(config.supportsToolCalling) { "当前模型配置未启用工具调用" }
                val apiKey = apiKeyStore.get(configId) ?: error("模型 API Key 不存在")
                if (agentMessages.isEmpty()) {
                    agentMessages += AgentMessage.System(
                        """
                        你是长篇小说项目的人物 Agent。只处理人物卡、人物关系和人物弧光。
                        任何面向具体章节的读取都必须使用真实章节号，并遵守 HIDDEN、MENTION_ONLY、FULL 过滤；
                        不得猜测或泄露未到首次出场条件的人物信息。项目核心创意：${project.creativePremise}
                        """.trimIndent(),
                    )
                }
                agentMessages += AgentMessage.User(userText)
                val session = AgentSession(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    agentType = "character",
                    mode = if (mode == CharacterAgentMode.MANUAL) {
                        AgentExecutionMode.MANUAL
                    } else AgentExecutionMode.SOLO,
                    messages = agentMessages.toList(),
                    availableTools = CharacterTools.definitions,
                )
                val runtime = AgentRuntime(
                    modelProvider = OpenAiCompatibleModelProvider.create(config, apiKey),
                    toolExecutor = CharacterToolExecutor(projectId, characterRepository),
                    pendingToolCallStore = pendingToolCallStore,
                    runStore = agentRunStore,
                )
                val result = runtime.run(session)
                markProjectWorldbuilding(projectId)
                handleAgentResult(result, runtime, session)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                appendAgentMessage("人物 Agent 运行失败：${error.message ?: "请检查模型配置后重试"}", true)
                operationState.update { it.copy(isAgentRunning = false) }
            }
        }
    }

    fun resolveTool(approved: Boolean) {
        val runtime = pendingRuntime ?: return
        val session = pendingSession ?: return
        val call = pendingCall ?: return
        if (operationState.value.isAgentRunning) return
        operationState.update { it.copy(isAgentRunning = true, errorMessage = null) }
        scope.launch {
            try {
                clearPending(clearUi = false)
                val result = runtime.resumeAfterConfirmation(session, call, approved)
                session.projectId?.let { markProjectWorldbuilding(it) }
                handleAgentResult(result, runtime, session)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                appendAgentMessage("处理人物工具确认失败：${error.message ?: "请稍后重试"}", true)
                operationState.update { it.copy(isAgentRunning = false, pendingToolCall = null) }
            }
        }
    }

    private fun launchMutation(block: suspend () -> Unit) {
        operationState.update { it.copy(isSaving = true, errorMessage = null) }
        scope.launch {
            try {
                block()
                operationState.update { it.copy(isSaving = false, errorMessage = null) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                operationState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "保存人物失败：${error.message ?: "请检查内容后重试"}",
                    )
                }
            }
        }
    }

    private fun handleAgentResult(result: AgentResult, runtime: AgentRuntime, session: AgentSession) {
        when (result) {
            is AgentResult.Completed -> {
                agentMessages += AgentMessage.Assistant(result.text)
                appendAgentMessage(result.text)
                clearPending(clearUi = false)
                operationState.update { it.copy(isAgentRunning = false, pendingToolCall = null) }
            }
            is AgentResult.WaitingConfirmation -> {
                pendingRuntime = runtime
                pendingSession = session
                pendingCall = result.toolCall
                operationState.update {
                    it.copy(
                        isAgentRunning = false,
                        pendingToolCall = PendingWorldToolCallState(
                            result.toolCall.id,
                            "确认执行 ${result.toolCall.name}",
                            "风险等级：${result.toolCall.riskLevel.name}；参数：${result.toolCall.arguments}",
                        ),
                    )
                }
            }
            is AgentResult.Failed -> finishWithError(result.message)
            AgentResult.StepLimitReached -> finishWithError("Agent 已达到最大步数")
            AgentResult.ToolCallLimitReached -> finishWithError("Agent 已达到连续工具调用上限")
            AgentResult.TimedOut -> finishWithError("Agent 运行超时")
        }
    }

    private fun finishWithError(message: String) {
        appendAgentMessage(message, true)
        clearPending(clearUi = false)
        operationState.update { it.copy(isAgentRunning = false, pendingToolCall = null) }
    }

    private fun appendAgentMessage(message: String, isError: Boolean = false) {
        operationState.update {
            it.copy(
                agentMessages = it.agentMessages + WorldAgentMessageState(
                    UUID.randomUUID().toString(),
                    isUser = false,
                    content = message,
                    isError = isError,
                ),
            )
        }
    }

    private fun clearPending(clearUi: Boolean = true) {
        pendingRuntime = null
        pendingSession = null
        pendingCall = null
        if (clearUi) operationState.update { it.copy(pendingToolCall = null) }
    }

    private suspend fun markProjectWorldbuilding(projectId: String) {
        val project = projectRepository.getProject(projectId) ?: return
        if (project.currentPhase == ProjectPhase.SETUP) {
            projectRepository.saveProject(
                project.copy(currentPhase = ProjectPhase.WORLD_BUILDING, updatedAt = System.currentTimeMillis()),
            )
        }
    }
}

private fun CharacterDraft.hasArcContent(): Boolean = listOf(
    arcType,
    arcInitialState,
    arcTargetState,
    arcCurrentStage,
    arcTurningPoints,
    arcNotes,
).any(String::isNotBlank)

private fun toUiState(
    characters: List<Character>,
    relationships: List<CharacterRelationship>,
    arcs: List<CharacterArc>,
): CharacterUiState {
    val arcsByCharacter = arcs.associateBy(CharacterArc::characterId)
    val charactersById = characters.associateBy(Character::id)
    return CharacterUiState(
        characters = characters.map { character ->
            val arc = arcsByCharacter[character.id]
            CharacterCardState(
                id = character.id,
                name = character.name,
                aliases = character.aliases,
                gender = character.gender,
                age = character.age,
                identity = character.identity,
                publicRumors = character.publicRumors,
                factionId = character.factionId,
                appearance = character.appearance,
                mentionAllowedFromChapter = character.mentionAllowedFromChapter,
                firstAppearanceChapter = character.firstAppearanceChapter,
                importance = character.importance,
                currentStatus = character.currentStatus,
                thinkingStyle = character.thinkingStyle,
                speechStyle = character.speechStyle,
                behaviorHabits = character.behaviorHabits,
                decisionPattern = character.decisionPattern,
                valuesAndLimits = character.valuesAndLimits,
                relationshipHandling = character.relationshipHandling,
                informationHandling = character.informationHandling,
                background = character.background,
                currentGoal = character.currentGoal,
                longTermGoal = character.longTermGoal,
                coreDesire = character.coreDesire,
                coreFear = character.coreFear,
                secret = character.secret,
                abilitiesAndResources = character.abilitiesAndResources,
                weaknesses = character.weaknesses,
                isArchived = character.isArchived,
                arc = arc?.let {
                    CharacterArcCardState(
                        it.arcType,
                        it.initialState,
                        it.targetState,
                        it.currentStage,
                        it.turningPoints,
                        it.notes,
                    )
                },
            )
        },
        relationships = relationships.map { relationship ->
            CharacterRelationshipCardState(
                id = relationship.id,
                sourceCharacterId = relationship.sourceCharacterId,
                sourceName = charactersById[relationship.sourceCharacterId]?.name ?: "未知人物",
                targetCharacterId = relationship.targetCharacterId,
                targetName = charactersById[relationship.targetCharacterId]?.name ?: "未知人物",
                type = relationship.type,
                summary = relationship.summary,
                hiddenDetails = relationship.hiddenDetails,
            )
        },
    )
}
