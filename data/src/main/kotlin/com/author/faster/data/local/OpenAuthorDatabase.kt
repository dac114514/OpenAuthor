package com.author.faster.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class OpenAuthorDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}

