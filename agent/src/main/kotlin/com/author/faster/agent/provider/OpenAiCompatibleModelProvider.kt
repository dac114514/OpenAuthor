package com.author.faster.agent.provider

import com.author.faster.agent.runtime.AgentMessage
import com.author.faster.agent.runtime.ModelProvider
import com.author.faster.agent.runtime.ModelResponse
import com.author.faster.agent.runtime.ToolCall
import com.author.faster.agent.runtime.ToolDefinition
import com.author.faster.agent.runtime.ToolRiskLevel
import com.author.faster.core.model.ModelConfig
import com.author.faster.core.model.ModelProtocol
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.http.client.okhttp.OkHttpClient
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.openai.OpenAiChatModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class OpenAiCompatibleModelProvider private constructor(
    private val chatModel: ChatModel,
) : ModelProvider {
    override suspend fun complete(
        messages: List<AgentMessage>,
        tools: List<ToolDefinition>,
    ): ModelResponse = withContext(Dispatchers.IO) {
        val request = ChatRequest.builder()
            .messages(messages.toLangChainMessages())
            .toolSpecifications(tools.map(ToolDefinition::toToolSpecification))
            .build()
        val aiMessage = chatModel.chat(request).aiMessage()
        ModelResponse(
            text = aiMessage.text().orEmpty(),
            toolCalls = aiMessage.toolExecutionRequests().map { requestCall ->
                requestCall.toToolCall(tools)
            },
        )
    }

    companion object {
        fun create(
            config: ModelConfig,
            apiKey: String,
        ): OpenAiCompatibleModelProvider {
            require(config.protocol == ModelProtocol.OPENAI_COMPATIBLE) {
                "仅 OpenAI Compatible 配置可创建此 Provider"
            }
            require(apiKey.isNotBlank()) { "API Key 不能为空" }

            val builder = OpenAiChatModel.builder()
                .httpClientBuilder(OkHttpClient.builder())
                .baseUrl(config.baseUrl)
                .apiKey(apiKey)
                .modelName(config.modelId)
                .temperature(config.temperature)
                .maxTokens(config.maxOutputTokens)
                .maxRetries(0)
            val headers = parseHeaders(config.extraHeadersJson)
            if (headers.isNotEmpty()) builder.customHeaders(headers)
            return OpenAiCompatibleModelProvider(builder.build())
        }

        internal fun fromChatModel(chatModel: ChatModel) = OpenAiCompatibleModelProvider(chatModel)

        private fun parseHeaders(json: String): Map<String, String> {
            val element = Json.parseToJsonElement(json)
            val objectValue = element as? JsonObject ?: error("额外请求头必须是 JSON 对象")
            return objectValue.mapValues { (name, value) ->
                val primitive = value as? JsonPrimitive
                    ?: error("请求头 $name 的值必须是字符串")
                require(primitive.isString) { "请求头 $name 的值必须是字符串" }
                primitive.content
            }
        }
    }
}

private fun List<AgentMessage>.toLangChainMessages(): List<ChatMessage> {
    val toolNames = asSequence()
        .filterIsInstance<AgentMessage.Assistant>()
        .flatMap { it.toolCalls.asSequence() }
        .associate { it.id to it.name }

    return map { message ->
        when (message) {
            is AgentMessage.System -> SystemMessage.from(message.content)
            is AgentMessage.User -> UserMessage.from(message.content)
            is AgentMessage.Assistant -> AiMessage.from(
                message.content,
                message.toolCalls.map(ToolCall::toToolExecutionRequest),
            )
            is AgentMessage.Tool -> ToolExecutionResultMessage.builder()
                .id(message.toolCallId)
                .toolName(toolNames[message.toolCallId] ?: "unknown_tool")
                .text(message.content)
                .isError(message.isError)
                .build()
        }
    }
}

private fun ToolCall.toToolExecutionRequest(): ToolExecutionRequest = ToolExecutionRequest.builder()
    .id(id)
    .name(name)
    .arguments(arguments.toString())
    .build()

private fun ToolDefinition.toToolSpecification(): ToolSpecification {
    val json = buildJsonObject {
        put("name", name)
        put("description", description)
        put("parameters", inputSchema)
    }
    return ToolSpecification.fromJson(json.toString())
}

private fun ToolExecutionRequest.toToolCall(definitions: List<ToolDefinition>): ToolCall {
    val parsedArguments = runCatching { Json.parseToJsonElement(arguments()).jsonObject }
    val definition = definitions.singleOrNull { it.name == name() }
    return ToolCall(
        id = id(),
        name = name(),
        arguments = parsedArguments.getOrDefault(JsonObject(emptyMap())),
        riskLevel = definition?.riskLevel ?: ToolRiskLevel.SYSTEM,
        argumentError = parsedArguments.exceptionOrNull()?.message,
    )
}
