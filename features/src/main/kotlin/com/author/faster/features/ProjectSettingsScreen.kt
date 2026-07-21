package com.author.faster.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class SettingAsset { WORLD, CHARACTER }

@Composable
fun ProjectSettingsScreen(
    worldbuildingUiState: WorldbuildingUiState,
    characterUiState: CharacterUiState,
    onCreateWorldCategory: (WorldCategoryDraft) -> Unit,
    onSaveWorldEntry: (WorldEntryDraft) -> Unit,
    onDeleteWorldEntry: (String) -> Unit,
    onRunWorldAgent: (String, WorldAgentMode) -> Unit,
    onResolveWorldTool: (Boolean) -> Unit,
    onSaveCharacter: (CharacterDraft) -> Unit,
    onArchiveCharacter: (String) -> Unit,
    onDeleteCharacter: (String) -> Unit,
    onSaveRelationship: (CharacterRelationshipDraft) -> Unit,
    onRunCharacterAgent: (String, CharacterAgentMode) -> Unit,
    onResolveCharacterTool: (Boolean) -> Unit,
) {
    var asset by rememberSaveable { mutableStateOf(SettingAsset.WORLD) }
    Column(Modifier.fillMaxSize()) {
        Text(
            "长期创作资产",
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        LazyRow(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = asset == SettingAsset.WORLD,
                    onClick = { asset = SettingAsset.WORLD },
                    label = { Text("世界观") },
                )
            }
            item {
                FilterChip(
                    selected = asset == SettingAsset.CHARACTER,
                    onClick = { asset = SettingAsset.CHARACTER },
                    label = { Text("人物") },
                )
            }
        }
        when (asset) {
            SettingAsset.WORLD -> WorldbuildingScreen(
                state = worldbuildingUiState,
                onCreateCategory = onCreateWorldCategory,
                onSaveEntry = onSaveWorldEntry,
                onDeleteEntry = onDeleteWorldEntry,
                onRunAgent = onRunWorldAgent,
                onResolvePendingTool = onResolveWorldTool,
                modifier = Modifier.weight(1f),
            )
            SettingAsset.CHARACTER -> CharacterScreen(
                state = characterUiState,
                onSaveCharacter = onSaveCharacter,
                onArchiveCharacter = onArchiveCharacter,
                onDeleteCharacter = onDeleteCharacter,
                onSaveRelationship = onSaveRelationship,
                onRunAgent = onRunCharacterAgent,
                onResolvePendingTool = onResolveCharacterTool,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
