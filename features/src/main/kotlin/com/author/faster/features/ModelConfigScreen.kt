package com.author.faster.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class ModelConfigCardState(
    val id: String,
    val name: String,
    val protocol: String,
    val baseUrl: String,
    val modelId: String,
    val maxContextTokens: Int,
    val maxOutputTokens: Int,
    val supportsToolCalling: Boolean,
    val supportsStreamingToolCalling: Boolean,
    val hasApiKey: Boolean,
)

data class ModelConfigsUiState(
    val configs: List<ModelConfigCardState> = emptyList(),
)

@Composable
fun ModelConfigScreen(
    state: ModelConfigsUiState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("模型配置", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(
                "每个项目只绑定一个模型；更换配置只影响后续生成。",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp)) {
                    Icon(Icons.Rounded.Security, contentDescription = null)
                    Text(
                        "API Key 由 Android Keystore 加密，仅保存在本机，不进入项目导出包。",
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }

        if (state.configs.isEmpty()) {
            item {
                Card(shape = MaterialTheme.shapes.extraLarge) {
                    Column(Modifier.fillMaxWidth().padding(24.dp)) {
                        Icon(Icons.Rounded.Key, contentDescription = null)
                        Spacer(Modifier.height(16.dp))
                        Text("暂无模型配置", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "通过新建项目向导添加第一个模型配置。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(state.configs, key = ModelConfigCardState::id) { config ->
                ModelConfigCard(config)
            }
        }
    }
}

@Composable
private fun ModelConfigCard(config: ModelConfigCardState) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(config.name, style = MaterialTheme.typography.headlineSmall)
                Text(config.protocol, color = MaterialTheme.colorScheme.primary)
            }
            Text(config.modelId, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold)
            Text(config.baseUrl, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("上下文 ${config.maxContextTokens} · 输出 ${config.maxOutputTokens}")
            Text(
                buildString {
                    append(if (config.supportsToolCalling) "支持工具调用" else "不支持工具调用")
                    if (config.supportsStreamingToolCalling) append(" · 支持流式工具")
                    append(if (config.hasApiKey) " · Key 已保存" else " · Key 缺失")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelConfigScreenPreview() {
    MaterialTheme {
        ModelConfigScreen(
            ModelConfigsUiState(
                listOf(
                    ModelConfigCardState(
                        id = "preview",
                        name = "创作模型",
                        protocol = "OpenAI Compatible",
                        baseUrl = "https://api.example.com/v1",
                        modelId = "novel-model",
                        maxContextTokens = 128000,
                        maxOutputTokens = 8192,
                        supportsToolCalling = true,
                        supportsStreamingToolCalling = true,
                        hasApiKey = true,
                    ),
                ),
            ),
        )
    }
}

