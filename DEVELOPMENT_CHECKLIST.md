# OpenAuthor 开发清单

本清单依据《openauthor 开发规范》和《导航与界面结构补充规范》维护。状态以已合入代码和 GitHub Actions 验证结果为准。

## 当前状态

- 当前阶段：M0 / Task 5「人物」（进行中）
- 当前分支：`codex/task5-characters`
- 构建策略：仅通过 GitHub Actions 执行单元测试、Lint 和 APK 构建
- 上一阶段：Task 4 已完成，等待 PR 合入

## Task 1：初始化工程（已完成）

- [x] Kotlin 与原生 Android 工程
- [x] Jetpack Compose 与 Material 3
- [x] Navigation Compose
- [x] Room 与基础数据库
- [x] 手动依赖注入
- [x] GitHub Actions：单元测试、Lint、Debug APK
- [x] 基础主题与模块结构（app / core / data / agent / features）

## Task 2：项目、模型配置与基础导航（已完成）

- [x] ProjectEntity、DAO 与 Repository
- [x] ModelConfigEntity、DAO 与 Repository
- [x] Android Keystore API Key 存储
- [x] 项目创建与项目列表
- [x] 模型配置创建、编辑与删除
- [x] 单项目单模型关联
- [x] App 主界面：工作台 / 设置
- [x] 项目界面：总览 / 设定 / 大纲 / 章节 / AI
- [x] 项目内外导航与返回逻辑
- [x] Compose Preview / Fake State
- [x] Room 1 → 2 Migration

## Task 3：Agent Runtime（已完成）

- [x] ModelProvider 接口
- [x] AgentSession、ToolDefinition、ToolExecutor 基础模型
- [x] Manual / Solo 基础循环
- [x] 多步工具结果回传模型
- [x] 步数、连续工具调用数、超时与协程取消限制
- [x] OpenAI Compatible LangChain4j Provider（Android OkHttp）
- [x] AgentRun 状态与日志持久化
- [x] ToolCall 执行记录持久化
- [x] PendingToolCall 持久化、批准与拒绝后续跑
- [x] 工具名称、风险等级与参数 Schema 校验
- [x] 模型调用与工具执行的有限重试
- [x] 用户可理解的失败结果
- [x] Room 2 → 3 Migration
- [x] Agent Runtime 单元测试与 Fake Model 多步测试

验收：

- [x] Solo 模式可连续执行多个合法业务工具
- [x] Manual 模式只自动执行 READ，写操作暂停等待确认
- [x] 工具结果正确回传模型，循环不会在第一步停止
- [x] 非法工具、非法参数、超限、超时、取消和执行失败均留下明确状态
- [x] GitHub Actions 全部通过

## Task 4：世界观（已完成）

- [x] WorldCategoryEntity / WorldEntryEntity 与 Room 3 → 4 Migration
- [x] 七个内置分类与自定义分类初始化
- [x] 世界观分类、条目列表、创建、编辑与删除页面
- [x] 世界观读取、创建、更新、删除工具及 Schema
- [x] 世界观 Agent 对话、Manual / Solo 与写操作确认
- [x] 页面 Preview / Fake State 与单元测试
- [x] GitHub Actions 全部通过

## Task 5：人物（进行中）

- [x] CharacterEntity / CharacterRelationshipEntity / CharacterArcEntity
- [x] Room 4 → 5 Migration
- [x] 人物列表、详情、关系与弧光页面
- [x] 人物业务工具及 Schema
- [x] HIDDEN / MENTION_ONLY / FULL 首次出场过滤
- [x] Character Agent 对话、Manual / Solo 与写操作确认
- [x] 页面 Preview / Fake State 与单元测试
- [ ] GitHub Actions 全部通过

## Task 6：故事骨架与分卷（待开发）

- [ ] StorySpineEntity / VolumeEntity
- [ ] 故事骨架页面
- [ ] 手动创建分卷并锁定章节范围
- [ ] AI 仅填充分卷内容，不创建分卷或修改范围
- [ ] 分卷工具、校验、Preview 与测试

## Task 7：章节细纲（待开发）

- [ ] ChapterOutlineEntity
- [ ] 章节细纲列表与编辑页面
- [ ] 章节类型与必填字段校验
- [ ] AI 创建、修改细纲工具
- [ ] 无有效细纲时禁止生成正文

## Task 8：正文生成（待开发）

- [ ] ChapterEntity
- [ ] 连续章节生成与跳章草稿
- [ ] 纯文本编辑器
- [ ] AI 修改选中文本与整章修改
- [ ] 接受正文工作流
- [ ] 最近章节与跳章上下文组装

## Task 9：一致性（待开发）

- [ ] CharacterStateEntity / WorldStateEntity
- [ ] OpenThreadEntity / ForeshadowingEntity / ChapterSummaryEntity
- [ ] 一致性 Agent 与业务工具
- [ ] 自动重试与可见失败状态
- [ ] 连续章节更新一致性，跳章章节不更新
- [ ] 用户手改正文后的保存选项

## Task 10：导入导出与发布（待开发）

- [ ] 项目 ZIP 导入导出
- [ ] TXT / Markdown / JSON 导出
- [ ] 封面与项目统计
- [ ] 模型列表获取、Agent 运行详情与错误日志
- [ ] GitHub Release 自动生成 APK
- [ ] 完整 README、许可证与用户文档

## 全程约束

- [ ] 所有数据库改动附带 Migration
- [ ] 所有 Agent Runtime 改动附带单元测试
- [ ] 所有工具均为合法业务工具并声明 Schema
- [ ] 所有 Compose 页面提供 Preview 或 Fake State
- [ ] 错误信息对用户可理解
- [ ] 不引入服务器、WebView 主体或富文本编辑器
- [ ] 保持单项目单模型、分卷手动创建、首次出场过滤等核心规则
- [ ] 每个阶段均以 GitHub Actions 通过为完成标准
