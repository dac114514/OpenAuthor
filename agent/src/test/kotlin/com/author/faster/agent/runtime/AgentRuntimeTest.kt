package com.author.faster.agent.runtime

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRuntimeTest {
    private val writeDefinition = ToolDefinition(
        name = "create_character",
        description = "创建人物",
        inputSchema = buildJsonObject { put("type", "object") },
        riskLevel = ToolRiskLevel.CREATE,
    )
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
        val readDefinition = writeDefinition.copy(
            name = readCall.name,
            description = "读取人物",
            riskLevel = ToolRiskLevel.READ,
        )
        val runtime = AgentRuntime(
            modelProvider = ModelProvider { _, _ -> ModelResponse("", listOf(readCall)) },
            toolExecutor = ToolExecutor { ToolExecutionResult("ok") },
            pendingToolCallStore = PendingToolCallStore { _, _ -> },
        )

        val result = runtime.run(
            session = session(AgentExecutionMode.SOLO, listOf(readDefinition)),
            policy = AgentRunPolicy(maxSteps = 2),
        )

        assertEquals(AgentResult.StepLimitReached, result)
    }

    @Test
    fun manualModeUsesRegisteredRiskInsteadOfModelSuppliedRisk() = runBlocking {
        val forgedCall = writeCall.copy(riskLevel = ToolRiskLevel.READ)
        var executed = false
        val runtime = AgentRuntime(
            modelProvider = ModelProvider { _, _ -> ModelResponse("", listOf(forgedCall)) },
            toolExecutor = ToolExecutor {
                executed = true
                ToolExecutionResult("不应执行")
            },
            pendingToolCallStore = PendingToolCallStore { _, _ -> },
        )

        val result = runtime.run(session(AgentExecutionMode.MANUAL))

        assertEquals(AgentResult.WaitingConfirmation(writeCall), result)
        assertEquals(false, executed)
    }

    @Test
    fun invalidArgumentsAreReturnedToModelWithoutExecutingTool() = runBlocking {
        val definition = writeDefinition.copy(
            inputSchema = buildJsonObject {
                put("type", "object")
                put("required", kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive("name")) })
            },
        )
        var requestCount = 0
        var executed = false
        val runtime = AgentRuntime(
            modelProvider = ModelProvider { messages, _ ->
                requestCount += 1
                if (requestCount == 1) {
                    ModelResponse("", listOf(writeCall))
                } else {
                    assertTrue(messages.any { it is AgentMessage.Tool && it.isError })
                    ModelResponse("参数错误已说明")
                }
            },
            toolExecutor = ToolExecutor {
                executed = true
                ToolExecutionResult("不应执行")
            },
            pendingToolCallStore = PendingToolCallStore { _, _ -> },
        )

        val result = runtime.run(session(AgentExecutionMode.SOLO, listOf(definition)))

        assertEquals(AgentResult.Completed("参数错误已说明"), result)
        assertEquals(false, executed)
    }

    @Test
    fun retryableReadToolRetriesOnce() = runBlocking {
        val readCall = writeCall.copy(name = "get_character", riskLevel = ToolRiskLevel.READ)
        val readDefinition = writeDefinition.copy(
            name = readCall.name,
            riskLevel = ToolRiskLevel.READ,
            retryable = true,
        )
        var modelCalls = 0
        var toolCalls = 0
        val runtime = AgentRuntime(
            modelProvider = ModelProvider { _, _ ->
                modelCalls += 1
                if (modelCalls == 1) ModelResponse("", listOf(readCall)) else ModelResponse("完成")
            },
            toolExecutor = ToolExecutor {
                toolCalls += 1
                if (toolCalls == 1) ToolExecutionResult("暂时失败", true) else ToolExecutionResult("读取成功")
            },
            pendingToolCallStore = PendingToolCallStore { _, _ -> },
        )

        val result = runtime.run(
            session(AgentExecutionMode.SOLO, listOf(readDefinition)),
            AgentRunPolicy(maxToolRetries = 1),
        )

        assertEquals(AgentResult.Completed("完成"), result)
        assertEquals(2, toolCalls)
    }

    @Test
    fun approvedPendingCallExecutesAndContinues() = runBlocking {
        var executed = false
        val runtime = AgentRuntime(
            modelProvider = ModelProvider { messages, _ ->
                assertTrue(messages.any { it is AgentMessage.Tool && !it.isError })
                ModelResponse("人物已创建")
            },
            toolExecutor = ToolExecutor {
                executed = true
                ToolExecutionResult("创建成功")
            },
            pendingToolCallStore = PendingToolCallStore { _, _ -> },
        )

        val result = runtime.resumeAfterConfirmation(
            session = session(AgentExecutionMode.MANUAL),
            call = writeCall,
            approved = true,
        )

        assertTrue(executed)
        assertEquals(AgentResult.Completed("人物已创建"), result)
    }

    @Test
    fun modelFailureIsRetriedAndRunStatusIsPersisted() = runBlocking {
        var modelCalls = 0
        val statuses = mutableListOf<AgentRunStatus>()
        val store = object : AgentRunStore {
            override suspend fun startRun(session: AgentSession, startedAt: Long) = Unit

            override suspend fun updateRun(
                sessionId: String,
                status: AgentRunStatus,
                stepCount: Int,
                message: String?,
                updatedAt: Long,
            ) {
                statuses += status
            }

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
        val runtime = AgentRuntime(
            modelProvider = ModelProvider { _, _ ->
                modelCalls += 1
                if (modelCalls == 1) error("临时网络错误")
                ModelResponse("已恢复")
            },
            toolExecutor = ToolExecutor { ToolExecutionResult("ok") },
            pendingToolCallStore = PendingToolCallStore { _, _ -> },
            runStore = store,
        )

        val result = runtime.run(
            session(AgentExecutionMode.SOLO),
            AgentRunPolicy(maxModelRetries = 1),
        )

        assertEquals(2, modelCalls)
        assertEquals(AgentResult.Completed("已恢复"), result)
        assertEquals(AgentRunStatus.COMPLETED, statuses.last())
    }

    private fun session(
        mode: AgentExecutionMode,
        tools: List<ToolDefinition> = listOf(writeDefinition),
    ) = AgentSession(
        id = "session-1",
        mode = mode,
        messages = listOf(AgentMessage.User("创建一名人物")),
        availableTools = tools,
    )
}
