package com.ramparshad.taskmanager.di


import android.content.Context
import androidx.room.Room
import com.ramparshad.taskmanager.data.local.AppDatabase
import com.ramparshad.taskmanager.data.local.SubtaskDao
import com.ramparshad.taskmanager.data.local.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "taskmanager.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun taskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun subtaskDao(db: AppDatabase): SubtaskDao = db.subtaskDao()
}