package com.author.faster.features

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

enum class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    OVERVIEW("overview", "总览", Icons.Rounded.Home),
    SETTING("setting", "设定", Icons.Rounded.SettingsSuggest),
    OUTLINE("outline", "大纲", Icons.Rounded.MenuBook),
    CHAPTER("chapter", "章节", Icons.Rounded.AutoStories),
    AI("ai", "AI", Icons.Rounded.ChatBubble),
}

data class ProjectCardState(
    val id: String,
    val title: String,
    val author: String,
    val currentChapter: Int,
    val targetChapters: Int,
    val phase: String,
    val model: String,
    val generationState: String,
)

data class ProjectsUiState(
    val projects: List<ProjectCardState> = emptyList(),
)

@Composable
fun OpenAuthorShell(
    projectsUiState: ProjectsUiState,
    onCreateProject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(MainDestination.OVERVIEW.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainDestination.OVERVIEW.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(MainDestination.OVERVIEW.route) {
                ProjectHomeScreen(projectsUiState, onCreateProject)
            }
            composable(MainDestination.SETTING.route) { PlaceholderScreen("设定", "世界观与人物将在这里由 AI 协作维护") }
            composable(MainDestination.OUTLINE.route) { PlaceholderScreen("大纲", "故事骨架、分卷和章节细纲") }
            composable(MainDestination.CHAPTER.route) { PlaceholderScreen("章节", "生成、审阅和接受章节正文") }
            composable(MainDestination.AI.route) { PlaceholderScreen("AI", "Manual / Solo 项目对话") }
        }
    }
}

@Composable
fun ProjectHomeScreen(
    state: ProjectsUiState,
    onCreateProject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "OpenAuthor",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "让 AI 负责长篇创作，让你掌控方向。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }

            if (state.projects.isEmpty()) {
                item { EmptyProjectsCard() }
            } else {
                items(state.projects, key = ProjectCardState::id) { project ->
                    ProjectCard(project)
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onCreateProject,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            icon = { Text("＋", style = MaterialTheme.typography.titleLarge) },
            text = { Text("新建项目") },
        )
    }
}

@Composable
private fun EmptyProjectsCard() {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(28.dp)) {
            Icon(
                imageVector = Icons.Rounded.AutoStories,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(28.dp))
            Text("从一个核心创意开始", style = MaterialTheme.typography.headlineSmall)
            Text(
                "创建项目后，AI 会与你一起完成世界观、人物、故事骨架和章节。",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ProjectCard(project: ProjectCardState) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(project.title, style = MaterialTheme.typography.headlineSmall)
                    Text(project.author, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(project.phase, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
            AnimatedContent(targetState = project.currentChapter, label = "chapter-progress") { chapter ->
                Text("第 $chapter / ${project.targetChapters} 章", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(project.model, style = MaterialTheme.typography.bodySmall)
                Text(project.generationState, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            description,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyProjectHomePreview() {
    MaterialTheme { ProjectHomeScreen(ProjectsUiState(), onCreateProject = {}) }
}

@Preview(showBackground = true)
@Composable
private fun ProjectHomePreview() {
    MaterialTheme {
        ProjectHomeScreen(
            state = ProjectsUiState(
                projects = listOf(
                    ProjectCardState(
                        id = "preview",
                        title = "群星归途",
                        author = "林野",
                        currentChapter = 12,
                        targetChapters = 240,
                        phase = "规划中",
                        model = "OpenAI Compatible",
                        generationState = "等待审阅",
                    ),
                ),
            ),
            onCreateProject = {},
        )
    }
}

