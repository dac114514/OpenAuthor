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
import androidx.compose.material.icons.rounded.Archive
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

enum class CharacterAgentMode { MANUAL, SOLO }

data class CharacterArcCardState(
    val arcType: String = "",
    val initialState: String = "",
    val targetState: String = "",
    val currentStage: String = "",
    val turningPoints: String = "",
    val notes: String = "",
)

data class CharacterCardState(
    val id: String,
    val name: String,
    val aliases: String,
    val gender: String,
    val age: String,
    val identity: String,
    val publicRumors: String,
    val factionId: String?,
    val appearance: String,
    val mentionAllowedFromChapter: Int?,
    val firstAppearanceChapter: Int,
    val importance: String,
    val currentStatus: String,
    val thinkingStyle: String,
    val speechStyle: String,
    val behaviorHabits: String,
    val decisionPattern: String,
    val valuesAndLimits: String,
    val relationshipHandling: String,
    val informationHandling: String,
    val background: String,
    val currentGoal: String,
    val longTermGoal: String,
    val coreDesire: String,
    val coreFear: String,
    val secret: String,
    val abilitiesAndResources: String,
    val weaknesses: String,
    val isArchived: Boolean,
    val arc: CharacterArcCardState? = null,
)

data class CharacterRelationshipCardState(
    val id: String,
    val sourceCharacterId: String,
    val sourceName: String,
    val targetCharacterId: String,
    val targetName: String,
    val type: String,
    val summary: String,
    val hiddenDetails: String,
)

data class CharacterUiState(
    val characters: List<CharacterCardState> = emptyList(),
    val relationships: List<CharacterRelationshipCardState> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val agentMessages: List<WorldAgentMessageState> = emptyList(),
    val isAgentRunning: Boolean = false,
    val pendingToolCall: PendingWorldToolCallState? = null,
)

data class CharacterDraft(
    val id: String? = null,
    val name: String = "",
    val aliases: String = "",
    val gender: String = "",
    val age: String = "",
    val identity: String = "",
    val publicRumors: String = "",
    val factionId: String = "",
    val appearance: String = "",
    val mentionAllowedFromChapter: String = "",
    val firstAppearanceChapter: String = "1",
    val importance: String = "主要",
    val currentStatus: String = "正常",
    val thinkingStyle: String = "",
    val speechStyle: String = "",
    val behaviorHabits: String = "",
    val decisionPattern: String = "",
    val valuesAndLimits: String = "",
    val relationshipHandling: String = "",
    val informationHandling: String = "",
    val background: String = "",
    val currentGoal: String = "",
    val longTermGoal: String = "",
    val coreDesire: String = "",
    val coreFear: String = "",
    val secret: String = "",
    val abilitiesAndResources: String = "",
    val weaknesses: String = "",
    val arcType: String = "",
    val arcInitialState: String = "",
    val arcTargetState: String = "",
    val arcCurrentStage: String = "",
    val arcTurningPoints: String = "",
    val arcNotes: String = "",
)

data class CharacterRelationshipDraft(
    val id: String? = null,
    val sourceCharacterId: String = "",
    val targetCharacterId: String = "",
    val type: String = "",
    val summary: String = "",
    val hiddenDetails: String = "",
)

private enum class CharacterSection { CHARACTERS, RELATIONSHIPS }

@Composable
fun CharacterScreen(
    state: CharacterUiState,
    onSaveCharacter: (CharacterDraft) -> Unit,
    onArchiveCharacter: (String) -> Unit,
    onDeleteCharacter: (String) -> Unit,
    onSaveRelationship: (CharacterRelationshipDraft) -> Unit,
    onRunAgent: (String, CharacterAgentMode) -> Unit = { _, _ -> },
    onResolvePendingTool: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var section by rememberSaveable { mutableStateOf(CharacterSection.CHARACTERS) }
    var editingCharacter by remember { mutableStateOf<CharacterDraft?>(null) }
    var editingRelationship by remember { mutableStateOf<CharacterRelationshipDraft?>(null) }
    var deletingCharacter by remember { mutableStateOf<CharacterCardState?>(null) }
    var prompt by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(CharacterAgentMode.MANUAL) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("人物", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "维护人物卡、关系和弧光；章节上下文会强制执行首次出场过滤。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.errorMessage?.let { message ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(message, modifier = Modifier.fillMaxWidth().padding(16.dp))
                    }
                }
            }
            item {
                CharacterAgentCard(
                    messages = state.agentMessages,
                    prompt = prompt,
                    mode = mode,
                    isRunning = state.isAgentRunning,
                    pendingToolCall = state.pendingToolCall,
                    onPromptChange = { prompt = it },
                    onModeChange = { mode = it },
                    onSend = {
                        onRunAgent(prompt, mode)
                        prompt = ""
                    },
                    onResolvePendingTool = onResolvePendingTool,
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = section == CharacterSection.CHARACTERS,
                            onClick = { section = CharacterSection.CHARACTERS },
                            label = { Text("人物卡 ${state.characters.count { !it.isArchived }}") },
                        )
                    }
                    item {
                        FilterChip(
                            selected = section == CharacterSection.RELATIONSHIPS,
                            onClick = { section = CharacterSection.RELATIONSHIPS },
                            label = { Text("人物关系 ${state.relationships.size}") },
                        )
                    }
                }
            }
            when (section) {
                CharacterSection.CHARACTERS -> {
                    if (state.characters.isEmpty()) item { EmptyCharacterCard("尚未创建人物，可手动创建或交给 AI。") }
                    items(state.characters, key = CharacterCardState::id) { character ->
                        CharacterCard(
                            character = character,
                            onEdit = { editingCharacter = character.toDraft() },
                            onArchive = { onArchiveCharacter(character.id) },
                            onDelete = { deletingCharacter = character },
                        )
                    }
                }
                CharacterSection.RELATIONSHIPS -> {
                    if (state.relationships.isEmpty()) item { EmptyCharacterCard("尚未建立人物关系。") }
                    items(state.relationships, key = CharacterRelationshipCardState::id) { relationship ->
                        RelationshipCard(relationship) { editingRelationship = relationship.toDraft() }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                if (section == CharacterSection.CHARACTERS) editingCharacter = CharacterDraft()
                else editingRelationship = CharacterRelationshipDraft()
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            icon = {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                else Icon(Icons.Rounded.Add, contentDescription = null)
            },
            text = { Text(if (section == CharacterSection.CHARACTERS) "新建人物" else "新建关系") },
        )
    }

    editingCharacter?.let { draft ->
        CharacterDialog(
            initial = draft,
            onDismiss = { editingCharacter = null },
            onSave = {
                onSaveCharacter(it)
                editingCharacter = null
            },
        )
    }
    editingRelationship?.let { draft ->
        RelationshipDialog(
            initial = draft,
            characters = state.characters.filterNot(CharacterCardState::isArchived),
            onDismiss = { editingRelationship = null },
            onSave = {
                onSaveRelationship(it)
                editingRelationship = null
            },
        )
    }
    deletingCharacter?.let { character ->
        AlertDialog(
            onDismissRequest = { deletingCharacter = null },
            title = { Text("删除“${character.name}”？") },
            text = { Text("人物卡、人物关系和人物弧光都会被永久删除。") },
            confirmButton = {
                Button(onClick = {
                    onDeleteCharacter(character.id)
                    deletingCharacter = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletingCharacter = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun CharacterAgentCard(
    messages: List<WorldAgentMessageState>,
    prompt: String,
    mode: CharacterAgentMode,
    isRunning: Boolean,
    pendingToolCall: PendingWorldToolCallState?,
    onPromptChange: (String) -> Unit,
    onModeChange: (CharacterAgentMode) -> Unit,
    onSend: () -> Unit,
    onResolvePendingTool: (Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("人物 Agent", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == CharacterAgentMode.MANUAL,
                        onClick = { onModeChange(CharacterAgentMode.MANUAL) },
                        label = { Text("Manual") },
                    )
                    FilterChip(
                        selected = mode == CharacterAgentMode.SOLO,
                        onClick = { onModeChange(CharacterAgentMode.SOLO) },
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
                        Text(if (message.isUser) "你" else "Agent", fontWeight = FontWeight.SemiBold)
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
                            Button(onClick = { onResolvePendingTool(true) }, enabled = !isRunning) { Text("批准") }
                            TextButton(onClick = { onResolvePendingTool(false) }, enabled = !isRunning) { Text("拒绝") }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("描述要创建或修改的人物") },
                    minLines = 2,
                    enabled = !isRunning && pendingToolCall == null,
                )
                IconButton(
                    onClick = onSend,
                    enabled = prompt.isNotBlank() && !isRunning && pendingToolCall == null,
                ) {
                    if (isRunning) CircularProgressIndicator() else Icon(Icons.Rounded.Send, contentDescription = "发送")
                }
            }
        }
    }
}

@Composable
private fun CharacterCard(
    character: CharacterCardState,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(character.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        listOf(character.identity, character.importance, if (character.isArchived) "已归档" else "使用中")
                            .filter(String::isNotBlank).joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "编辑") }
                if (!character.isArchived) {
                    IconButton(onClick = onArchive) { Icon(Icons.Rounded.Archive, contentDescription = "归档") }
                }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "删除") }
            }
            Text("正式出场：第 ${character.firstAppearanceChapter} 章" +
                (character.mentionAllowedFromChapter?.let { " · 可从第 $it 章提及" } ?: ""))
            if (character.currentGoal.isNotBlank()) Text("当前目标：${character.currentGoal}")
            character.arc?.takeIf { it.currentStage.isNotBlank() || it.arcType.isNotBlank() }?.let { arc ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(
                        "弧光：${arc.arcType.ifBlank { "未分类" }} · ${arc.currentStage.ifBlank { "未开始" }}",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RelationshipCard(relationship: CharacterRelationshipCardState, onEdit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${relationship.sourceName} → ${relationship.targetName}", style = MaterialTheme.typography.titleMedium)
                Text(relationship.type, color = MaterialTheme.colorScheme.primary)
                if (relationship.summary.isNotBlank()) Text(relationship.summary)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "编辑关系") }
        }
    }
}

@Composable
private fun EmptyCharacterCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(24.dp))
    }
}

@Composable
private fun CharacterDialog(
    initial: CharacterDraft,
    onDismiss: () -> Unit,
    onSave: (CharacterDraft) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == null) "新建人物" else "编辑人物") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { DraftField(draft.name, { draft = draft.copy(name = it) }, "姓名") }
                item { DraftField(draft.aliases, { draft = draft.copy(aliases = it) }, "别名") }
                item { DraftField(draft.identity, { draft = draft.copy(identity = it) }, "公开身份") }
                item { DraftField(draft.publicRumors, { draft = draft.copy(publicRumors = it) }, "公开传闻") }
                item { DraftField(draft.factionId, { draft = draft.copy(factionId = it) }, "所属势力 ID") }
                item { DraftField(draft.gender, { draft = draft.copy(gender = it) }, "性别") }
                item { DraftField(draft.age, { draft = draft.copy(age = it) }, "年龄") }
                item { DraftField(draft.appearance, { draft = draft.copy(appearance = it) }, "外貌") }
                item { DraftField(draft.mentionAllowedFromChapter, { draft = draft.copy(mentionAllowedFromChapter = it) }, "允许提及章节（可空）") }
                item { DraftField(draft.firstAppearanceChapter, { draft = draft.copy(firstAppearanceChapter = it) }, "正式出场章节") }
                item { DraftField(draft.importance, { draft = draft.copy(importance = it) }, "重要程度") }
                item { DraftField(draft.currentStatus, { draft = draft.copy(currentStatus = it) }, "当前状态") }
                item { DraftField(draft.thinkingStyle, { draft = draft.copy(thinkingStyle = it) }, "思考方式") }
                item { DraftField(draft.speechStyle, { draft = draft.copy(speechStyle = it) }, "说话方式") }
                item { DraftField(draft.behaviorHabits, { draft = draft.copy(behaviorHabits = it) }, "行为习惯") }
                item { DraftField(draft.decisionPattern, { draft = draft.copy(decisionPattern = it) }, "决策模式") }
                item { DraftField(draft.valuesAndLimits, { draft = draft.copy(valuesAndLimits = it) }, "价值观与底线") }
                item { DraftField(draft.relationshipHandling, { draft = draft.copy(relationshipHandling = it) }, "关系处理") }
                item { DraftField(draft.informationHandling, { draft = draft.copy(informationHandling = it) }, "信息处理") }
                item { DraftField(draft.background, { draft = draft.copy(background = it) }, "人物背景") }
                item { DraftField(draft.currentGoal, { draft = draft.copy(currentGoal = it) }, "当前目标") }
                item { DraftField(draft.longTermGoal, { draft = draft.copy(longTermGoal = it) }, "长期目标") }
                item { DraftField(draft.coreDesire, { draft = draft.copy(coreDesire = it) }, "核心欲望") }
                item { DraftField(draft.coreFear, { draft = draft.copy(coreFear = it) }, "核心恐惧") }
                item { DraftField(draft.secret, { draft = draft.copy(secret = it) }, "秘密") }
                item { DraftField(draft.abilitiesAndResources, { draft = draft.copy(abilitiesAndResources = it) }, "能力与资源") }
                item { DraftField(draft.weaknesses, { draft = draft.copy(weaknesses = it) }, "弱点") }
                item { Text("人物弧光", style = MaterialTheme.typography.titleMedium) }
                item { DraftField(draft.arcType, { draft = draft.copy(arcType = it) }, "弧光类型") }
                item { DraftField(draft.arcInitialState, { draft = draft.copy(arcInitialState = it) }, "初始状态") }
                item { DraftField(draft.arcTargetState, { draft = draft.copy(arcTargetState = it) }, "目标状态") }
                item { DraftField(draft.arcCurrentStage, { draft = draft.copy(arcCurrentStage = it) }, "当前阶段") }
                item { DraftField(draft.arcTurningPoints, { draft = draft.copy(arcTurningPoints = it) }, "关键转折点") }
                item { DraftField(draft.arcNotes, { draft = draft.copy(arcNotes = it) }, "弧光备注") }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draft) },
                enabled = draft.name.isNotBlank() && draft.identity.isNotBlank() &&
                    (draft.firstAppearanceChapter.toIntOrNull() ?: 0) > 0,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun DraftField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RelationshipDialog(
    initial: CharacterRelationshipDraft,
    characters: List<CharacterCardState>,
    onDismiss: () -> Unit,
    onSave: (CharacterRelationshipDraft) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == null) "新建人物关系" else "编辑人物关系") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("起点人物", style = MaterialTheme.typography.labelLarge) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(characters, key = CharacterCardState::id) { character ->
                            FilterChip(
                                selected = draft.sourceCharacterId == character.id,
                                onClick = { draft = draft.copy(sourceCharacterId = character.id) },
                                label = { Text(character.name) },
                            )
                        }
                    }
                }
                item { Text("终点人物", style = MaterialTheme.typography.labelLarge) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(characters, key = CharacterCardState::id) { character ->
                            FilterChip(
                                selected = draft.targetCharacterId == character.id,
                                onClick = { draft = draft.copy(targetCharacterId = character.id) },
                                label = { Text(character.name) },
                            )
                        }
                    }
                }
                item { DraftField(draft.type, { draft = draft.copy(type = it) }, "关系类型") }
                item { DraftField(draft.summary, { draft = draft.copy(summary = it) }, "公开摘要") }
                item { DraftField(draft.hiddenDetails, { draft = draft.copy(hiddenDetails = it) }, "隐藏信息") }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draft) },
                enabled = draft.sourceCharacterId.isNotBlank() && draft.targetCharacterId.isNotBlank() &&
                    draft.sourceCharacterId != draft.targetCharacterId && draft.type.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun CharacterCardState.toDraft() = CharacterDraft(
    id = id,
    name = name,
    aliases = aliases,
    gender = gender,
    age = age,
    identity = identity,
    publicRumors = publicRumors,
    factionId = factionId.orEmpty(),
    appearance = appearance,
    mentionAllowedFromChapter = mentionAllowedFromChapter?.toString().orEmpty(),
    firstAppearanceChapter = firstAppearanceChapter.toString(),
    importance = importance,
    currentStatus = currentStatus,
    thinkingStyle = thinkingStyle,
    speechStyle = speechStyle,
    behaviorHabits = behaviorHabits,
    decisionPattern = decisionPattern,
    valuesAndLimits = valuesAndLimits,
    relationshipHandling = relationshipHandling,
    informationHandling = informationHandling,
    background = background,
    currentGoal = currentGoal,
    longTermGoal = longTermGoal,
    coreDesire = coreDesire,
    coreFear = coreFear,
    secret = secret,
    abilitiesAndResources = abilitiesAndResources,
    weaknesses = weaknesses,
    arcType = arc?.arcType.orEmpty(),
    arcInitialState = arc?.initialState.orEmpty(),
    arcTargetState = arc?.targetState.orEmpty(),
    arcCurrentStage = arc?.currentStage.orEmpty(),
    arcTurningPoints = arc?.turningPoints.orEmpty(),
    arcNotes = arc?.notes.orEmpty(),
)

private fun CharacterRelationshipCardState.toDraft() = CharacterRelationshipDraft(
    id, sourceCharacterId, targetCharacterId, type, summary, hiddenDetails,
)

@Preview(showBackground = true)
@Composable
private fun CharacterScreenPreview() {
    MaterialTheme {
        CharacterScreen(
            state = CharacterUiState(
                characters = listOf(
                    CharacterCardState(
                        id = "c1", name = "沈潮", aliases = "潮生", gender = "女", age = "27",
                        identity = "领航员", publicRumors = "来自外海", appearance = "银灰短发",
                        factionId = null,
                        mentionAllowedFromChapter = 2, firstAppearanceChapter = 5, importance = "主要",
                        currentStatus = "正常", thinkingStyle = "先观察潮汐", speechStyle = "简短",
                        behaviorHabits = "", decisionPattern = "", valuesAndLimits = "", relationshipHandling = "",
                        informationHandling = "", background = "流亡领航员", currentGoal = "找到失踪航线",
                        longTermGoal = "重建港口", coreDesire = "归属", coreFear = "再次失去船员",
                        secret = "", abilitiesAndResources = "星图", weaknesses = "不信任组织", isArchived = false,
                        arc = CharacterArcCardState("成长", "拒绝责任", "成为船长", "被迫带队"),
                    ),
                ),
            ),
            onSaveCharacter = {},
            onArchiveCharacter = {},
            onDeleteCharacter = {},
            onSaveRelationship = {},
        )
    }
}
