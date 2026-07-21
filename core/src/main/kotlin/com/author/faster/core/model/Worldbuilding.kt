package com.author.faster.core.model

data class WorldCategory(
    val id: String,
    val projectId: String,
    val name: String,
    val description: String,
    val fieldSchemaJson: String,
    val isBuiltIn: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

data class WorldEntry(
    val id: String,
    val projectId: String,
    val categoryId: String,
    val title: String,
    val summary: String,
    val content: String,
    val structuredFieldsJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class BuiltInWorldCategory(
    val key: String,
    val name: String,
    val description: String,
    val fieldSchemaJson: String,
)

object BuiltInWorldCategories {
    val all = listOf(
        BuiltInWorldCategory("foundation", "世界基础", "世界规则、时代背景与基础约束", "{}"),
        BuiltInWorldCategory("location", "地理地点", "地区、城市、建筑与自然环境", "{}"),
        BuiltInWorldCategory("faction", "势力组织", "国家、阵营、组织与权力关系", "{}"),
        BuiltInWorldCategory("society", "社会制度", "法律、阶层、文化、经济与日常制度", "{}"),
        BuiltInWorldCategory("system", "能力或科技体系", "力量、魔法、科技及其代价与限制", "{}"),
        BuiltInWorldCategory("history", "历史事件", "塑造当前世界的重要历史节点", "{}"),
        BuiltInWorldCategory("resource", "物品资源", "关键物品、资源、装备与特殊材料", "{}"),
    )
}
