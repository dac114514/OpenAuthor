package com.author.faster.agent.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class AgentRuntime(
    private val modelProvider: ModelProvider,
    private val toolExecutor: ToolExecutor,
    private val pendingToolCallStore: PendingToolCallStore,
    private val runStore: AgentRunStore = NoOpAgentRunStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun run(
        session: AgentSession,
        policy: AgentRunPolicy = AgentRunPolicy(),
    ): AgentResult {
        validatePolicy(policy)
        runStore.startRun(session, clock())
        return execute(session, policy)
    }

    suspend fun resumeAfterConfirmation(
        session: AgentSession,
        call: ToolCall,
        approved: Boolean,
        policy: AgentRunPolicy = AgentRunPolicy(),
    ): AgentResult {
        validatePolicy(policy)
        pendingToolCallStore.resolve(
            sessionId = session.id,
            callId = call.id,
            status = if (approved) PendingToolCallStatus.APPROVED else PendingToolCallStatus.REJECTED,
        )
        runStore.updateRun(session.id, AgentRunStatus.RUNNING, 0, null, clock())

        val definition = session.availableTools.singleOrNull { it.name == call.name }
            ?: return finish(
                session.id,
                RunCompletion.failed("待执行工具不存在或已不可用：${call.name}"),
            )
        val normalizedCall = call.copy(riskLevel = definition.riskLevel)
        val validation = ToolArgumentValidator.validate(normalizedCall.arguments, definition.inputSchema)
        if (!validation.isValid) {
            return finish(
                session.id,
                RunCompletion.failed("工具参数无效：${validation.errors.joinToString("；")}"),
            )
        }

        return executeWithTimeout(session.id, policy) {
            val messages = session.messages.toMutableList()
            messages += AgentMessage.Assistant(content = "", toolCalls = listOf(normalizedCall))
            messages += if (approved) {
                executeTool(session.id, normalizedCall, definition, policy).toMessage(normalizedCall.id)
            } else {
                AgentMessage.Tool(
                    content = "用户拒绝了该写操作，未修改任何数据。",
                    toolCallId = normalizedCall.id,
                    isError = true,
                )
            }
            runWithinTimeout(session.copy(messages = messages), policy)
        }
    }

    private suspend fun execute(
        session: AgentSession,
        policy: AgentRunPolicy,
    ): AgentResult = executeWithTimeout(session.id, policy) {
        runWithinTimeout(session, policy)
    }

    private suspend fun executeWithTimeout(
        sessionId: String,
        policy: AgentRunPolicy,
        block: suspend () -> RunCompletion,
    ): AgentResult = try {
        finish(sessionId, withTimeout(policy.timeoutMillis) { block() })
    } catch (_: TimeoutCancellationException) {
        finish(sessionId, RunCompletion(AgentResult.TimedOut, AgentRunStatus.TIMED_OUT, 0, "Agent 运行超时"))
    } catch (cancellation: CancellationException) {
        runStore.updateRun(
            sessionId,
            AgentRunStatus.CANCELLED,
            0,
            "Agent 运行已取消",
            clock(),
        )
        throw cancellation
    } catch (error: Exception) {
        finish(
            sessionId,
            RunCompletion.failed("Agent 运行失败：${error.message ?: error::class.simpleName.orEmpty()}"),
        )
    }

    private suspend fun runWithinTimeout(
        session: AgentSession,
        policy: AgentRunPolicy,
    ): RunCompletion {
        val messages = session.messages.toMutableList()
        var continuousToolCalls = 0

        repeat(policy.maxSteps) { stepIndex ->
            val stepCount = stepIndex + 1
            runStore.updateRun(session.id, AgentRunStatus.RUNNING, stepCount, null, clock())
            val response = completeWithRetry(messages, session.availableTools, policy)
            if (response.toolCalls.isEmpty()) {
                return RunCompletion(
                    result = AgentResult.Completed(response.text),
                    status = AgentRunStatus.COMPLETED,
                    stepCount = stepCount,
                    message = response.text,
                )
            }

            messages += AgentMessage.Assistant(response.text, response.toolCalls)

            for (rawCall in response.toolCalls) {
                continuousToolCalls += 1
                if (continuousToolCalls > policy.maxContinuousToolCalls) {
                    return RunCompletion(
                        AgentResult.ToolCallLimitReached,
                        AgentRunStatus.TOOL_CALL_LIMIT_REACHED,
                        stepCount,
                        "已达到连续工具调用上限",
                    )
                }

                val definition = session.availableTools.singleOrNull { it.name == rawCall.name }
                if (definition == null) {
                    messages += recordRejectedCall(
                        sessionId = session.id,
                        call = rawCall,
                        error = "模型请求了未授权工具：${rawCall.name}",
                    )
                    continue
                }

                val call = rawCall.copy(riskLevel = definition.riskLevel)
                if (call.argumentError != null) {
                    messages += recordRejectedCall(
                        sessionId = session.id,
                        call = call,
                        error = "工具参数不是有效的 JSON 对象：${call.argumentError}",
                    )
                    continue
                }
                val validation = ToolArgumentValidator.validate(call.arguments, definition.inputSchema)
                if (!validation.isValid) {
                    messages += recordRejectedCall(
                        sessionId = session.id,
                        call = call,
                        error = "工具参数无效：${validation.errors.joinToString("；")}",
                    )
                    continue
                }

                if (session.mode == AgentExecutionMode.MANUAL && definition.riskLevel != ToolRiskLevel.READ) {
                    pendingToolCallStore.save(session.id, call)
                    return RunCompletion(
                        AgentResult.WaitingConfirmation(call),
                        AgentRunStatus.WAITING_CONFIRMATION,
                        stepCount,
                        "等待用户确认：${definition.description}",
                    )
                }

                messages += executeTool(session.id, call, definition, policy).toMessage(call.id)
            }
        }

        return RunCompletion(
            AgentResult.StepLimitReached,
            AgentRunStatus.STEP_LIMIT_REACHED,
            policy.maxSteps,
            "已达到 Agent 最大步数",
        )
    }

    private suspend fun completeWithRetry(
        messages: List<AgentMessage>,
        tools: List<ToolDefinition>,
        policy: AgentRunPolicy,
    ): ModelResponse {
        var lastError: Exception? = null
        repeat(policy.maxModelRetries + 1) {
            try {
                return modelProvider.complete(messages, tools)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("模型调用失败")
    }

    private suspend fun executeTool(
        sessionId: String,
        call: ToolCall,
        definition: ToolDefinition,
        policy: AgentRunPolicy,
    ): ToolExecutionResult {
        val maxAttempts = if (definition.retryable) policy.maxToolRetries + 1 else 1
        var lastResult: ToolExecutionResult? = null

        repeat(maxAttempts) { attemptIndex ->
            val attempt = attemptIndex + 1
            runStore.startToolCall(sessionId, call, attempt, clock())
            val result = try {
                toolExecutor.execute(call)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                ToolExecutionResult(
                    content = "工具执行失败：${error.message ?: error::class.simpleName.orEmpty()}",
                    isError = true,
                )
            }
            lastResult = result
            runStore.finishToolCall(
                sessionId = sessionId,
                callId = call.id,
                status = if (result.isError) ToolCallStatus.FAILED else ToolCallStatus.COMPLETED,
                result = result.content.takeUnless { result.isError },
                errorMessage = result.content.takeIf { result.isError },
                finishedAt = clock(),
            )
            if (!result.isError) return result
        }

        return lastResult ?: ToolExecutionResult("工具执行失败", isError = true)
    }

    private suspend fun recordRejectedCall(
        sessionId: String,
        call: ToolCall,
        error: String,
    ): AgentMessage.Tool {
        runStore.startToolCall(sessionId, call, attempt = 1, startedAt = clock())
        runStore.finishToolCall(
            sessionId = sessionId,
            callId = call.id,
            status = ToolCallStatus.FAILED,
            result = null,
            errorMessage = error,
            finishedAt = clock(),
        )
        return AgentMessage.Tool(error, call.id, isError = true)
    }

    private suspend fun finish(sessionId: String, completion: RunCompletion): AgentResult {
        runStore.updateRun(
            sessionId = sessionId,
            status = completion.status,
            stepCount = completion.stepCount,
            message = completion.message,
            updatedAt = clock(),
        )
        return completion.result
    }

    private fun validatePolicy(policy: AgentRunPolicy) {
        require(policy.maxSteps > 0) { "maxSteps 必须大于 0" }
        require(policy.maxContinuousToolCalls > 0) { "maxContinuousToolCalls 必须大于 0" }
        require(policy.timeoutMillis > 0) { "timeoutMillis 必须大于 0" }
        require(policy.maxModelRetries >= 0) { "maxModelRetries 不能小于 0" }
        require(policy.maxToolRetries >= 0) { "maxToolRetries 不能小于 0" }
    }

    private fun ToolExecutionResult.toMessage(callId: String) = AgentMessage.Tool(
        content = content,
        toolCallId = callId,
        isError = isError,
    )

    private data class RunCompletion(
        val result: AgentResult,
        val status: AgentRunStatus,
        val stepCount: Int,
        val message: String?,
    ) {
        companion object {
            fun failed(message: String) = RunCompletion(
                result = AgentResult.Failed(message),
                status = AgentRunStatus.FAILED,
                stepCount = 0,
                message = message,
            )
        }
    }
}
