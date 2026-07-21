package com.author.faster

import android.app.Application
import androidx.room.Room
import com.author.faster.agent.runtime.AgentRunStore
import com.author.faster.agent.runtime.PendingToolCallStore
import com.author.faster.core.repository.CharacterRepository
import com.author.faster.core.repository.ProjectRepository
import com.author.faster.core.repository.ModelConfigRepository
import com.author.faster.core.security.ApiKeyStore
import com.author.faster.core.repository.WorldbuildingRepository
import com.author.faster.data.local.OpenAuthorDatabase
import com.author.faster.data.repository.RoomModelConfigRepository
import com.author.faster.data.repository.RoomCharacterRepository
import com.author.faster.data.repository.RoomProjectRepository
import com.author.faster.data.repository.RoomWorldbuildingRepository
import com.author.faster.data.security.KeystoreApiKeyStore
import com.author.faster.runtime.RoomAgentRuntimeStore

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
            .addMigrations(
                OpenAuthorDatabase.MIGRATION_1_2,
                OpenAuthorDatabase.MIGRATION_2_3,
                OpenAuthorDatabase.MIGRATION_3_4,
                OpenAuthorDatabase.MIGRATION_4_5,
            )
            .build()
        val agentRuntimeStore = RoomAgentRuntimeStore(database.agentRunDao())
        container = AppContainer(
            projectRepository = RoomProjectRepository(database.projectDao()),
            modelConfigRepository = RoomModelConfigRepository(database.modelConfigDao()),
            apiKeyStore = KeystoreApiKeyStore(applicationContext),
            agentRunStore = agentRuntimeStore,
            pendingToolCallStore = agentRuntimeStore,
            worldbuildingRepository = RoomWorldbuildingRepository(database.worldbuildingDao()),
            characterRepository = RoomCharacterRepository(database.characterDao()),
        )
    }
}

data class AppContainer(
    val projectRepository: ProjectRepository,
    val modelConfigRepository: ModelConfigRepository,
    val apiKeyStore: ApiKeyStore,
    val agentRunStore: AgentRunStore,
    val pendingToolCallStore: PendingToolCallStore,
    val worldbuildingRepository: WorldbuildingRepository,
    val characterRepository: CharacterRepository,
)
