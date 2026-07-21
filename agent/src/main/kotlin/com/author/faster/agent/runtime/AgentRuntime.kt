package com.author.faster.agent.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class AgentRuntime(
    private val modelProvider: ModelProvider,
    private val toolExecutor: ToolExecutor,
    private val pendingToolCallStore: PendingToolCallStore,
) {
    suspend fun run(
        session: AgentSession,
        policy: AgentRunPolicy = AgentRunPolicy(),
    ): AgentResult = try {
        withTimeout(policy.timeoutMillis) {
            runWithinTimeout(session, policy)
        }
    } catch (_: TimeoutCancellationException) {
        AgentResult.TimedOut
    } catch (cancellation: CancellationException) {
        throw cancellation
    }

    private suspend fun runWithinTimeout(
        session: AgentSession,
        policy: AgentRunPolicy,
    ): AgentResult {
        val messages = session.messages.toMutableList()
        var continuousToolCalls = 0

        repeat(policy.maxSteps) {
            val response = modelProvider.complete(messages, session.availableTools)
            if (response.toolCalls.isEmpty()) {
                return AgentResult.Completed(response.text)
            }

            messages += AgentMessage.Assistant(response.text, response.toolCalls)

            for (call in response.toolCalls) {
                continuousToolCalls += 1
                if (continuousToolCalls > policy.maxContinuousToolCalls) {
                    return AgentResult.ToolCallLimitReached
                }

                if (session.mode == AgentExecutionMode.MANUAL && call.riskLevel != ToolRiskLevel.READ) {
                    pendingToolCallStore.save(session.id, call)
                    return AgentResult.WaitingConfirmation(call)
                }

                val result = toolExecutor.execute(call)
                messages += AgentMessage.Tool(
                    content = result.content,
                    toolCallId = call.id,
                    isError = result.isError,
                )
            }
        }

        return AgentResult.StepLimitReached
    }
}

