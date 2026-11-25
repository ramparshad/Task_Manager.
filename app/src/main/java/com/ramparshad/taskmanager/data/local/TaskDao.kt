package com.ramparshad.taskmanager.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND dueDate BETWEEN :from AND :to")
    fun observeBetween(from: Long, to: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    @Query("UPDATE tasks SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: com.ramparshad.taskmanager.domain.model.TaskStatus, now: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET priority = :priority, updatedAt = :now WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: com.ramparshad.taskmanager.domain.model.Priority, now: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL")
    suspend fun purgeDeleted()

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY id")
    fun observeForTask(taskId: Long): Flow<List<SubtaskEntity>>

    @Upsert suspend fun upsert(subtask: SubtaskEntity): Long
    @Delete suspend fun delete(subtask: SubtaskEntity)
    @Query("DELETE FROM subtasks WHERE taskId = :taskId") suspend fun deleteForTask(taskId: Long)
}