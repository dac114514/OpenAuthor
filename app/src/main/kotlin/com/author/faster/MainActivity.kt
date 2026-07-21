package com.author.faster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.author.faster.features.OpenAuthorShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenAuthorTheme {
                val viewModel: OpenAuthorViewModel = viewModel(
                    factory = OpenAuthorViewModel.factory(
                        projectRepository = (application as OpenAuthorApplication).container.projectRepository,
                        modelConfigRepository = (application as OpenAuthorApplication).container.modelConfigRepository,
                        apiKeyStore = (application as OpenAuthorApplication).container.apiKeyStore,
                        worldbuildingRepository = (application as OpenAuthorApplication).container.worldbuildingRepository,
                        agentRunStore = (application as OpenAuthorApplication).container.agentRunStore,
                        pendingToolCallStore = (application as OpenAuthorApplication).container.pendingToolCallStore,
                    ),
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                OpenAuthorShell(
                    projectsUiState = state.projects,
                    modelConfigsUiState = state.modelConfigs,
                    projectCreationUiState = state.projectCreation,
                    activeProject = state.activeProject,
                    onOpenProject = viewModel::openProject,
                    onCloseProject = viewModel::closeProject,
                    onProjectCreationIntent = viewModel::onProjectCreationIntent,
                    worldbuildingUiState = state.worldbuilding,
                    onCreateWorldCategory = viewModel::createWorldCategory,
                    onSaveWorldEntry = viewModel::saveWorldEntry,
                    onDeleteWorldEntry = viewModel::deleteWorldEntry,
                    onRunWorldAgent = viewModel::runWorldAgent,
                    onResolveWorldTool = viewModel::resolveWorldTool,
                )
            }
        }
    }
}

@Composable
private fun OpenAuthorTheme(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colors = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFD0BCFF),
            secondary = Color(0xFFCCC2DC),
            tertiary = Color(0xFFEFB8C8),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF65558F),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF7D5260),
        )
    }

    MaterialTheme(
        colorScheme = colors,
        shapes = MaterialTheme.shapes.copy(
            large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        ),
        content = content,
    )
}
