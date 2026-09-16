package com.agendamanicura.notifications

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore by preferencesDataStore("reminder_settings")
class ReminderPreferences(private val context: Context) {
    val hour = context.reminderDataStore.data.map { it[HOUR] ?: 9 }
    suspend fun setHour(hour: Int) { context.reminderDataStore.edit { it[HOUR] = hour.coerceIn(0, 23) }; DailyReminder.schedule(context, hour) }
    private companion object { val HOUR = intPreferencesKey("daily_hour") }
}
