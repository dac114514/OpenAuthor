package com.author.faster

import android.app.Application
import androidx.room.Room
import com.author.faster.core.repository.ProjectRepository
import com.author.faster.core.repository.ModelConfigRepository
import com.author.faster.core.security.ApiKeyStore
import com.author.faster.data.local.OpenAuthorDatabase
import com.author.faster.data.repository.RoomModelConfigRepository
import com.author.faster.data.repository.RoomProjectRepository
import com.author.faster.data.security.KeystoreApiKeyStore

class OpenAuthorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            applicationContext,
            OpenAuthorDatabase::class.java,
            "openauthor.db",
        )
            .addMigrations(OpenAuthorDatabase.MIGRATION_1_2)
            .build()
        container = AppContainer(
            projectRepository = RoomProjectRepository(database.projectDao()),
            modelConfigRepository = RoomModelConfigRepository(database.modelConfigDao()),
            apiKeyStore = KeystoreApiKeyStore(applicationContext),
        )
    }
}

data class AppContainer(
    val projectRepository: ProjectRepository,
    val modelConfigRepository: ModelConfigRepository,
    val apiKeyStore: ApiKeyStore,
)
