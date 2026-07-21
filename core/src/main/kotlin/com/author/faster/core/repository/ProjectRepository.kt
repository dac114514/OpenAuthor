package com.author.faster.core.repository

import com.author.faster.core.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>

    suspend fun getProject(id: String): Project?

    suspend fun saveProject(project: Project)

    suspend fun deleteProject(id: String)
}

