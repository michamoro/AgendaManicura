package com.agendamanicura.notifications

import android.content.Context
import androidx.work.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object DailyReminder {
    fun schedule(context: Context, hour: Int = 9) {
        val now = LocalDateTime.now(); val next = now.toLocalDate().atTime(LocalTime.of(hour, 0)).let { if (it.isAfter(now)) it else it.plusDays(1) }
        val request = PeriodicWorkRequestBuilder<DailyAgendaWorker>(24, TimeUnit.HOURS).setInitialDelay(Duration.between(now, next)).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily-agenda", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
