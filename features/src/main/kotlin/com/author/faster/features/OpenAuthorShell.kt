package com.author.faster.features

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    WORKBENCH("workbench", "工作台", Icons.Rounded.Home),
    SETTINGS("settings", "设置", Icons.Rounded.Settings),
}

private enum class ProjectDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    OVERVIEW("project/overview", "总览", Icons.Rounded.Home),
    SETTING("project/setting", "设定", Icons.Rounded.SettingsSuggest),
    OUTLINE("project/outline", "大纲", Icons.Rounded.MenuBook),
    CHAPTER("project/chapter", "章节", Icons.Rounded.AutoStories),
    AI("project/ai", "AI", Icons.Rounded.ChatBubble),
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
    val lastUpdated: String = "刚刚",
    val consistencyState: String = "未初始化",
)

data class ProjectsUiState(
    val projects: List<ProjectCardState> = emptyList(),
)

@Composable
fun OpenAuthorShell(
    projectsUiState: ProjectsUiState,
    modelConfigsUiState: ModelConfigsUiState,
    projectCreationUiState: ProjectCreationUiState,
    activeProject: ProjectCardState?,
    onOpenProject: (String) -> Unit,
    onCloseProject: () -> Unit,
    onProjectCreationIntent: (ProjectCreationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = activeProject,
        contentKey = { it?.id ?: "app" },
        label = "navigation-level",
        modifier = modifier.fillMaxSize(),
    ) { project ->
        if (project == null) {
            AppLevelShell(
                projectsUiState = projectsUiState,
                modelConfigsUiState = modelConfigsUiState,
                onCreateProject = { onProjectCreationIntent(ProjectCreationIntent.Open) },
                onOpenProject = onOpenProject,
            )
        } else {
            ProjectLevelShell(
                project = project,
                onCloseProject = onCloseProject,
            )
        }
    }

    if (activeProject == null && projectCreationUiState.isOpen) {
        ProjectCreationWizard(
            state = projectCreationUiState,
            onIntent = onProjectCreationIntent,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppLevelShell(
    projectsUiState: ProjectsUiState,
    modelConfigsUiState: ModelConfigsUiState,
    onCreateProject: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.WORKBENCH.route
    val isPrimaryRoute = AppDestination.entries.any { it.route == currentRoute }

    BackHandler(enabled = currentRoute != AppDestination.WORKBENCH.route) {
        if (currentRoute == AppDestination.SETTINGS.route) {
            navController.navigate(AppDestination.WORKBENCH.route) {
                popUpTo(AppDestination.WORKBENCH.route)
                launchSingleTop = true
            }
        } else {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            if (!isPrimaryRoute) {
                TopAppBar(
                    title = {
                        Text(settingsDetailTitle(currentRoute, backStackEntry?.arguments?.getString("id")))
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回设置")
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (isPrimaryRoute) {
                NavigationBar {
                    AppDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = backStackEntry?.destination?.hierarchy?.any {
                                it.route == destination.route
                            } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(AppDestination.WORKBENCH.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.WORKBENCH.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.WORKBENCH.route) {
                ProjectHomeScreen(
                    state = projectsUiState,
                    onCreateProject = onCreateProject,
                    onOpenProject = onOpenProject,
                )
            }
            composable(AppDestination.SETTINGS.route) {
                AppSettingsScreen(
                    onOpenModelConfigs = { navController.navigate(MODEL_CONFIG_ROUTE) },
                    onOpenDetail = { id -> navController.navigate("settings/detail/$id") },
                )
            }
            composable(MODEL_CONFIG_ROUTE) { ModelConfigScreen(modelConfigsUiState) }
            composable(
                route = "settings/detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                SettingsDetailPlaceholder(entry.arguments?.getString("id").orEmpty())
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProjectLevelShell(
    project: ProjectCardState,
    onCloseProject: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    BackHandler(onBack = onCloseProject)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            project.model,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCloseProject) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回工作台")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                ProjectDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = backStackEntry?.destination?.hierarchy?.any {
                            it.route == destination.route
                        } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(ProjectDestination.OVERVIEW.route) { saveState = true }
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
            startDestination = ProjectDestination.OVERVIEW.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ProjectDestination.OVERVIEW.route) {
                ProjectOverviewScreen(
                    project = project,
                    onContinueWriting = { navController.navigate(ProjectDestination.CHAPTER.route) },
                )
            }
            composable(ProjectDestination.SETTING.route) {
                ProjectPlaceholderScreen("设定", "世界观、人物、人物关系、文风、故事骨架与一致性资产")
            }
            composable(ProjectDestination.OUTLINE.route) {
                ProjectPlaceholderScreen("大纲", "手动创建分卷，并由 AI 协作填写分卷与章节细纲")
            }
            composable(ProjectDestination.CHAPTER.route) {
                ProjectPlaceholderScreen("章节", "生成、审阅、编辑与接受当前项目的章节正文")
            }
            composable(ProjectDestination.AI.route) {
                ProjectPlaceholderScreen("AI", "当前项目的 Manual / Solo Agent 控制中心")
            }
        }
    }
}

@Composable
fun ProjectHomeScreen(
    state: ProjectsUiState,
    onCreateProject: () -> Unit,
    onOpenProject: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("工作台", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    "管理小说项目，继续最近的创作。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }

            if (state.projects.isEmpty()) {
                item { EmptyProjectsCard() }
            } else {
                items(state.projects, key = ProjectCardState::id) { project ->
                    ProjectCard(project = project, onClick = { onOpenProject(project.id) })
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
private fun ProjectCard(project: ProjectCardState, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 84.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            RoundedCornerShape(20.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        project.title.take(1),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(project.title, style = MaterialTheme.typography.headlineSmall)
                    Text(project.author, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        project.phase,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            AnimatedContent(targetState = project.currentChapter, label = "chapter-progress") { chapter ->
                Text("连续完成 $chapter / ${project.targetChapters} 章", style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator(
                progress = {
                    if (project.targetChapters == 0) 0f
                    else project.currentChapter.toFloat() / project.targetChapters
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(project.model, style = MaterialTheme.typography.bodySmall)
                Text(project.generationState, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("一致性：${project.consistencyState}", style = MaterialTheme.typography.bodySmall)
                Text(project.lastUpdated, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProjectOverviewScreen(
    project: ProjectCardState,
    onContinueWriting: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(project.phase, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "第 ${project.currentChapter} / ${project.targetChapters} 章",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (project.targetChapters == 0) 0f
                            else project.currentChapter.toFloat() / project.targetChapters
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    )
                    Button(onClick = onContinueWriting) { Text("继续创作") }
                }
            }
        }
        item { SectionTitle("项目资产") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("世界观", "0", Modifier.weight(1f))
                MetricCard("人物", "0", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("分卷", "0", Modifier.weight(1f))
                MetricCard("章节细纲", "0", Modifier.weight(1f))
            }
        }
        item { SectionTitle("运行状态") }
        item {
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("一致性池", style = MaterialTheme.typography.titleMedium)
                    Text(project.consistencyState, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("最近 Agent 运行", style = MaterialTheme.typography.titleMedium)
                    Text("暂无运行记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectPlaceholderScreen(title: String, description: String) {
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

private fun settingsDetailTitle(route: String, id: String?): String = when {
    route == MODEL_CONFIG_ROUTE -> "模型配置"
    route.startsWith("settings/detail/") -> settingsItemTitle(id.orEmpty())
    else -> "设置"
}

private const val MODEL_CONFIG_ROUTE = "settings/models"

@Preview(showBackground = true)
@Composable
private fun ProjectHomePreview() {
    MaterialTheme {
        ProjectHomeScreen(
            state = ProjectsUiState(listOf(previewProject)),
            onCreateProject = {},
            onOpenProject = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProjectOverviewPreview() {
    MaterialTheme { ProjectOverviewScreen(previewProject, onContinueWriting = {}) }
}

@Preview(showBackground = true)
@Composable
private fun AppLevelShellPreview() {
    MaterialTheme {
        OpenAuthorShell(
            projectsUiState = ProjectsUiState(listOf(previewProject)),
            modelConfigsUiState = ModelConfigsUiState(),
            projectCreationUiState = ProjectCreationUiState(),
            activeProject = null,
            onOpenProject = {},
            onCloseProject = {},
            onProjectCreationIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProjectLevelShellPreview() {
    MaterialTheme {
        OpenAuthorShell(
            projectsUiState = ProjectsUiState(listOf(previewProject)),
            modelConfigsUiState = ModelConfigsUiState(),
            projectCreationUiState = ProjectCreationUiState(),
            activeProject = previewProject,
            onOpenProject = {},
            onCloseProject = {},
            onProjectCreationIntent = {},
        )
    }
}

private val previewProject = ProjectCardState(
    id = "preview",
    title = "群星归途",
    author = "林野",
    currentChapter = 12,
    targetChapters = 240,
    phase = "规划中",
    model = "OpenAI Compatible · novel-model",
    generationState = "等待审阅",
    lastUpdated = "10 分钟前",
    consistencyState = "正常",
)
