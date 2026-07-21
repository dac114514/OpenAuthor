package com.author.faster.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentRunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRun(run: AgentRunEntity)

    @Query(
        """
        UPDATE agent_runs
        SET status = :status, stepCount = :stepCount, message = :message, updatedAt = :updatedAt
        WHERE id = :runId
        """,
    )
    suspend fun updateRun(
        runId: String,
        status: String,
        stepCount: Int,
        message: String?,
        updatedAt: Long,
    )

    @Query("SELECT * FROM agent_runs WHERE id = :runId LIMIT 1")
    suspend fun getRun(runId: String): AgentRunEntity?

    @Query("SELECT * FROM agent_runs WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeRuns(projectId: String): Flow<List<AgentRunEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertToolCall(call: AgentToolCallEntity)

    @Query(
        """
        UPDATE agent_tool_calls
        SET status = :status, attempt = :attempt, updatedAt = :updatedAt
        WHERE agentRunId = :runId AND id = :callId
        """,
    )
    suspend fun restartToolCall(runId: String, callId: String, status: String, attempt: Int, updatedAt: Long)

    @Query(
        """
        UPDATE agent_tool_calls
        SET status = :status, result = :result, errorMessage = :errorMessage, updatedAt = :updatedAt
        WHERE agentRunId = :runId AND id = :callId
        """,
    )
    suspend fun finishToolCall(
        runId: String,
        callId: String,
        status: String,
        result: String?,
        errorMessage: String?,
        updatedAt: Long,
    )

    @Query("SELECT * FROM agent_tool_calls WHERE agentRunId = :runId ORDER BY createdAt ASC")
    fun observeToolCalls(runId: String): Flow<List<AgentToolCallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPendingCall(call: PendingToolCallEntity)

    @Query(
        """
        UPDATE pending_tool_calls
        SET status = :status, updatedAt = :updatedAt
        WHERE id = :callId AND agentRunId = :runId
        """,
    )
    suspend fun updatePendingCallStatus(runId: String, callId: String, status: String, updatedAt: Long)

    @Query("SELECT * FROM pending_tool_calls WHERE agentRunId = :runId ORDER BY createdAt ASC")
    fun observePendingCalls(runId: String): Flow<List<PendingToolCallEntity>>
}
