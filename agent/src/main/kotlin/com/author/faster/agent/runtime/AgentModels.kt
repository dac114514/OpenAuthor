package com.author.faster.agent.runtime

import kotlinx.serialization.json.JsonObject

enum class AgentExecutionMode {
    MANUAL,
    SOLO,
}

enum class ToolRiskLevel {
    READ,
    CREATE,
    UPDATE,
    DELETE,
    BULK_UPDATE,
    SYSTEM,
}

data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
    val riskLevel: ToolRiskLevel,
    val retryable: Boolean = riskLevel == ToolRiskLevel.READ,
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: JsonObject,
    val riskLevel: ToolRiskLevel,
    val argumentError: String? = null,
)

sealed interface AgentMessage {
    val content: String

    data class System(override val content: String) : AgentMessage
    data class User(override val content: String) : AgentMessage
    data class Assistant(
        override val content: String,
        val toolCalls: List<ToolCall> = emptyList(),
    ) : AgentMessage

    data class Tool(
        override val content: String,
        val toolCallId: String,
        val isError: Boolean,
    ) : AgentMessage
}

data class ModelResponse(
    val text: String,
    val toolCalls: List<ToolCall> = emptyList(),
)

data class ToolExecutionResult(
    val content: String,
    val isError: Boolean = false,
)

data class AgentSession(
    val id: String,
    val mode: AgentExecutionMode,
    val messages: List<AgentMessage>,
    val availableTools: List<ToolDefinition>,
    val projectId: String? = null,
    val agentType: String = "general",
)

data class AgentRunPolicy(
    val maxSteps: Int = 12,
    val maxContinuousToolCalls: Int = 24,
    val timeoutMillis: Long = 120_000,
    val maxModelRetries: Int = 2,
    val maxToolRetries: Int = 1,
)

enum class AgentRunStatus {
    RUNNING,
    WAITING_CONFIRMATION,
    COMPLETED,
    STEP_LIMIT_REACHED,
    TOOL_CALL_LIMIT_REACHED,
    TIMED_OUT,
    CANCELLED,
    FAILED,
}

enum class ToolCallStatus {
    RUNNING,
    COMPLETED,
    FAILED,
}

enum class PendingToolCallStatus {
    PENDING,
    APPROVED,
    REJECTED,
}

sealed interface AgentResult {
    data class Completed(val text: String) : AgentResult
    data class WaitingConfirmation(val toolCall: ToolCall) : AgentResult
    data class Failed(val message: String) : AgentResult
    data object StepLimitReached : AgentResult
    data object ToolCallLimitReached : AgentResult
    data object TimedOut : AgentResult
}

fun interface ModelProvider {
    suspend fun complete(
        messages: List<AgentMessage>,
        tools: List<ToolDefinition>,
    ): ModelResponse
}

fun interface ToolExecutor {
    suspend fun execute(call: ToolCall): ToolExecutionResult
}

fun interface PendingToolCallStore {
    suspend fun save(sessionId: String, call: ToolCall)

    suspend fun resolve(
        sessionId: String,
        callId: String,
        status: PendingToolCallStatus,
    ) = Unit
}

interface AgentRunStore {
    suspend fun startRun(session: AgentSession, startedAt: Long)

    suspend fun updateRun(
        sessionId: String,
        status: AgentRunStatus,
        stepCount: Int,
        message: String?,
        updatedAt: Long,
    )

    suspend fun startToolCall(
        sessionId: String,
        call: ToolCall,
        attempt: Int,
        startedAt: Long,
    )

    suspend fun finishToolCall(
        sessionId: String,
        callId: String,
        status: ToolCallStatus,
        result: String?,
        errorMessage: String?,
        finishedAt: Long,
    )
}

object NoOpAgentRunStore : AgentRunStore {
    override suspend fun startRun(session: AgentSession, startedAt: Long) = Unit

    override suspend fun updateRun(
        sessionId: String,
        status: AgentRunStatus,
        stepCount: Int,
        message: String?,
        updatedAt: Long,
    ) = Unit

    override suspend fun startToolCall(
        sessionId: String,
        call: ToolCall,
        attempt: Int,
        startedAt: Long,
    ) = Unit

    override suspend fun finishToolCall(
        sessionId: String,
        callId: String,
        status: ToolCallStatus,
        result: String?,
        errorMessage: String?,
        finishedAt: Long,
    ) = Unit
}
