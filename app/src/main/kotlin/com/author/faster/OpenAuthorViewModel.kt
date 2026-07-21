package com.author.faster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.author.faster.core.model.Project
import com.author.faster.core.repository.ProjectRepository
import com.author.faster.features.ProjectCardState
import com.author.faster.features.ProjectsUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class OpenAuthorUiState(
    val projects: ProjectsUiState = ProjectsUiState(),
    val isProjectCreationRequested: Boolean = false,
)

class OpenAuthorViewModel(
    projectRepository: ProjectRepository,
) : ViewModel() {
    val uiState: StateFlow<OpenAuthorUiState> = projectRepository.observeProjects()
        .map { projects -> OpenAuthorUiState(projects = ProjectsUiState(projects.map(Project::toCardState))) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OpenAuthorUiState(),
        )

    fun startProjectCreation() {
        // The project wizard is implemented in Task 2. Keeping this event at the ViewModel boundary
        // prevents the initial UI scaffold from bypassing the workflow layer.
    }

    companion object {
        fun factory(repository: ProjectRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    OpenAuthorViewModel(repository) as T
            }
    }
}

private fun Project.toCardState(): ProjectCardState = ProjectCardState(
    id = id,
    title = title,
    author = author,
    currentChapter = lastSequentialCompletedChapter,
    targetChapters = targetChapterCount,
    phase = currentPhase.name,
    model = if (modelConfigId == null) "未配置模型" else "项目模型",
    generationState = "空闲",
)
