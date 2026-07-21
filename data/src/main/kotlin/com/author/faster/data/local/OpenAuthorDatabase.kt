package com.author.faster.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        ModelConfigEntity::class,
        AgentRunEntity::class,
        AgentToolCallEntity::class,
        PendingToolCallEntity::class,
        WorldCategoryEntity::class,
        WorldEntryEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class OpenAuthorDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    abstract fun modelConfigDao(): ModelConfigDao

    abstract fun agentRunDao(): AgentRunDao

    abstract fun worldbuildingDao(): WorldbuildingDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `model_configs` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `protocol` TEXT NOT NULL,
                        `baseUrl` TEXT NOT NULL,
                        `apiKeyAlias` TEXT NOT NULL,
                        `modelId` TEXT NOT NULL,
                        `maxContextTokens` INTEGER NOT NULL,
                        `maxOutputTokens` INTEGER NOT NULL,
                        `temperature` REAL NOT NULL,
                        `supportsToolCalling` INTEGER NOT NULL,
                        `supportsStreamingToolCalling` INTEGER NOT NULL,
                        `extraHeadersJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `agent_runs` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT,
                        `agentType` TEXT NOT NULL,
                        `executionMode` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `stepCount` INTEGER NOT NULL,
                        `message` TEXT,
                        `startedAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_agent_runs_projectId` ON `agent_runs` (`projectId`)",
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `agent_tool_calls` (
                        `id` TEXT NOT NULL,
                        `agentRunId` TEXT NOT NULL,
                        `toolName` TEXT NOT NULL,
                        `argumentsJson` TEXT NOT NULL,
                        `riskLevel` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `attempt` INTEGER NOT NULL,
                        `result` TEXT,
                        `errorMessage` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`agentRunId`, `id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_agent_tool_calls_agentRunId` ON `agent_tool_calls` (`agentRunId`)",
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_tool_calls` (
                        `id` TEXT NOT NULL,
                        `agentRunId` TEXT NOT NULL,
                        `toolName` TEXT NOT NULL,
                        `argumentsJson` TEXT NOT NULL,
                        `displayTitle` TEXT NOT NULL,
                        `displayDescription` TEXT NOT NULL,
                        `riskLevel` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`agentRunId`, `id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pending_tool_calls_agentRunId` ON `pending_tool_calls` (`agentRunId`)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `world_categories` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `fieldSchemaJson` TEXT NOT NULL,
                        `isBuiltIn` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_world_categories_projectId` ON `world_categories` (`projectId`)",
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_world_categories_projectId_name` ON `world_categories` (`projectId`, `name`)",
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `world_entries` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `structuredFieldsJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_world_entries_projectId` ON `world_entries` (`projectId`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_world_entries_categoryId` ON `world_entries` (`categoryId`)",
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_world_entries_projectId_categoryId_title` ON `world_entries` (`projectId`, `categoryId`, `title`)",
                )
            }
        }
    }
}
