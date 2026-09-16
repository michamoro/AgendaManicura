package com.agendamanicura.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agendamanicura.R
import com.agendamanicura.MainActivity
import com.agendamanicura.data.AgendaTime
import com.agendamanicura.data.AgendaDatabase
import com.agendamanicura.data.AppointmentStatus
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import java.util.Locale

class DailyAgendaWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val targetDate = AgendaTime.today().plusDays(1); val db = Room.databaseBuilder(applicationContext, AgendaDatabase::class.java, "agenda-manicura.db").addMigrations(AgendaDatabase.MIGRATION_1_2, AgendaDatabase.MIGRATION_2_3).build()
        val appointments = db.appointments().observeBetween(targetDate.atStartOfDay(AgendaTime.zone).toInstant().toEpochMilli(), targetDate.plusDays(1).atStartOfDay(AgendaTime.zone).toInstant().toEpochMilli()).first().filter { it.appointment.status != AppointmentStatus.CANCELLED }
        db.close()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Agenda diaria", NotificationManager.IMPORTANCE_DEFAULT))
        val title = "Citas del día ${targetDate.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es")))}"
        val text = if (appointments.isEmpty()) "Mañana tienes el día libre: no hay citas agendadas." else "${appointments.size} ${if (appointments.size == 1) "cita" else "citas"}: ${appointments.take(3).joinToString { it.client.name }}"
        val openApp = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP; putExtra(MainActivity.EXTRA_OPEN_AGENDA, true) }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(1001, NotificationCompat.Builder(applicationContext, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_today).setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(openApp).setAutoCancel(true).build())
        return Result.success()
    }
    private companion object { const val CHANNEL = "daily_agenda" }
}
