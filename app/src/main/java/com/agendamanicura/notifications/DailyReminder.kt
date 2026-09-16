package com.agendamanicura.notifications

import android.content.Context
import androidx.work.*
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import com.agendamanicura.data.AgendaTime
import java.util.concurrent.TimeUnit

object DailyReminder {
    fun schedule(context: Context, hour: Int = 9) {
        val now = ZonedDateTime.now(AgendaTime.zone)
        val next = now.toLocalDate().atTime(LocalTime.of(hour, 0)).atZone(AgendaTime.zone).let { if (it.isAfter(now)) it else it.plusDays(1) }
        val request = PeriodicWorkRequestBuilder<DailyAgendaWorker>(24, TimeUnit.HOURS).setInitialDelay(Duration.between(now, next)).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily-agenda", ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun test(context: Context) {
        val request = OneTimeWorkRequestBuilder<DailyAgendaWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("daily-agenda-test", ExistingWorkPolicy.REPLACE, request)
    }
}
