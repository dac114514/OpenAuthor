package com.author.faster.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agent_runs",
    indices = [Index("projectId")],
)
data class AgentRunEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val agentType: String,
    val executionMode: String,
    val status: String,
    val stepCount: Int,
    val message: String?,
    val startedAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "agent_tool_calls",
    indices = [Index("agentRunId")],
    primaryKeys = ["agentRunId", "id"],
)
data class AgentToolCallEntity(
    val id: String,
    val agentRunId: String,
    val toolName: String,
    val argumentsJson: String,
    val riskLevel: String,
    val status: String,
    val attempt: Int,
    val result: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "pending_tool_calls",
    indices = [Index("agentRunId")],
    primaryKeys = ["agentRunId", "id"],
)
data class PendingToolCallEntity(
    val id: String,
    val agentRunId: String,
    val toolName: String,
    val argumentsJson: String,
    val displayTitle: String,
    val displayDescription: String,
    val riskLevel: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)
