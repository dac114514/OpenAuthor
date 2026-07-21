package com.author.faster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.author.faster.agent.provider.OpenAiCompatibleModelProvider
import com.author.faster.agent.runtime.AgentExecutionMode
import com.author.faster.agent.runtime.AgentMessage
import com.author.faster.agent.runtime.AgentResult
import com.author.faster.agent.runtime.AgentRunStore
import com.author.faster.agent.runtime.AgentRuntime
import com.author.faster.agent.runtime.AgentSession
import com.author.faster.agent.runtime.PendingToolCallStore
import com.author.faster.agent.runtime.ToolCall
import com.author.faster.agent.tools.WorldbuildingToolExecutor
import com.author.faster.agent.tools.WorldbuildingTools
import com.author.faster.core.model.ModelConfig
import com.author.faster.core.model.Project
import com.author.faster.core.model.ProjectPhase
import com.author.faster.core.model.WorldCategory
import com.author.faster.core.model.WorldEntry
import com.author.faster.core.repository.ModelConfigRepository
import com.author.faster.core.repository.ProjectRepository
import com.author.faster.core.repository.WorldbuildingRepository
import com.author.faster.core.security.ApiKeyStore
import com.author.faster.core.validation.ModelConfigInput
import com.author.faster.core.validation.ProjectInput
import com.author.faster.core.validation.ProjectInputValidator
import com.author.faster.features.ModelConfigCardState
import com.author.faster.features.ModelConfigsUiState
import com.author.faster.features.PendingWorldToolCallState
import com.author.faster.features.ProjectCardState
import com.author.faster.features.ProjectCreationDraft
import com.author.faster.features.ProjectCreationIntent
import com.author.faster.features.ProjectCreationUiState
import com.author.faster.features.ProjectsUiState
import com.author.faster.features.WorldCategoryCardState
import com.author.faster.features.WorldCategoryDraft
import com.author.faster.features.WorldEntryCardState
import com.author.faster.features.WorldEntryDraft
import com.author.faster.features.WorldAgentMessageState
import com.author.faster.features.WorldAgentMode
import com.author.faster.features.WorldbuildingUiState
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

data class OpenAuthorUiState(
    val projects: ProjectsUiState = ProjectsUiState(),
    val modelConfigs: ModelConfigsUiState = ModelConfigsUiState(),
    val projectCreation: ProjectCreationUiState = ProjectCreationUiState(),
    val activeProject: ProjectCardState? = null,
    val worldbuilding: WorldbuildingUiState = WorldbuildingUiState(),
)

private data class WorldOperationState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val agentMessages: List<WorldAgentMessageState> = emptyList(),
    val isAgentRunning: Boolean = false,
    val pendingToolCall: PendingWorldToolCallState? = null,
)

class OpenAuthorViewModel(
    private val projectRepository: ProjectRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val apiKeyStore: ApiKeyStore,
    private val worldbuildingRepository: WorldbuildingRepository,
    private val agentRunStore: AgentRunStore,
    private val pendingToolCallStore: PendingToolCallStore,
) : ViewModel() {
    private val projectCreationState = MutableStateFlow(ProjectCreationUiState())
    private val activeProjectId = MutableStateFlow<String?>(null)
    private val worldOperationState = MutableStateFlow(WorldOperationState())
    private val worldAgentMessages = mutableListOf<AgentMessage>()
    private var pendingAgentSession: AgentSession? = null
    private var pendingAgentCall: ToolCall? = null
    private var pendingAgentRuntime: AgentRuntime? = null

    private val activeWorldContent = activeProjectId.flatMapLatest { projectId ->
        if (projectId == null) {
            flowOf(WorldbuildingUiState())
        } else {
            combine(
                worldbuildingRepository.observeCategories(projectId),
                worldbuildingRepository.observeEntries(projectId),
            ) { categories, entries ->
                categories.toUiState(entries)
            }
        }
    }

    private val activeWorldbuilding = combine(activeWorldContent, worldOperationState) { content, operation ->
        content.copy(
            isSaving = operation.isSaving,
            errorMessage = operation.errorMessage,
            agentMessages = operation.agentMessages,
            isAgentRunning = operation.isAgentRunning,
            pendingToolCall = operation.pendingToolCall,
        )
    }

    val uiState: StateFlow<OpenAuthorUiState> = combine(
        projectRepository.observeProjects(),
        modelConfigRepository.observeModelConfigs(),
        projectCreationState,
        activeProjectId,
        activeWorldbuilding,
    ) { projects, configs, creation, selectedProjectId, worldbuilding ->
        val configsById = configs.associateBy(ModelConfig::id)
        val projectCards = projects.map { project ->
            project.toCardState(project.modelConfigId?.let(configsById::get))
        }
        OpenAuthorUiState(
            projects = ProjectsUiState(projectCards),
            modelConfigs = ModelConfigsUiState(configs.map(ModelConfig::toCardState)),
            projectCreation = creation,
            activeProject = projectCards.firstOrNull { it.id == selectedProjectId },
            worldbuilding = worldbuilding,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OpenAuthorUiState(),
    )

    fun openProject(id: String) {
        activeProjectId.value = id
        worldOperationState.value = WorldOperationState()
        clearPendingWorldAgent()
        worldAgentMessages.clear()
        viewModelScope.launch {
            runCatching { worldbuildingRepository.ensureBuiltInCategories(id) }
                .onFailure { error ->
                    worldOperationState.update {
                        it.copy(errorMessage = "初始化世界观分类失败：${error.message ?: "请稍后重试"}")
                    }
                }
        }
    }

    fun closeProject() {
        activeProjectId.value = null
        worldOperationState.value = WorldOperationState()
        clearPendingWorldAgent()
        worldAgentMessages.clear()
    }

    fun createWorldCategory(draft: WorldCategoryDraft) {
        val projectId = activeProjectId.value ?: return
        val schema = parseJsonObject(draft.fieldSchemaJson, "字段 Schema") ?: return
        launchWorldMutation {
            val categories = worldbuildingRepository.observeCategories(projectId).first()
            require(categories.none { it.name == draft.name.trim() }) { "同名世界观分类已存在" }
            val now = System.currentTimeMillis()
            worldbuildingRepository.saveCategory(
                WorldCategory(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    name = draft.name.trim(),
                    description = draft.description.trim(),
                    fieldSchemaJson = schema.toString(),
                    isBuiltIn = false,
                    sortOrder = (categories.maxOfOrNull(WorldCategory::sortOrder) ?: -1) + 1,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            markProjectWorldbuilding(projectId)
        }
    }

    fun saveWorldEntry(draft: WorldEntryDraft) {
        val projectId = activeProjectId.value ?: return
        val structuredFields = parseJsonObject(draft.structuredFieldsJson, "结构化字段") ?: return
        launchWorldMutation {
            require(draft.title.isNotBlank()) { "条目标题不能为空" }
            require(draft.content.isNotBlank()) { "条目内容不能为空" }
            require(worldbuildingRepository.getCategory(projectId, draft.categoryId) != null) {
                "世界观分类不存在"
            }
            val now = System.currentTimeMillis()
            val existing = draft.id?.let { worldbuildingRepository.getEntry(projectId, it) }
            worldbuildingRepository.saveEntry(
                WorldEntry(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    projectId = projectId,
                    categoryId = draft.categoryId,
                    title = draft.title.trim(),
                    summary = draft.summary.trim(),
                    content = draft.content.trim(),
                    structuredFieldsJson = structuredFields.toString(),
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            markProjectWorldbuilding(projectId)
        }
    }

    fun deleteWorldEntry(entryId: String) {
        val projectId = activeProjectId.value ?: return
        launchWorldMutation {
            require(worldbuildingRepository.getEntry(projectId, entryId) != null) { "世界观条目不存在" }
            worldbuildingRepository.deleteEntry(projectId, entryId)
            markProjectWorldbuilding(projectId)
        }
    }

    fun runWorldAgent(prompt: String, mode: WorldAgentMode) {
        val projectId = activeProjectId.value ?: return
        if (prompt.isBlank() || worldOperationState.value.isAgentRunning) return
        val userText = prompt.trim()
        worldOperationState.update {
            it.copy(
                agentMessages = it.agentMessages + WorldAgentMessageState(
                    id = UUID.randomUUID().toString(),
                    isUser = true,
                    content = userText,
                ),
                isAgentRunning = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                val project = projectRepository.getProject(projectId) ?: error("项目不存在")
                val configId = project.modelConfigId ?: error("项目尚未绑定模型")
                val config = modelConfigRepository.getModelConfig(configId) ?: error("模型配置不存在")
                require(config.supportsToolCalling) { "当前模型配置未启用工具调用" }
                val apiKey = apiKeyStore.get(configId) ?: error("模型 API Key 不存在")
                if (worldAgentMessages.isEmpty()) {
                    worldAgentMessages += AgentMessage.System(
                        """
                        你是长篇小说项目的世界观 Agent。只处理世界观分类和条目；读取后再修改，
                        不得调用未提供的工具，不得改动人物、分卷或章节。项目核心创意：${project.creativePremise}
                        """.trimIndent(),
                    )
                }
                worldAgentMessages += AgentMessage.User(userText)
                val session = AgentSession(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    agentType = "worldbuilding",
                    mode = if (mode == WorldAgentMode.MANUAL) AgentExecutionMode.MANUAL else AgentExecutionMode.SOLO,
                    messages = worldAgentMessages.toList(),
                    availableTools = WorldbuildingTools.definitions,
                )
                val runtime = AgentRuntime(
                    modelProvider = OpenAiCompatibleModelProvider.create(config, apiKey),
                    toolExecutor = WorldbuildingToolExecutor(projectId, worldbuildingRepository),
                    pendingToolCallStore = pendingToolCallStore,
                    runStore = agentRunStore,
                )
                val result = runtime.run(session)
                markProjectWorldbuilding(projectId)
                handleWorldAgentResult(result, runtime, session)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                appendWorldAgentMessage(
                    "世界观 Agent 运行失败：${error.message ?: "请检查模型配置后重试"}",
                    isError = true,
                )
                worldOperationState.update { it.copy(isAgentRunning = false) }
            }
        }
    }

    fun resolveWorldTool(approved: Boolean) {
        val runtime = pendingAgentRuntime ?: return
        val session = pendingAgentSession ?: return
        val call = pendingAgentCall ?: return
        if (worldOperationState.value.isAgentRunning) return
        worldOperationState.update { it.copy(isAgentRunning = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                clearPendingWorldAgent(clearUi = false)
                val result = runtime.resumeAfterConfirmation(session, call, approved)
                session.projectId?.let { markProjectWorldbuilding(it) }
                handleWorldAgentResult(
                    result,
                    runtime,
                    session,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                appendWorldAgentMessage(
                    "处理工具确认失败：${error.message ?: "请稍后重试"}",
                    isError = true,
                )
                worldOperationState.update { it.copy(isAgentRunning = false, pendingToolCall = null) }
            }
        }
    }

    fun onProjectCreationIntent(intent: ProjectCreationIntent) {
        when (intent) {
            ProjectCreationIntent.Open -> projectCreationState.value = ProjectCreationUiState(isOpen = true)
            ProjectCreationIntent.Dismiss -> if (!projectCreationState.value.isSaving) {
                projectCreationState.value = ProjectCreationUiState()
            }
            ProjectCreationIntent.Back -> projectCreationState.update {
                it.copy(step = (it.step - 1).coerceAtLeast(0), errorMessage = null)
            }
            ProjectCreationIntent.Next -> advanceWizard()
            ProjectCreationIntent.Save -> saveProject()
            is ProjectCreationIntent.UpdateDraft -> projectCreationState.update {
                it.copy(draft = intent.draft, errorMessage = null)
            }
        }
    }

    private fun advanceWizard() {
        val state = projectCreationState.value
        val error = validateStep(state.step, state.draft)
        projectCreationState.update {
            if (error == null) it.copy(step = (it.step + 1).coerceAtMost(2), errorMessage = null)
            else it.copy(errorMessage = error)
        }
    }

    private fun saveProject() {
        val draft = projectCreationState.value.draft
        val error = validateAll(draft)
        if (error != null) {
            projectCreationState.update { it.copy(errorMessage = error) }
            return
        }

        projectCreationState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val projectId = UUID.randomUUID().toString()
            val configId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            try {
                apiKeyStore.save(configId, draft.apiKey.trim())
                modelConfigRepository.saveModelConfig(draft.toModelConfig(configId, now))
                projectRepository.saveProject(draft.toProject(projectId, configId, now))
                runCatching { worldbuildingRepository.ensureBuiltInCategories(projectId) }
                projectCreationState.value = ProjectCreationUiState()
            } catch (cancellation: CancellationException) {
                runCatching { modelConfigRepository.deleteModelConfig(configId) }
                runCatching { apiKeyStore.delete(configId) }
                throw cancellation
            } catch (throwable: Exception) {
                runCatching { modelConfigRepository.deleteModelConfig(configId) }
                runCatching { apiKeyStore.delete(configId) }
                projectCreationState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "保存失败：${throwable.message ?: "请检查本地存储后重试"}",
                    )
                }
            }
        }
    }

    private fun validateStep(step: Int, draft: ProjectCreationDraft): String? = when (step) {
        0 -> when {
            draft.title.isBlank() -> "请填写书名"
            draft.author.isBlank() -> "请填写作者"
            draft.creativePremise.isBlank() -> "请填写核心创意"
            else -> null
        }
        1 -> ProjectInputValidator.validateProject(draft.toProjectInput()).firstOrNull()
        else -> validateModel(draft)
    }

    private fun validateAll(draft: ProjectCreationDraft): String? =
        ProjectInputValidator.validateProject(draft.toProjectInput()).firstOrNull() ?: validateModel(draft)

    private fun validateModel(draft: ProjectCreationDraft): String? {
        ProjectInputValidator.validateModel(draft.toModelInput()).firstOrNull()?.let { return it }
        val headers = runCatching { Json.parseToJsonElement(draft.extraHeadersJson) }.getOrNull()
        return if (headers is JsonObject) null else "额外请求头必须是 JSON 对象"
    }

    private fun parseJsonObject(value: String, label: String): JsonObject? {
        val parsed = runCatching { Json.parseToJsonElement(value) }.getOrNull()
        if (parsed is JsonObject) return parsed
        worldOperationState.update { it.copy(errorMessage = "$label 必须是 JSON 对象") }
        return null
    }

    private fun launchWorldMutation(block: suspend () -> Unit) {
        worldOperationState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                block()
                worldOperationState.update { it.copy(isSaving = false, errorMessage = null) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                worldOperationState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "保存世界观失败：${error.message ?: "请检查内容后重试"}",
                    )
                }
            }
        }
    }

    private fun handleWorldAgentResult(
        result: AgentResult,
        runtime: AgentRuntime,
        session: AgentSession,
    ) {
        when (result) {
            is AgentResult.Completed -> {
                worldAgentMessages += AgentMessage.Assistant(result.text)
                appendWorldAgentMessage(result.text)
                clearPendingWorldAgent(clearUi = false)
                worldOperationState.update { it.copy(isAgentRunning = false, pendingToolCall = null) }
            }
            is AgentResult.WaitingConfirmation -> {
                pendingAgentRuntime = runtime
                pendingAgentSession = session
                pendingAgentCall = result.toolCall
                worldOperationState.update {
                    it.copy(
                        isAgentRunning = false,
                        pendingToolCall = PendingWorldToolCallState(
                            id = result.toolCall.id,
                            title = "确认执行 ${result.toolCall.name}",
                            description = "风险等级：${result.toolCall.riskLevel.name}；参数：${result.toolCall.arguments}",
                        ),
                    )
                }
            }
            is AgentResult.Failed -> finishWorldAgentWithError(result.message)
            AgentResult.StepLimitReached -> finishWorldAgentWithError("Agent 已达到最大步数")
            AgentResult.ToolCallLimitReached -> finishWorldAgentWithError("Agent 已达到连续工具调用上限")
            AgentResult.TimedOut -> finishWorldAgentWithError("Agent 运行超时")
        }
    }

    private fun finishWorldAgentWithError(message: String) {
        appendWorldAgentMessage(message, isError = true)
        clearPendingWorldAgent(clearUi = false)
        worldOperationState.update { it.copy(isAgentRunning = false, pendingToolCall = null) }
    }

    private fun appendWorldAgentMessage(message: String, isError: Boolean = false) {
        worldOperationState.update {
            it.copy(
                agentMessages = it.agentMessages + WorldAgentMessageState(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    content = message,
                    isError = isError,
                ),
            )
        }
    }

    private fun clearPendingWorldAgent(clearUi: Boolean = true) {
        pendingAgentRuntime = null
        pendingAgentSession = null
        pendingAgentCall = null
        if (clearUi) worldOperationState.update { it.copy(pendingToolCall = null) }
    }

    private suspend fun markProjectWorldbuilding(projectId: String) {
        val project = projectRepository.getProject(projectId) ?: return
        if (project.currentPhase == ProjectPhase.SETUP) {
            projectRepository.saveProject(
                project.copy(
                    currentPhase = ProjectPhase.WORLD_BUILDING,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    companion object {
        fun factory(
            projectRepository: ProjectRepository,
            modelConfigRepository: ModelConfigRepository,
            apiKeyStore: ApiKeyStore,
            worldbuildingRepository: WorldbuildingRepository,
            agentRunStore: AgentRunStore,
            pendingToolCallStore: PendingToolCallStore,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                OpenAuthorViewModel(
                    projectRepository,
                    modelConfigRepository,
                    apiKeyStore,
                    worldbuildingRepository,
                    agentRunStore,
                    pendingToolCallStore,
                ) as T
        }
    }
}

private fun ProjectCreationDraft.toProjectInput() = ProjectInput(
    title = title,
    author = author,
    creativePremise = creativePremise,
    targetChapterCount = targetChapterCount.toIntOrNull() ?: 0,
    defaultWordsPerChapter = defaultWordsPerChapter.toIntOrNull() ?: 0,
)

private fun ProjectCreationDraft.toModelInput() = ModelConfigInput(
    name = modelConfigName,
    baseUrl = baseUrl,
    apiKey = apiKey,
    modelId = modelId,
    maxContextTokens = maxContextTokens.toIntOrNull() ?: 0,
    maxOutputTokens = maxOutputTokens.toIntOrNull() ?: 0,
    temperature = temperature.toDoubleOrNull() ?: -1.0,
)

private fun ProjectCreationDraft.toModelConfig(id: String, now: Long) = ModelConfig(
    id = id,
    name = modelConfigName.trim(),
    protocol = protocol,
    baseUrl = baseUrl.trim().trimEnd('/'),
    modelId = modelId.trim(),
    maxContextTokens = maxContextTokens.toInt(),
    maxOutputTokens = maxOutputTokens.toInt(),
    temperature = temperature.toDouble(),
    supportsToolCalling = supportsToolCalling,
    supportsStreamingToolCalling = supportsStreamingToolCalling,
    extraHeadersJson = extraHeadersJson.trim(),
    hasApiKey = true,
    createdAt = now,
    updatedAt = now,
)

private fun ProjectCreationDraft.toProject(id: String, configId: String, now: Long) = Project(
    id = id,
    title = title.trim(),
    author = author.trim(),
    displaySummary = displaySummary.trim(),
    creativePremise = creativePremise.trim(),
    coverPath = null,
    targetChapterCount = targetChapterCount.toInt(),
    defaultWordsPerChapter = defaultWordsPerChapter.toInt(),
    genre = genre.trim(),
    pov = pov.trim(),
    modelConfigId = configId,
    currentPhase = ProjectPhase.SETUP,
    lastSequentialCompletedChapter = 0,
    createdAt = now,
    updatedAt = now,
)

private fun Project.toCardState(modelConfig: ModelConfig?): ProjectCardState = ProjectCardState(
    id = id,
    title = title,
    author = author,
    currentChapter = lastSequentialCompletedChapter,
    targetChapters = targetChapterCount,
    phase = when (currentPhase) {
        ProjectPhase.SETUP -> "初始化"
        ProjectPhase.WORLD_BUILDING -> "设定中"
        ProjectPhase.PLANNING -> "规划中"
        ProjectPhase.WRITING -> "创作中"
        ProjectPhase.COMPLETED -> "已完成"
    },
    model = modelConfig?.let { "${it.protocol.displayName} · ${it.modelId}" } ?: "模型配置缺失",
    generationState = "空闲",
    lastUpdated = DateTimeFormatter.ofPattern("MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(updatedAt)),
    consistencyState = if (lastSequentialCompletedChapter == 0) "未初始化" else "正常",
)

private fun ModelConfig.toCardState() = ModelConfigCardState(
    id = id,
    name = name,
    protocol = protocol.displayName,
    baseUrl = baseUrl,
    modelId = modelId,
    maxContextTokens = maxContextTokens,
    maxOutputTokens = maxOutputTokens,
    supportsToolCalling = supportsToolCalling,
    supportsStreamingToolCalling = supportsStreamingToolCalling,
    hasApiKey = hasApiKey,
)

private fun List<WorldCategory>.toUiState(entries: List<WorldEntry>): WorldbuildingUiState {
    val entryCounts = entries.groupingBy(WorldEntry::categoryId).eachCount()
    return WorldbuildingUiState(
        categories = map { category ->
            WorldCategoryCardState(
                id = category.id,
                name = category.name,
                description = category.description,
                fieldSchemaJson = category.fieldSchemaJson,
                isBuiltIn = category.isBuiltIn,
                entryCount = entryCounts[category.id] ?: 0,
            )
        },
        entries = entries.map { entry ->
            WorldEntryCardState(
                id = entry.id,
                categoryId = entry.categoryId,
                title = entry.title,
                summary = entry.summary,
                content = entry.content,
                structuredFieldsJson = entry.structuredFieldsJson,
            )
        },
    )
}
