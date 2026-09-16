package com.agendamanicura

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.agendamanicura.notifications.DailyReminder
import com.agendamanicura.notifications.ReminderPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

@HiltAndroidApp class AgendaApplication : Application() {
    override fun onCreate() { super.onCreate(); CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { DailyReminder.schedule(this@AgendaApplication, ReminderPreferences(this@AgendaApplication).hour.first()) } }
}
