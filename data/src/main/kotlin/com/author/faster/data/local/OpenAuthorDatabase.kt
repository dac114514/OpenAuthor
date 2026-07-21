package com.author.faster.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProjectEntity::class, ModelConfigEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class OpenAuthorDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    abstract fun modelConfigDao(): ModelConfigDao

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
    }
}
