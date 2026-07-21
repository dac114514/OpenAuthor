# OpenAuthor

OpenAuthor 是一个面向个人使用、本地优先、由 AI 驱动的 Android 长篇小说生产系统。

## 当前阶段

项目处于 **M0：Agent Runtime 验证**。当前基座已经包含：

- Kotlin、Jetpack Compose 与 Material 3 Expressive 风格主题
- `app / core / data / agent / features` 模块结构
- Room 数据库入口与项目 Repository
- 分步项目创建向导与单项目单模型配置
- Android Keystore + AES-256/GCM 加密 API Key 存储
- Manual / Solo Agent 核心状态机和业务工具边界
- 五个主要页面的原生 Compose 导航
- GitHub Actions 单元测试、Lint 与 Debug APK 构建

## 构建规则

本项目不在开发者本机执行 Gradle 构建。所有测试、Lint 和 APK 构建均由 GitHub Actions 完成。可在仓库的 **Actions → Android CI → Run workflow** 手动触发构建，并从运行产物中取得 `openauthor-debug`。

## 架构

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

详细产品与开发约束见 [`openauthor开发规范.md`](./openauthor开发规范.md)。
