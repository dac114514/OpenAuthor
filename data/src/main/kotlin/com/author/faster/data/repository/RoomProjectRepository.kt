package com.author.faster.data.repository

import com.author.faster.core.model.Project
import com.author.faster.core.model.ProjectPhase
import com.author.faster.core.repository.ProjectRepository
import com.author.faster.data.local.ProjectDao
import com.author.faster.data.local.ProjectEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomProjectRepository(
    private val projectDao: ProjectDao,
) : ProjectRepository {
    override fun observeProjects(): Flow<List<Project>> =
        projectDao.observeAll().map { projects -> projects.map(ProjectEntity::toDomain) }

    override suspend fun getProject(id: String): Project? = projectDao.getById(id)?.toDomain()

    override suspend fun saveProject(project: Project) {
        projectDao.upsert(project.toEntity())
    }

    override suspend fun deleteProject(id: String) {
        projectDao.deleteById(id)
    }
}

private fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    title = title,
    author = author,
    displaySummary = displaySummary,
    creativePremise = creativePremise,
    coverPath = coverPath,
    targetChapterCount = targetChapterCount,
    defaultWordsPerChapter = defaultWordsPerChapter,
    genre = genre,
    pov = pov,
    modelConfigId = modelConfigId,
    currentPhase = ProjectPhase.valueOf(currentPhase),
    lastSequentialCompletedChapter = lastSequentialCompletedChapter,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    title = title,
    author = author,
    displaySummary = displaySummary,
    creativePremise = creativePremise,
    coverPath = coverPath,
    targetChapterCount = targetChapterCount,
    defaultWordsPerChapter = defaultWordsPerChapter,
    genre = genre,
    pov = pov,
    modelConfigId = modelConfigId,
    currentPhase = currentPhase.name,
    lastSequentialCompletedChapter = lastSequentialCompletedChapter,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

