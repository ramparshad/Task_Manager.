package com.ramparshad.taskmanager.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: return Result.failure()
        val body = inputData.getString("body") ?: ""
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("tasks", "Task Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.notify(
            id.hashCode(),
            NotificationCompat.Builder(applicationContext, "tasks")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .build(),
        )
        return Result.success()
    }

    companion object {
        fun schedule(ctx: Context, taskId: Long, title: String, body: String, atMillis: Long) {
            val delay = (atMillis - System.currentTimeMillis()).coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("title" to title, "body" to body))
                .addTag("task_$taskId")
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "task_$taskId", ExistingWorkPolicy.REPLACE, request,
            )
        }

        fun cancel(ctx: Context, taskId: Long) {
            WorkManager.getInstance(ctx).cancelUniqueWork("task_$taskId")
        }
    }
}