package com.author.faster.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.author.faster.core.model.ModelProtocol

data class ProjectCreationDraft(
    val title: String = "",
    val author: String = "",
    val displaySummary: String = "",
    val creativePremise: String = "",
    val targetChapterCount: String = "200",
    val defaultWordsPerChapter: String = "3000",
    val genre: String = "",
    val pov: String = "第三人称限知",
    val modelConfigName: String = "默认创作模型",
    val protocol: ModelProtocol = ModelProtocol.OPENAI_COMPATIBLE,
    val baseUrl: String = ModelProtocol.OPENAI_COMPATIBLE.defaultBaseUrl,
    val apiKey: String = "",
    val modelId: String = "",
    val maxContextTokens: String = "128000",
    val maxOutputTokens: String = "8192",
    val temperature: String = "0.8",
    val supportsToolCalling: Boolean = true,
    val supportsStreamingToolCalling: Boolean = true,
    val extraHeadersJson: String = "{}",
)

data class ProjectCreationUiState(
    val isOpen: Boolean = false,
    val step: Int = 0,
    val draft: ProjectCreationDraft = ProjectCreationDraft(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
)

sealed interface ProjectCreationIntent {
    data object Open : ProjectCreationIntent
    data object Dismiss : ProjectCreationIntent
    data object Next : ProjectCreationIntent
    data object Back : ProjectCreationIntent
    data object Save : ProjectCreationIntent
    data class UpdateDraft(val draft: ProjectCreationDraft) : ProjectCreationIntent
}

@Composable
fun ProjectCreationWizard(
    state: ProjectCreationUiState,
    onIntent: (ProjectCreationIntent) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!state.isSaving) onIntent(ProjectCreationIntent.Dismiss) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text("新建小说项目", style = MaterialTheme.typography.headlineMedium)
            Text(
                listOf("基础信息", "创作计划", "项目模型")[state.step],
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            LinearProgressIndicator(
                progress = { (state.step + 1) / 3f },
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            )

            when (state.step) {
                0 -> BasicInfoStep(state.draft, onIntent)
                1 -> WritingPlanStep(state.draft, onIntent)
                else -> ModelConfigStep(state.draft, onIntent)
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        onIntent(if (state.step == 0) ProjectCreationIntent.Dismiss else ProjectCreationIntent.Back)
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.step == 0) "取消" else "上一步")
                }
                Button(
                    onClick = {
                        onIntent(if (state.step == 2) ProjectCreationIntent.Save else ProjectCreationIntent.Next)
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.isSaving) "正在保存…" else if (state.step == 2) "创建项目" else "下一步")
                }
            }
        }
    }
}

@Composable
private fun BasicInfoStep(draft: ProjectCreationDraft, onIntent: (ProjectCreationIntent) -> Unit) {
    WizardTextField("书名", draft.title) { onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(title = it))) }
    WizardTextField("作者", draft.author) { onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(author = it))) }
    WizardTextField("一句话简介（仅展示）", draft.displaySummary, minLines = 2) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(displaySummary = it)))
    }
    WizardTextField("核心创意", draft.creativePremise, minLines = 3) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(creativePremise = it)))
    }
}

@Composable
private fun WritingPlanStep(draft: ProjectCreationDraft, onIntent: (ProjectCreationIntent) -> Unit) {
    WizardTextField("目标章节数", draft.targetChapterCount, KeyboardType.Number) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(targetChapterCount = it)))
    }
    WizardTextField("默认每章字数", draft.defaultWordsPerChapter, KeyboardType.Number) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(defaultWordsPerChapter = it)))
    }
    WizardTextField("小说类型", draft.genre) { onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(genre = it))) }
    WizardTextField("叙事视角", draft.pov) { onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(pov = it))) }
}

@Composable
private fun ModelConfigStep(draft: ProjectCreationDraft, onIntent: (ProjectCreationIntent) -> Unit) {
    Text("协议", style = MaterialTheme.typography.titleMedium)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        ModelProtocol.entries.forEach { protocol ->
            FilterChip(
                selected = draft.protocol == protocol,
                onClick = {
                    onIntent(
                        ProjectCreationIntent.UpdateDraft(
                            draft.copy(protocol = protocol, baseUrl = protocol.defaultBaseUrl),
                        ),
                    )
                },
                label = { Text(protocol.displayName) },
            )
        }
    }
    WizardTextField("配置名称", draft.modelConfigName) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(modelConfigName = it)))
    }
    WizardTextField("Base URL", draft.baseUrl) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(baseUrl = it)))
    }
    OutlinedTextField(
        value = draft.apiKey,
        onValueChange = { onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(apiKey = it))) },
        label = { Text("API Key") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
    WizardTextField("模型 ID", draft.modelId) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(modelId = it)))
    }
    WizardTextField("最大上下文", draft.maxContextTokens, KeyboardType.Number) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(maxContextTokens = it)))
    }
    WizardTextField("最大输出", draft.maxOutputTokens, KeyboardType.Number) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(maxOutputTokens = it)))
    }
    WizardTextField("温度（0–2）", draft.temperature, KeyboardType.Decimal) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(temperature = it)))
    }
    WizardTextField("额外请求头（JSON）", draft.extraHeadersJson, minLines = 2) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(extraHeadersJson = it)))
    }
    ToggleRow("支持工具调用", draft.supportsToolCalling) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(supportsToolCalling = it)))
    }
    ToggleRow("支持流式工具调用", draft.supportsStreamingToolCalling) {
        onIntent(ProjectCreationIntent.UpdateDraft(draft.copy(supportsStreamingToolCalling = it)))
    }
}

@Composable
private fun WizardTextField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        minLines = minLines,
        singleLine = minLines == 1,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.padding(top = 12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true)
@Composable
private fun ProjectCreationWizardPreview() {
    MaterialTheme {
        ProjectCreationWizard(
            state = ProjectCreationUiState(isOpen = true, step = 2),
            onIntent = {},
        )
    }
}

