package com.author.faster.core.validation

data class ProjectInput(
    val title: String,
    val author: String,
    val creativePremise: String,
    val targetChapterCount: Int,
    val defaultWordsPerChapter: Int,
)

data class ModelConfigInput(
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val maxContextTokens: Int,
    val maxOutputTokens: Int,
    val temperature: Double,
)

object ProjectInputValidator {
    fun validateProject(input: ProjectInput): List<String> = buildList {
        if (input.title.isBlank()) add("请填写书名")
        if (input.author.isBlank()) add("请填写作者")
        if (input.creativePremise.isBlank()) add("请填写核心创意")
        if (input.targetChapterCount !in 1..10_000) add("目标章节数应为 1–10000")
        if (input.defaultWordsPerChapter !in 100..100_000) add("每章字数应为 100–100000")
    }

    fun validateModel(input: ModelConfigInput): List<String> = buildList {
        if (input.name.isBlank()) add("请填写配置名称")
        if (input.baseUrl.isBlank() || !input.baseUrl.startsWith("https://")) add("Base URL 必须使用 HTTPS")
        if (input.apiKey.isBlank()) add("请填写 API Key")
        if (input.modelId.isBlank()) add("请填写模型 ID")
        if (input.maxContextTokens !in 1..10_000_000) add("最大上下文必须大于 0")
        if (input.maxOutputTokens !in 1..1_000_000) add("最大输出必须大于 0")
        if (input.temperature !in 0.0..2.0) add("温度应为 0–2")
    }
}

