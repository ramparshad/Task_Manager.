package com.ramparshad.taskmanager.data.local


import androidx.room.TypeConverter
import com.ramparshad.taskmanager.domain.model.Priority
import com.ramparshad.taskmanager.domain.model.RecurrenceType
import com.ramparshad.taskmanager.domain.model.TaskStatus

class Converters {

    // Safely converts List<String> to a single String using an uncommonly used delimiter
    @TypeConverter
    fun listToString(value: List<String>?): String {
        return value?.joinToString(separator = "|||") ?: ""
    }

    // Converts String back to List<String>
    @TypeConverter
    fun stringToList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("|||")
    }

    // Enum Converters
    @TypeConverter
    fun priorityToString(value: Priority): String = value.name

    @TypeConverter
    fun stringToPriority(value: String): Priority = runCatching { Priority.valueOf(value) }.getOrDefault(Priority.Medium)

    @TypeConverter
    fun statusToString(value: TaskStatus): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): TaskStatus = runCatching { TaskStatus.valueOf(value) }.getOrDefault(TaskStatus.Pending)

    @TypeConverter
    fun recurToString(value: RecurrenceType): String = value.name

    @TypeConverter
    fun stringToRecur(value: String): RecurrenceType = runCatching { RecurrenceType.valueOf(value) }.getOrDefault(RecurrenceType.None)
}