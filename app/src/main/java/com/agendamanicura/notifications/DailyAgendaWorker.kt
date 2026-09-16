package com.agendamanicura.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agendamanicura.R
import com.agendamanicura.data.AgendaDatabase
import com.agendamanicura.data.AppointmentStatus
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class DailyAgendaWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val today = LocalDate.now(); val db = Room.databaseBuilder(applicationContext, AgendaDatabase::class.java, "agenda-manicura.db").build()
        val appointments = db.appointments().observeBetween(today.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), today.plusDays(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()).first().filter { it.appointment.status == AppointmentStatus.PENDING }
        db.close(); if (appointments.isEmpty()) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Agenda diaria", NotificationManager.IMPORTANCE_DEFAULT))
        val names = appointments.take(3).joinToString { it.client.name }
        manager.notify(1001, NotificationCompat.Builder(applicationContext, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_today).setContentTitle("${appointments.size} citas pendientes hoy").setContentText(names).setStyle(NotificationCompat.BigTextStyle().bigText(names)).setAutoCancel(true).build())
        return Result.success()
    }
    private companion object { const val CHANNEL = "daily_agenda" }
}
