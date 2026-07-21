# AI 长篇小说生成器开发规范

包名 com.author.faster
应用名 OpenAuthor
安卓项目
禁止本地构建，构建必须新建GitHub仓库，使用actions构建。禁止本地任何构建。

> 项目定位：面向个人使用的、本地优先、AI 全程驱动的长篇小说生成器。  
> 技术路线：Kotlin + Jetpack Compose + Material 3 Expressive + Room + LangChain4j。  
> 构建方式：仅通过 GitHub Actions 构建 APK。  
> 开源策略：完整开源并遵守所使用依赖与参考项目的许可证要求。

---

## 1. 项目目标

本项目不是传统小说编辑器，也不是富文本码字工具，而是一个由 AI 主导完成世界观、人物、故事骨架、分卷规划、章节细纲、正文生成与一致性维护的长篇小说生产系统。

用户主要负责：

1. 创建项目并填写基础信息。
2. 与 AI 对话，提供方向与审阅反馈。
3. 审阅世界观、人物、故事骨架、分卷大纲、章节细纲和正文。
4. 通过简单文本编辑或 AI 指令修改内容。
5. 决定是否接受章节以及是否更新一致性状态。

AI 主要负责：

1. 生成与修改世界观。
2. 创建与维护人物卡。
3. 生成故事骨架。
4. 填充分卷大纲。
5. 创建逐章细纲。
6. 生成章节正文。
7. 更新一致性状态。
8. 在核心情节中推进人物弧光。
9. 通过自由聊天调用工具修改项目内容。

---

## 2. 核心产品原则

### 2.1 AI 主导

软件中的主要内容均由 AI 生成或维护，用户不承担逐字创作责任。

### 2.2 本地优先

- 所有项目数据保存在本地。
- API Key 保存在本地。
- 不依赖自建服务器。
- 不依赖 Termux、Pi 或外部 Linux 环境。
- App 直接请求模型 API。

### 2.3 原生安卓

- 使用 Kotlin。
- 使用 Jetpack Compose。
- 全局视觉采用 Material 3 Expressive。
- 不使用 WebView 作为主体。
- 不使用富文本编辑器。
- 正文仅提供基础纯文本编辑能力。

### 2.4 长篇优先

系统设计必须适配数百章甚至更长的小说，不得默认每一章都有冲突、伏笔、高潮或反转。

允许以下章节类型：

- 剧情推进
- 过渡
- 日常
- 人物塑造
- 氛围
- 世界展开
- 伏笔
- 信息揭示
- 冲突
- 高潮
- 余波
- 旅途
- 调查
- 关系发展
- 自定义

### 2.5 向前一致性

项目采用“只向前生效”的一致性策略。

当人物、世界观、文风、故事骨架或其它设定被修改时：

- 默认不检查历史章节。
- 默认不生成冲突报告。
- 默认不自动重写历史正文。
- 默认不回滚一致性池。
- 新设定只影响后续生成。

只有用户明确要求时，系统才分析历史内容。

---

## 3. 技术架构

```text
Jetpack Compose UI
        ↓
ViewModel + StateFlow
        ↓
Use Case / Workflow State Machine
        ↓
LangChain4j Agent Runtime
        ↓
Domain Tool Layer
        ↓
Repository Layer
        ↓
Room Database + Local File Storage
```

推荐模块划分：

```text
app
core
data
agent
features
```

后续项目扩大后可以拆分为：

```text
app
core-model
core-database
core-storage
core-ai
core-agent
feature-project
feature-world
feature-character
feature-outline
feature-chapter
feature-chat
feature-settings
```

---

## 4. 推荐技术栈

### 4.1 Android

- Kotlin
- Jetpack Compose
- Material 3 Expressive
- Navigation Compose
- ViewModel
- StateFlow
- Room
- DataStore
- Android Keystore
- WorkManager
- OkHttp
- Kotlin Coroutines
- Kotlin Serialization

### 4.2 AI

- LangChain4j
- OpenAI Compatible API
- Gemini Native API
- Anthropic Native API
- 流式输出
- Function Calling / Tool Calling
- 自定义工具执行器
- 自定义 Agent 运行状态机

### 4.3 构建与发布

- GitHub
- GitHub Actions
- Gradle
- APK Artifact
- GitHub Release

---

## 5. 不采用的方案

第一版明确不使用：

- Termux
- Pi
- Linux 虚拟环境
- 自建服务器
- 富文本编辑器
- Electron
- Next.js
- WebView 主体
- 多语言
- 老版本 Android 兼容
- 平板专门适配
- 第三方 Agent 插件
- MCP
- Shell 工具
- 任意代码执行
- 动态加载外部 Agent
- 多项目多模型路由
- 自动历史冲突修复
- 完整版本回退系统

---

## 6. App 页面结构

底部导航建议：

```text
总览
设定
大纲
章节
AI
```

### 6.1 项目首页

显示项目卡片：

- 封面
- 书名
- 作者
- 当前章节
- 目标章节数
- 当前阶段
- 最近更新时间
- 当前模型
- 生成状态

支持：

- 新建项目
- 打开项目
- 删除项目
- 导入项目
- 导出项目

### 6.2 新建项目页

字段：

- 封面
- 书名
- 作者
- 一句话简介
- 核心创意
- 目标章节数
- 默认每章字数
- 小说类型
- 叙事视角
- 项目模型配置

规则：

- 一句话简介仅用于展示，不进入上下文。
- 核心创意可以进入上下文。
- 同一项目只能绑定一个模型配置。
- 模型可在项目设置中更换，但更换后仅影响后续生成。

### 6.3 世界观页

采用“聊天 + 分类内容管理”结构。

内置类别：

1. 世界基础
2. 地理地点
3. 势力组织
4. 社会制度
5. 能力或科技体系
6. 历史事件
7. 物品资源
8. 自定义类别

AI 可通过工具：

- 新建世界观条目
- 修改世界观条目
- 删除世界观条目
- 查询世界观条目
- 创建自定义类别
- 修改分类字段

### 6.4 人物页

采用“聊天 + 人物卡列表”结构。

人物卡必须包含：

#### 基础信息

- 姓名
- 别名
- 性别
- 年龄
- 身份
- 所属势力
- 外貌
- 允许被提及章节
- 正式出场章节
- 重要程度
- 当前状态

#### 差异化字段

- 思考方式
- 说话方式
- 行为习惯
- 决策模式
- 价值观与底线
- 关系处理
- 信息处理

#### 动机与背景

- 人物背景
- 当前目标
- 长期目标
- 核心欲望
- 核心恐惧
- 秘密
- 能力与资源
- 弱点

#### 角色弧光

- 弧光类型
- 初始状态
- 目标状态
- 当前弧光阶段
- 关键转折点
- 弧光备注

### 6.5 故事骨架页

字段：

- 核心矛盾
- 主角目标
- 起点
- 终点
- 主要转折
- 结局方向
- 核心主题
- 整体情绪
- 故事推进原则
- 禁止事项

### 6.6 分卷页

规则：

- 分卷只能由用户手动创建。
- AI 不能创建分卷。
- 分卷创建后禁止修改章节范围。
- AI 可以修改分卷内容。
- 已存在细纲或正文的分卷不可删除。
- 空分卷可以由用户删除后重新创建。

字段：

- 卷号
- 卷名
- 起始章节
- 结束章节
- 本卷定位
- 主要目标
- 核心事件
- 主要人物
- 人物弧光
- 世界变化
- 卷末状态

### 6.7 章节细纲页

每章必须有细纲才能生成正文。

必填字段：

- 章号
- 章节标题
- 所属分卷
- 章节类型
- 出场人物
- 主要内容
- 开始状态
- 结束状态

可选字段：

- POV 人物
- 发生地点
- 时间位置
- 情节目标
- 冲突
- 人物互动
- 信息揭示
- 人物变化
- 氛围目标
- 伏笔埋设
- 伏笔回收
- 世界观展示
- 与上一章衔接
- 与下一章衔接
- 特殊文风要求
- 计划字数

任何可选字段为空都属于合法状态。

### 6.8 章节页

章节列表显示：

- 章号
- 标题
- 所属卷
- 章节类型
- 是否存在细纲
- 生成状态
- 是否连续章节
- 一致性状态

无细纲时显示：

```text
未创建细纲
```

有细纲时显示章节标题。

### 6.9 章节正文页

功能：

- 阅读模式
- 简单文本编辑
- AI 整章修改
- AI 修改选中文本
- 保存草稿
- 接受章节
- 更新一致性
- 重新生成
- 查看 Agent 日志

不提供：

- 富文本
- 复杂格式工具栏
- Word 分页
- 批注系统
- 多版本撤回

---

## 7. 聊天模式

聊天支持两种执行模式：

```kotlin
enum class AgentExecutionMode {
    MANUAL,
    SOLO
}
```

### 7.1 Manual 模式

所有读取工具可直接执行。

所有写操作必须等待用户确认：

- 新建
- 修改
- 删除
- 批量修改
- 保存正文
- 更新一致性
- 更新弧光

流程：

```text
用户发送指令
→ Agent 分析
→ Agent 生成工具调用
→ 工具调用进入待确认队列
→ UI 展示确认卡
→ 用户确认或拒绝
→ 执行工具
→ 返回结果给模型
→ Agent 继续
```

### 7.2 Solo 模式

所有允许范围内的业务工具自动执行。

仍然不能：

- 创建分卷
- 修改分卷范围
- 绕过首次出场限制
- 执行 Shell
- 访问任意文件
- 执行任意代码
- 使用当前页面之外的工具
- 无限循环

### 7.3 工具风险等级

```kotlin
enum class ToolRiskLevel {
    READ,
    CREATE,
    UPDATE,
    DELETE,
    BULK_UPDATE,
    SYSTEM
}
```

Manual 模式下，除 READ 外均需确认。

---

## 8. Agent Runtime

### 8.1 Agent 类型

```text
WorldAgent
CharacterAgent
StorySpineAgent
VolumeAgent
ChapterOutlineAgent
ChapterWriterAgent
ConsistencyAgent
ProjectChatAgent
```

这些 Agent：

- 共享同一个项目模型。
- 使用不同 System Prompt。
- 使用不同工具集。
- 使用不同上下文组装规则。
- 使用不同最大步骤限制。

### 8.2 Agent 核心循环

```kotlin
suspend fun runAgent(
    session: AgentSession,
    maxSteps: Int = 12
): AgentResult {
    var messages = session.messages

    repeat(maxSteps) {
        val response = modelClient.complete(
            messages = messages,
            tools = session.availableTools
        )

        if (response.toolCalls.isEmpty()) {
            return AgentResult.Completed(response.text)
        }

        messages += response.asAssistantMessage()

        for (call in response.toolCalls) {
            val result = if (session.mode == AgentExecutionMode.MANUAL &&
                call.toolRisk != ToolRiskLevel.READ
            ) {
                pendingToolCallStore.save(call)
                return AgentResult.WaitingConfirmation(call.id)
            } else {
                toolExecutor.execute(call)
            }

            messages += result.asToolMessage(call.id)
        }
    }

    return AgentResult.StepLimitReached
}
```

### 8.3 Agent 限制

必须支持：

- 最大步骤数
- 最大连续工具调用数
- 超时
- 用户取消
- 参数 Schema 校验
- 工具失败返回
- 失败重试
- Agent 日志
- Pending Tool Call
- Tool Result
- 运行状态持久化

---

## 9. 工具设计

Agent 不允许直接操作任意文件。

禁止工具：

- read_file
- write_file
- edit_file
- bash
- shell
- exec
- run_script

只允许业务工具。

### 9.1 项目读取工具

```text
get_project
get_story_spine
get_writing_style
get_project_progress
```

### 9.2 世界观工具

```text
list_world_categories
list_world_entries
get_world_entry
create_world_entry
update_world_entry
delete_world_entry
create_world_category
update_world_category
```

### 9.3 人物工具

```text
list_available_characters
get_character
create_character
update_character
archive_character
delete_character
list_relationships
update_relationship
```

### 9.4 分卷工具

```text
list_volumes
get_volume
update_volume
validate_volume_ranges
```

明确不提供：

```text
create_volume
update_volume_range
```

### 9.5 细纲工具

```text
get_chapter_outline
create_chapter_outline
update_chapter_outline
list_chapter_outlines
validate_chapter_outline
```

### 9.6 正文工具

```text
get_chapter_draft
save_chapter_draft
replace_selected_text
save_accepted_chapter
```

### 9.7 一致性工具

```text
get_character_states
update_character_state
get_world_states
update_world_state
list_open_threads
create_open_thread
update_open_thread
list_foreshadowing
add_foreshadowing
resolve_foreshadowing
save_chapter_summary
```

### 9.8 弧光工具

```text
get_character_arc
propose_arc_update
apply_arc_update
```

弧光更新只在以下条件触发：

- 当前章标记为核心情节
- 细纲明确要求弧光变化
- 用户明确要求
- 一致性 Agent 判断存在重大变化

---

## 10. 首次出场规则

人物卡必须包含：

```kotlin
mentionAllowedFromChapter: Int?
firstAppearanceChapter: Int
```

上下文等级：

```kotlin
enum class CharacterContextLevel {
    HIDDEN,
    MENTION_ONLY,
    FULL
}
```

判断逻辑：

```kotlin
fun CharacterEntity.contextLevelAt(chapter: Int): CharacterContextLevel {
    return when {
        chapter >= firstAppearanceChapter ->
            CharacterContextLevel.FULL

        mentionAllowedFromChapter != null &&
            chapter >= mentionAllowedFromChapter ->
            CharacterContextLevel.MENTION_ONLY

        else ->
            CharacterContextLevel.HIDDEN
    }
}
```

### HIDDEN

- 不注入姓名
- 不注入身份
- 不注入关系
- 不注入状态
- 不允许检索命中

### MENTION_ONLY

只注入：

- 姓名或称呼
- 公开身份
- 公开传闻
- 允许提前提及的内容

不注入：

- 秘密
- 完整性格
- 隐藏关系
- 未来动机
- 弧光信息

### FULL

注入完整人物卡、关系、状态和弧光阶段。

首次出场章节后续被修改时，不检查历史章节，只影响未来生成。

---

## 11. 上下文组装

生成第 N 章时，ContextAssembler 按以下顺序组装：

```text
1. 项目写作配置
2. 文风规则
3. 故事骨架压缩版
4. 所属分卷内容
5. 当前章节细纲
6. 当前章可用人物
7. 当前章相关世界观
8. 当前一致性状态
9. 最近章节
10. 活跃线索
11. 相关伏笔
12. 用户临时要求
```

### 11.1 最近章节读取策略

默认：

- 上一章完整正文
- 前第 2～3 章详细摘要
- 前第 4～8 章简短摘要

禁止默认读取大量完整正文。

### 11.2 跳章生成上下文

跳章生成时：

- 读取目标章细纲
- 读取所属分卷
- 读取可用人物
- 读取相关世界观
- 读取最后连续完成章节的一致性池
- 读取目标章之前已有细纲
- 明确告诉模型这是跳章草稿
- 不假设中间章节已经发生的确切结果

### 11.3 禁止注入

- 未到首次出场章节的人物
- 未达到允许提及章节的人物
- 未来章节正文
- 后续分卷细节
- 与本章无关的世界观
- 全部历史正文
- 一句话简介
- 用户未批准的未来设定

---

## 12. 章节状态

```kotlin
enum class ChapterStatus {
    NOT_PLANNED,
    OUTLINE_READY,
    GENERATING,
    DRAFT,
    ACCEPTED,
    CONSISTENCY_UPDATING,
    COMPLETED,
    CONSISTENCY_FAILED,
    CONSISTENCY_POSSIBLY_OUTDATED
}
```

### 12.1 连续章节

当目标章节号等于：

```text
最后连续完成章节 + 1
```

则为连续章节。

连续章节接受后：

1. 保存正式正文。
2. 更新一致性池。
3. 更新章节摘要。
4. 必要时更新弧光。
5. 标记为 COMPLETED。

### 12.2 跳章章节

有细纲即可生成。

跳章章节：

- 可以生成
- 可以编辑
- 可以 AI 修改
- 可以保存
- 不更新一致性池
- 不更新人物状态
- 不更新世界状态
- 不更新伏笔
- 不更新弧光
- 不作为后续连续状态来源

```kotlin
enum class ChapterGenerationMode {
    SEQUENTIAL,
    OUT_OF_ORDER
}
```

---

## 13. 一致性更新

### 13.1 触发时机

仅在连续章节被接受后自动触发。

### 13.2 更新内容

- 人物当前位置
- 人物身体状态
- 人物心理状态
- 人物关系变化
- 人物目标变化
- 世界状态变化
- 势力状态变化
- 地点状态变化
- 活跃线索
- 新增伏笔
- 回收伏笔
- 章节摘要
- 关键弧光变化

### 13.3 失败重试

自动重试多次。

建议默认：

```kotlin
const val MAX_CONSISTENCY_RETRIES = 3
```

重试策略：

1. 原请求重试。
2. 缩减上下文后重试。
3. 使用更严格的结构化输出提示重试。

多次失败后：

- 显示错误。
- 标记章节为 CONSISTENCY_FAILED。
- 正文保留。
- 用户可手动重新执行。
- 不允许继续正式完成下一个连续章节。
- 仍允许查看、编辑、聊天和生成跳章草稿。

---

## 14. 手动修改章节后的处理

当用户修改已完成章节并保存时，弹出提示：

```text
章节内容可能发生变化，现有一致性记录可能已过期。
```

操作：

```text
取消
仅保存
保存并更新一致性
```

### 取消

不保存修改。

### 仅保存

- 保存正文。
- 标记为 CONSISTENCY_POSSIBLY_OUTDATED。
- 不自动更新一致性。

### 保存并更新一致性

- 保存正文。
- 重新运行本章一致性更新。

如果修改的不是最后一个连续完成章节：

- 允许保存。
- 默认不回滚后续一致性。
- 默认不重算后续章节。
- 只有用户明确要求时才进行历史检查。

---

## 15. 数据库设计

推荐 Room Entity：

```text
ProjectEntity
ModelConfigEntity
WorldCategoryEntity
WorldEntryEntity
CharacterEntity
CharacterRelationshipEntity
CharacterArcEntity
StorySpineEntity
VolumeEntity
ChapterOutlineEntity
ChapterEntity
ChapterSummaryEntity
CharacterStateEntity
WorldStateEntity
OpenThreadEntity
ForeshadowingEntity
WritingStyleEntity
ChatSessionEntity
ChatMessageEntity
AgentRunEntity
PendingToolCallEntity
ToolCallEntity
```

### 15.1 ProjectEntity

核心字段：

```kotlin
id
title
author
displaySummary
creativePremise
coverPath
targetChapterCount
defaultWordsPerChapter
genre
pov
modelConfigId
currentPhase
lastSequentialCompletedChapter
createdAt
updatedAt
```

### 15.2 CharacterEntity

核心字段：

```kotlin
id
projectId
name
aliases
gender
age
identity
factionId
appearance
mentionAllowedFromChapter
firstAppearanceChapter
importance
currentStatus
thinkingStyle
speechStyle
behaviorHabits
decisionPattern
valuesAndLimits
relationshipHandling
informationHandling
background
currentGoal
longTermGoal
coreDesire
coreFear
secret
abilitiesAndResources
weaknesses
isArchived
createdAt
updatedAt
```

### 15.3 VolumeEntity

```kotlin
id
projectId
volumeNumber
title
startChapter
endChapter
positioning
mainGoal
coreEvents
mainCharacters
arcSummary
worldChanges
endingState
createdAt
updatedAt
```

章节范围创建后不可修改。

### 15.4 ChapterOutlineEntity

```kotlin
id
projectId
chapterNumber
volumeId
title
chapterType
povCharacterId
appearingCharacterIds
locationIds
timePosition
mainContent
startState
endState
plotGoal
conflict
characterInteraction
informationReveal
characterChange
atmosphereGoal
foreshadowingSetup
foreshadowingPayoff
worldbuildingDisplay
previousChapterLink
nextChapterLink
styleRequirements
plannedWordCount
isArcCritical
createdAt
updatedAt
```

### 15.5 ChapterEntity

```kotlin
id
projectId
chapterNumber
outlineId
title
draftContent
acceptedContent
status
generationMode
consistencyStatus
wordCount
createdAt
updatedAt
acceptedAt
```

### 15.6 PendingToolCallEntity

```kotlin
id
agentRunId
toolName
argumentsJson
displayTitle
displayDescription
riskLevel
status
createdAt
updatedAt
```

---

## 16. 模型配置

同一项目只允许绑定一个模型。

支持协议：

- OpenAI Compatible
- Gemini Native
- Anthropic Native

字段：

```text
配置名称
协议类型
Base URL
API Key
模型 ID
最大上下文
最大输出
温度
是否支持工具调用
是否支持流式工具调用
额外请求头
```

规则：

- API Key 使用 Android Keystore 加密。
- API Key 不进入项目导出包。
- 项目切换模型后，仅影响未来生成。
- 旧内容不重新生成。
- 不做任务级模型路由。

---

## 17. 本地存储与导入导出

### 17.1 真实数据源

Room 为真实数据源。

### 17.2 文件存储

用于：

- 封面
- 项目 ZIP
- TXT 导出
- Markdown 导出
- JSON 导出
- 日志导出
- 数据库备份

### 17.3 项目导出结构

```text
project.zip
├── project.json
├── world/
├── characters/
├── story/
├── volumes/
├── outlines/
├── chapters/
├── state/
├── styles/
├── chats/
└── manifest.json
```

---

## 18. Material 3 Expressive 设计规范

全局要求：

- 使用 Material 3 Expressive。
- 使用明显层级。
- 使用大圆角容器。
- 使用动态形变与平滑过渡。
- 不使用玻璃拟态。
- 不复制 Author 的视觉风格。
- 不采用桌面三栏布局。
- 不采用传统后台管理风格。
- 不使用密集工具栏。
- 不使用复杂富文本控件。

推荐交互：

- 新建项目使用分步向导。
- Agent 工具调用显示为可折叠事件卡。
- Solo 模式显示自动执行状态。
- Manual 模式显示待确认操作卡。
- AI 生成状态使用动态进度容器。
- 正文页面以阅读体验为主。
- 详细参数放入 Bottom Sheet。
- 开发者日志放入二级页面。

---

## 19. Agent 系统提示通用规则

所有 Agent 必须遵守：

```text
1. 项目采用向前一致性原则。
2. 不主动检查历史正文。
3. 不主动修改已完成章节。
4. 不主动创建分卷。
5. 不修改分卷章节范围。
6. 不读取未达到首次出场条件的人物。
7. 不假设未来章节已经发生。
8. 不强制每章包含冲突、伏笔、高潮或钩子。
9. 过渡、日常、氛围和人物塑造章节均合法。
10. 所有写操作必须通过业务工具完成。
11. 不输出伪造的工具执行结果。
12. 工具失败后必须根据真实结果继续。
13. 达到最大步骤时停止。
14. Solo 模式仅代表自动执行合法工具，不代表无限权限。
```

---

## 20. 开发阶段

### M0：Agent Runtime 验证

实现：

- 基础 Compose 项目
- Room
- LangChain4j
- OpenAI Compatible
- 项目创建
- 世界观
- 人物
- Manual / Solo
- 工具调用
- Pending Tool Call
- Agent 日志
- GitHub Actions 构建 APK

验收标准：

- Solo 模式可连续创建人物与世界观。
- Manual 模式写操作可暂停等待确认。
- 工具结果可正确回传模型。
- 多步调用不会在第一步后停止。

### M1：规划系统

实现：

- 项目配置
- 世界观分类字段
- 人物卡
- 首次出场过滤
- 故事骨架
- 手动创建分卷
- 分卷内容生成
- 章节细纲
- Material 3 Expressive 统一视觉

验收标准：

- 无细纲章节不可生成正文。
- AI 不能创建分卷。
- 分卷范围不可修改。
- 未出场人物不进入上下文。

### M2：章节系统

实现：

- 连续章节生成
- 跳章草稿
- 纯文本编辑
- AI 修改选中文本
- AI 整章修改
- 接受章节
- 一致性更新
- 自动重试
- 章节摘要
- 弧光更新
- 最近章节上下文

验收标准：

- 连续章节可更新一致性。
- 跳章章节不更新一致性。
- 一致性多次失败会报错。
- 用户修改正文时会出现一致性提示。

### M3：完善与发布

实现：

- 项目导入导出
- 封面管理
- 模型列表获取
- Agent 运行详情
- 错误日志
- 一致性手动重跑
- 项目统计
- GitHub Release 自动生成 APK
- 完整 README
- AGPL 或选定许可证说明

---

## 21. GitHub Actions

建议工作流：

```yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - uses: gradle/actions/setup-gradle@v4

      - name: Run unit tests
        run: ./gradlew testDebugUnitTest

      - name: Run lint
        run: ./gradlew lintDebug

      - name: Build debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/*.apk
```

推荐附加：

- Gradle Cache
- Detekt
- Ktlint
- Compose Preview Screenshot Test
- Room Migration Test
- Agent Runtime Unit Test
- Fake Model Tool Calling Test

---

## 22. Codex 开发约束

Codex 每次任务必须：

1. 只修改当前目标所需文件。
2. 不一次重构整个项目。
3. 不擅自改变架构。
4. 不引入未批准框架。
5. 不添加服务器依赖。
6. 不添加 WebView 主体。
7. 不添加富文本编辑器。
8. 不改变单项目单模型规则。
9. 不改变分卷手动创建规则。
10. 不取消首次出场过滤。
11. 不添加历史自动回溯。
12. 所有工具必须有 Schema。
13. 所有数据库改动必须附带 Migration。
14. 所有 Agent Runtime 改动必须有单元测试。
15. 所有 Compose 页面必须提供 Preview 或 Fake State。
16. 所有错误必须显示用户可理解的信息。
17. GitHub Actions 必须保持可构建。

---

## 23. 第一批开发任务建议

### Task 1：初始化工程

- Kotlin
- Compose
- Material 3
- Navigation
- Room
- Hilt 或手动依赖注入
- GitHub Actions
- 基础主题
- 基础项目结构

### Task 2：项目与模型配置

- ProjectEntity
- ModelConfigEntity
- Keystore
- 项目创建页
- 项目首页
- 模型配置页

### Task 3：Agent Runtime

- ModelProvider 接口
- OpenAI Compatible Provider
- AgentSession
- AgentRun
- ToolDefinition
- ToolExecutor
- Manual / Solo
- PendingToolCall

### Task 4：世界观

- WorldCategoryEntity
- WorldEntryEntity
- 世界观页面
- 世界观工具
- Agent 对话

### Task 5：人物

- CharacterEntity
- CharacterRelationshipEntity
- CharacterArcEntity
- 人物页面
- 人物工具
- 首次出场过滤

### Task 6：故事骨架与分卷

- StorySpineEntity
- VolumeEntity
- 手动创建分卷
- 禁止修改范围
- AI 填充分卷内容

### Task 7：章节细纲

- ChapterOutlineEntity
- 细纲页面
- 章节类型
- 必填校验
- AI 创建与修改细纲

### Task 8：正文生成

- ChapterEntity
- 章节生成
- 跳章草稿
- 纯文本编辑
- AI 修改
- 接受正文

### Task 9：一致性

- CharacterStateEntity
- WorldStateEntity
- OpenThreadEntity
- ForeshadowingEntity
- ChapterSummaryEntity
- 一致性 Agent
- 自动重试
- 失败状态

### Task 10：导入导出与发布

- ZIP 导入导出
- TXT / Markdown / JSON
- GitHub Release
- README
- 许可证
- 用户文档

---

## 24. 最终产品定义

本项目最终应被定义为：

> 一个运行在 Android 上的、本地优先、由 LangChain4j 驱动、支持 Manual 与 Solo 两种执行模式、面向长篇小说规划与生成的 AI 小说生产系统。

它不是：

- 传统码字软件
- 富文本编辑器
- 通用聊天机器人
- Coding Agent
- 套壳网页
- 自动批量生成垃圾文本的工具

它的核心价值是：

- 结构化世界观
- 结构化人物
- 手动分卷控制
- AI 细纲
- 长篇上下文
- 强制首次出场
- 连续章节状态维护
- 跳章草稿
- 一致性池
- 人物弧光
- 可控工具调用
- Manual / Solo 双模式
- 本地数据安全
