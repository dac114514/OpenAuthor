package com.author.faster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.author.faster.core.model.ModelConfig
import com.author.faster.core.model.Project
import com.author.faster.core.model.ProjectPhase
import com.author.faster.core.repository.ModelConfigRepository
import com.author.faster.core.repository.ProjectRepository
import com.author.faster.core.security.ApiKeyStore
import com.author.faster.core.validation.ModelConfigInput
import com.author.faster.core.validation.ProjectInput
import com.author.faster.core.validation.ProjectInputValidator
import com.author.faster.features.ModelConfigCardState
import com.author.faster.features.ModelConfigsUiState
import com.author.faster.features.ProjectCardState
import com.author.faster.features.ProjectCreationDraft
import com.author.faster.features.ProjectCreationIntent
import com.author.faster.features.ProjectCreationUiState
import com.author.faster.features.ProjectsUiState
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

data class OpenAuthorUiState(
    val projects: ProjectsUiState = ProjectsUiState(),
    val modelConfigs: ModelConfigsUiState = ModelConfigsUiState(),
    val projectCreation: ProjectCreationUiState = ProjectCreationUiState(),
)

class OpenAuthorViewModel(
    private val projectRepository: ProjectRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val apiKeyStore: ApiKeyStore,
) : ViewModel() {
    private val projectCreationState = MutableStateFlow(ProjectCreationUiState())

    val uiState: StateFlow<OpenAuthorUiState> = combine(
        projectRepository.observeProjects(),
        modelConfigRepository.observeModelConfigs(),
        projectCreationState,
    ) { projects, configs, creation ->
        val configsById = configs.associateBy(ModelConfig::id)
        OpenAuthorUiState(
            projects = ProjectsUiState(
                projects.map { project ->
                    project.toCardState(project.modelConfigId?.let(configsById::get)?.name)
                },
            ),
            modelConfigs = ModelConfigsUiState(configs.map(ModelConfig::toCardState)),
            projectCreation = creation,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OpenAuthorUiState(),
    )

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

    companion object {
        fun factory(
            projectRepository: ProjectRepository,
            modelConfigRepository: ModelConfigRepository,
            apiKeyStore: ApiKeyStore,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                OpenAuthorViewModel(projectRepository, modelConfigRepository, apiKeyStore) as T
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

private fun Project.toCardState(modelName: String?): ProjectCardState = ProjectCardState(
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
    model = modelName ?: "模型配置缺失",
    generationState = "空闲",
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
