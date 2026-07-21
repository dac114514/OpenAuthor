package com.author.faster

import android.app.Application
import androidx.room.Room
import com.author.faster.core.repository.ProjectRepository
import com.author.faster.data.local.OpenAuthorDatabase
import com.author.faster.data.repository.RoomProjectRepository

class OpenAuthorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            applicationContext,
            OpenAuthorDatabase::class.java,
            "openauthor.db",
        ).build()
        container = AppContainer(
            projectRepository = RoomProjectRepository(database.projectDao()),
        )
    }
}

data class AppContainer(
    val projectRepository: ProjectRepository,
)

