package com.author.faster.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class WorldCategoryCardState(
    val id: String,
    val name: String,
    val description: String,
    val fieldSchemaJson: String,
    val isBuiltIn: Boolean,
    val entryCount: Int,
)

data class WorldEntryCardState(
    val id: String,
    val categoryId: String,
    val title: String,
    val summary: String,
    val content: String,
    val structuredFieldsJson: String,
)

data class WorldbuildingUiState(
    val categories: List<WorldCategoryCardState> = emptyList(),
    val entries: List<WorldEntryCardState> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val agentMessages: List<WorldAgentMessageState> = emptyList(),
    val isAgentRunning: Boolean = false,
    val pendingToolCall: PendingWorldToolCallState? = null,
)

enum class WorldAgentMode { MANUAL, SOLO }

data class WorldAgentMessageState(
    val id: String,
    val isUser: Boolean,
    val content: String,
    val isError: Boolean = false,
)

data class PendingWorldToolCallState(
    val id: String,
    val title: String,
    val description: String,
)

data class WorldCategoryDraft(
    val name: String = "",
    val description: String = "",
    val fieldSchemaJson: String = "{}",
)

data class WorldEntryDraft(
    val id: String? = null,
    val categoryId: String = "",
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    val structuredFieldsJson: String = "{}",
)

@Composable
fun WorldbuildingScreen(
    state: WorldbuildingUiState,
    onCreateCategory: (WorldCategoryDraft) -> Unit,
    onSaveEntry: (WorldEntryDraft) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onRunAgent: (String, WorldAgentMode) -> Unit = { _, _ -> },
    onResolvePendingTool: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<WorldEntryDraft?>(null) }
    var deletingEntry by remember { mutableStateOf<WorldEntryCardState?>(null) }
    var agentPrompt by rememberSaveable { mutableStateOf("") }
    var agentMode by rememberSaveable { mutableStateOf(WorldAgentMode.MANUAL) }

    LaunchedEffect(state.categories, selectedCategoryId) {
        if (state.categories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = state.categories.firstOrNull()?.id
        }
    }

    val visibleEntries = state.entries.filter { it.categoryId == selectedCategoryId }
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("世界观", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "按分类维护长期设定；AI 的写操作会遵循 Manual / Solo 规则。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.errorMessage?.let { message ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            message,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
            item {
                WorldAgentCard(
                    messages = state.agentMessages,
                    prompt = agentPrompt,
                    mode = agentMode,
                    isRunning = state.isAgentRunning,
                    pendingToolCall = state.pendingToolCall,
                    onPromptChange = { agentPrompt = it },
                    onModeChange = { agentMode = it },
                    onSend = {
                        onRunAgent(agentPrompt, agentMode)
                        agentPrompt = ""
                    },
                    onResolvePendingTool = onResolvePendingTool,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("分类", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { showCategoryDialog = true }) {
                        Icon(Icons.Rounded.Category, contentDescription = null)
                        Text("自定义分类", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.categories, key = WorldCategoryCardState::id) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text("${category.name} ${category.entryCount}") },
                        )
                    }
                }
            }
            state.categories.firstOrNull { it.id == selectedCategoryId }?.let { category ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text(category.name, style = MaterialTheme.typography.titleMedium)
                            Text(category.description, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            if (category.isBuiltIn) {
                                Text(
                                    "内置分类",
                                    modifier = Modifier.padding(top = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
            if (state.categories.isEmpty()) {
                item { EmptyWorldbuildingCard("正在初始化内置世界观分类…") }
            } else if (visibleEntries.isEmpty()) {
                item { EmptyWorldbuildingCard("此分类还没有条目，可手动创建或交给 AI 补充。") }
            } else {
                items(visibleEntries, key = WorldEntryCardState::id) { entry ->
                    WorldEntryCard(
                        entry = entry,
                        onEdit = { editingEntry = entry.toDraft() },
                        onDelete = { deletingEntry = entry },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                selectedCategoryId?.let { editingEntry = WorldEntryDraft(categoryId = it) }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            icon = {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                else Icon(Icons.Rounded.Add, contentDescription = null)
            },
            text = { Text("新建条目") },
            expanded = true,
        )
    }

    if (showCategoryDialog) {
        WorldCategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onSave = {
                onCreateCategory(it)
                showCategoryDialog = false
            },
        )
    }
    editingEntry?.let { draft ->
        WorldEntryDialog(
            initial = draft,
            onDismiss = { editingEntry = null },
            onSave = {
                onSaveEntry(it)
                editingEntry = null
            },
        )
    }
    deletingEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deletingEntry = null },
            title = { Text("删除“${entry.title}”？") },
            text = { Text("此操作会永久删除该世界观条目。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEntry(entry.id)
                        deletingEntry = null
                    },
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletingEntry = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun WorldAgentCard(
    messages: List<WorldAgentMessageState>,
    prompt: String,
    mode: WorldAgentMode,
    isRunning: Boolean,
    pendingToolCall: PendingWorldToolCallState?,
    onPromptChange: (String) -> Unit,
    onModeChange: (WorldAgentMode) -> Unit,
    onSend: () -> Unit,
    onResolvePendingTool: (Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("世界观 Agent", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == WorldAgentMode.MANUAL,
                        onClick = { onModeChange(WorldAgentMode.MANUAL) },
                        label = { Text("Manual") },
                    )
                    FilterChip(
                        selected = mode == WorldAgentMode.SOLO,
                        onClick = { onModeChange(WorldAgentMode.SOLO) },
                        label = { Text("Solo") },
                    )
                }
            }
            messages.takeLast(4).forEach { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            message.isError -> MaterialTheme.colorScheme.errorContainer
                            message.isUser -> MaterialTheme.colorScheme.surfaceContainerHighest
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        },
                    ),
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            if (message.isUser) "你" else "Agent",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(message.content)
                    }
                }
            }
            pendingToolCall?.let { pending ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(pending.title, style = MaterialTheme.typography.titleMedium)
                        Text(pending.description)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onResolvePendingTool(true) },
                                enabled = !isRunning,
                            ) { Text("批准") }
                            TextButton(
                                onClick = { onResolvePendingTool(false) },
                                enabled = !isRunning,
                            ) { Text("拒绝") }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("描述要补充或修改的世界观") },
                    minLines = 2,
                    enabled = !isRunning && pendingToolCall == null,
                )
                IconButton(
                    onClick = onSend,
                    enabled = prompt.isNotBlank() && !isRunning && pendingToolCall == null,
                ) {
                    if (isRunning) CircularProgressIndicator()
                    else Icon(Icons.Rounded.Send, contentDescription = "发送")
                }
            }
        }
    }
}

@Composable
private fun WorldEntryCard(
    entry: WorldEntryCardState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.title, style = MaterialTheme.typography.titleLarge)
                    if (entry.summary.isNotBlank()) {
                        Text(entry.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "编辑") }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "删除") }
            }
            Text(entry.content, modifier = Modifier.padding(top = 12.dp), maxLines = 5)
        }
    }
}

@Composable
private fun EmptyWorldbuildingCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(24.dp))
    }
}

@Composable
private fun WorldCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (WorldCategoryDraft) -> Unit,
) {
    var draft by remember { mutableStateOf(WorldCategoryDraft()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建世界观分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text("分类名称") },
                )
                OutlinedTextField(
                    value = draft.description,
                    onValueChange = { draft = draft.copy(description = it) },
                    label = { Text("用途说明") },
                    minLines = 2,
                )
                OutlinedTextField(
                    value = draft.fieldSchemaJson,
                    onValueChange = { draft = draft.copy(fieldSchemaJson = it) },
                    label = { Text("字段 Schema（JSON）") },
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(draft) }, enabled = draft.name.isNotBlank()) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun WorldEntryDialog(
    initial: WorldEntryDraft,
    onDismiss: () -> Unit,
    onSave: (WorldEntryDraft) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == null) "新建世界观条目" else "编辑世界观条目") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = { draft = draft.copy(title = it) },
                        label = { Text("标题") },
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.summary,
                        onValueChange = { draft = draft.copy(summary = it) },
                        label = { Text("摘要") },
                        minLines = 2,
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.content,
                        onValueChange = { draft = draft.copy(content = it) },
                        label = { Text("完整设定") },
                        minLines = 5,
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.structuredFieldsJson,
                        onValueChange = { draft = draft.copy(structuredFieldsJson = it) },
                        label = { Text("结构化字段（JSON）") },
                        minLines = 3,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draft) },
                enabled = draft.title.isNotBlank() && draft.content.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun WorldEntryCardState.toDraft() = WorldEntryDraft(
    id = id,
    categoryId = categoryId,
    title = title,
    summary = summary,
    content = content,
    structuredFieldsJson = structuredFieldsJson,
)

@Preview(showBackground = true)
@Composable
private fun WorldbuildingPreview() {
    MaterialTheme {
        WorldbuildingScreen(
            state = WorldbuildingUiState(
                categories = listOf(
                    WorldCategoryCardState(
                        id = "foundation",
                        name = "世界基础",
                        description = "世界规则、时代背景与基础约束",
                        fieldSchemaJson = "{}",
                        isBuiltIn = true,
                        entryCount = 1,
                    ),
                ),
                entries = listOf(
                    WorldEntryCardState(
                        id = "entry",
                        categoryId = "foundation",
                        title = "潮汐历",
                        summary = "群星潮汐决定航行窗口",
                        content = "每隔九十七天，星潮会让远距离跃迁短暂稳定。",
                        structuredFieldsJson = "{}",
                    ),
                ),
            ),
            onCreateCategory = {},
            onSaveEntry = {},
            onDeleteEntry = {},
            onRunAgent = { _, _ -> },
            onResolvePendingTool = {},
        )
    }
}
