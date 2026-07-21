package com.author.faster.runtime

import com.author.faster.agent.runtime.AgentRunStatus
import com.author.faster.agent.runtime.AgentRunStore
import com.author.faster.agent.runtime.AgentSession
import com.author.faster.agent.runtime.PendingToolCallStatus
import com.author.faster.agent.runtime.PendingToolCallStore
import com.author.faster.agent.runtime.ToolCall
import com.author.faster.agent.runtime.ToolCallStatus
import com.author.faster.data.local.AgentRunDao
import com.author.faster.data.local.AgentRunEntity
import com.author.faster.data.local.AgentToolCallEntity
import com.author.faster.data.local.PendingToolCallEntity

class RoomAgentRuntimeStore(
    private val dao: AgentRunDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : AgentRunStore, PendingToolCallStore {
    override suspend fun startRun(session: AgentSession, startedAt: Long) {
        dao.upsertRun(
            AgentRunEntity(
                id = session.id,
                projectId = session.projectId,
                agentType = session.agentType,
                executionMode = session.mode.name,
                status = AgentRunStatus.RUNNING.name,
                stepCount = 0,
                message = null,
                startedAt = startedAt,
                updatedAt = startedAt,
            ),
        )
    }

    override suspend fun updateRun(
        sessionId: String,
        status: AgentRunStatus,
        stepCount: Int,
        message: String?,
        updatedAt: Long,
    ) {
        dao.updateRun(sessionId, status.name, stepCount, message, updatedAt)
    }

    override suspend fun startToolCall(
        sessionId: String,
        call: ToolCall,
        attempt: Int,
        startedAt: Long,
    ) {
        dao.insertToolCall(
            AgentToolCallEntity(
                id = call.id,
                agentRunId = sessionId,
                toolName = call.name,
                argumentsJson = call.arguments.toString(),
                riskLevel = call.riskLevel.name,
                status = ToolCallStatus.RUNNING.name,
                attempt = attempt,
                result = null,
                errorMessage = null,
                createdAt = startedAt,
                updatedAt = startedAt,
            ),
        )
        dao.restartToolCall(sessionId, call.id, ToolCallStatus.RUNNING.name, attempt, startedAt)
    }

    override suspend fun finishToolCall(
        sessionId: String,
        callId: String,
        status: ToolCallStatus,
        result: String?,
        errorMessage: String?,
        finishedAt: Long,
    ) {
        dao.finishToolCall(sessionId, callId, status.name, result, errorMessage, finishedAt)
    }

    override suspend fun save(sessionId: String, call: ToolCall) {
        val now = clock()
        dao.upsertPendingCall(
            PendingToolCallEntity(
                id = call.id,
                agentRunId = sessionId,
                toolName = call.name,
                argumentsJson = call.arguments.toString(),
                displayTitle = "确认执行 ${call.name}",
                displayDescription = "风险等级：${call.riskLevel.name}；参数：${call.arguments}",
                riskLevel = call.riskLevel.name,
                status = PendingToolCallStatus.PENDING.name,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun resolve(
        sessionId: String,
        callId: String,
        status: PendingToolCallStatus,
    ) {
        dao.updatePendingCallStatus(sessionId, callId, status.name, clock())
    }
}
