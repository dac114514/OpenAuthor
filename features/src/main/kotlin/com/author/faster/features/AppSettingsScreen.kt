package com.author.faster.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private data class AppSettingItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
)

private val settingItems = listOf(
    AppSettingItem("models", "模型配置", "协议、模型 ID 与能力", Icons.Rounded.Memory),
    AppSettingItem("api_keys", "API Key", "本机加密凭据", Icons.Rounded.Key),
    AppSettingItem("defaults", "默认生成参数", "章节字数、上下文与执行模式", Icons.Rounded.Tune),
    AppSettingItem("appearance", "外观与主题", "动态色彩与显示偏好", Icons.Rounded.DarkMode),
    AppSettingItem("data", "数据与备份", "本地备份、导入与导出", Icons.Rounded.Backup),
    AppSettingItem("diagnostics", "日志与诊断", "错误日志与运行信息", Icons.Rounded.Description),
    AppSettingItem("licenses", "开源许可证", "依赖许可与开源信息", Icons.Rounded.Code),
    AppSettingItem("about", "关于 OpenAuthor", "版本与项目说明", Icons.Rounded.AutoAwesome),
)

@Composable
fun AppSettingsScreen(
    onOpenModelConfigs: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("设置", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(
                "管理 App 级配置。项目内容只在项目工作界面中修改。",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth()) {
                    settingItems.forEachIndexed { index, item ->
                        ListItem(
                            headlineContent = { Text(item.title) },
                            supportingContent = { Text(item.description) },
                            leadingContent = { Icon(item.icon, contentDescription = null) },
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                if (item.id == "models") onOpenModelConfigs() else onOpenDetail(item.id)
                            },
                        )
                        if (index != settingItems.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDetailPlaceholder(id: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(settingsItemTitle(id), style = MaterialTheme.typography.headlineMedium)
        Text(
            "此 App 级设置将在后续任务中实现。",
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun settingsItemTitle(id: String): String = settingItems.firstOrNull { it.id == id }?.title ?: "设置"

@Preview(showBackground = true)
@Composable
private fun AppSettingsScreenPreview() {
    MaterialTheme { AppSettingsScreen(onOpenModelConfigs = {}, onOpenDetail = {}) }
}

@Preview(showBackground = true)
@Composable
private fun SettingsDetailPlaceholderPreview() {
    MaterialTheme { SettingsDetailPlaceholder("defaults") }
}
