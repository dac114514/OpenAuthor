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
    ],
    version = 3,
    exportSchema = true,
)
abstract class OpenAuthorDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    abstract fun modelConfigDao(): ModelConfigDao

    abstract fun agentRunDao(): AgentRunDao

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
    }
}
