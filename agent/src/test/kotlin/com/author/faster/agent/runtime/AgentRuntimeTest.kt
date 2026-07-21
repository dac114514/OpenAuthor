package com.author.faster.agent.runtime

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRuntimeTest {
    private val writeCall = ToolCall(
        id = "call-1",
        name = "create_character",
        arguments = buildJsonObject {},
        riskLevel = ToolRiskLevel.CREATE,
    )

    @Test
    fun soloModeReturnsToolResultToModelAndContinues() = runBlocking {
        val observedMessages = mutableListOf<List<AgentMessage>>()
        var requestCount = 0
        val runtime = AgentRuntime(
            modelProvider = ModelProvider { messages, _ ->
                observedMessages += messages
                requestCount += 1
                if (requestCount == 1) {
                    ModelResponse(text = "", toolCalls = listOf(writeCall))
                } else {
                    ModelResponse(text = "人物已创建")
                }
            },
            toolExecutor = ToolExecutor { ToolExecutionResult("真实工具结果") },
            pendingToolCallStore = PendingToolCallStore { _, _ -> error("Solo mode must not queue") },
        )

        val result = runtime.run(session(AgentExecutionMode.SOLO))

        assertEquals(AgentResult.Completed("人物已创建"), result)
        assertTrue(observedMessages.last().any { it is AgentMessage.Tool && it.content == "真实工具结果" })
    }

    @Test
    fun manualModeQueuesWriteAndWaitsForConfirmation() = runBlocking {
        var pendingCall: ToolCall? = null
        val runtime = AgentRuntime(
            modelProvider = ModelProvider { _, _ -> ModelResponse("", listOf(writeCall)) },
            toolExecutor = ToolExecutor { error("Write must not run before confirmation") },
            pendingToolCallStore = PendingToolCallStore { _, call -> pendingCall = call },
        )

        val result = runtime.run(session(AgentExecutionMode.MANUAL))

        assertEquals(AgentResult.WaitingConfirmation(writeCall), result)
        assertEquals(writeCall, pendingCall)
    }

    @Test
    fun runtimeStopsAtConfiguredStepLimit() = runBlocking {
        val readCall = writeCall.copy(name = "get_character", riskLevel = ToolRiskLevel.READ)
        val runtime = AgentRuntime(
            modelProvider = ModelProvider { _, _ -> ModelResponse("", listOf(readCall)) },
            toolExecutor = ToolExecutor { ToolExecutionResult("ok") },
            pendingToolCallStore = PendingToolCallStore { _, _ -> },
        )

        val result = runtime.run(
            session = session(AgentExecutionMode.SOLO),
            policy = AgentRunPolicy(maxSteps = 2),
        )

        assertEquals(AgentResult.StepLimitReached, result)
    }

    private fun session(mode: AgentExecutionMode) = AgentSession(
        id = "session-1",
        mode = mode,
        messages = listOf(AgentMessage.User("创建一名人物")),
        availableTools = emptyList(),
    )
}

