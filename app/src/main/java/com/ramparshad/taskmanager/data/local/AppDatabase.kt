package com.ramparshad.taskmanager.data.local

import androidx.room.*

@Database(
    entities = [TaskEntity::class, SubtaskEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class) // <--- ADD THIS LINE
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
}