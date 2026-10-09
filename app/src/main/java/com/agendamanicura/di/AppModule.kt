package com.agendamanicura.di

import android.content.Context
import androidx.room.Room
import com.agendamanicura.data.AgendaDatabase
import com.agendamanicura.data.AgendaRepository
import com.agendamanicura.notifications.ReminderPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): AgendaDatabase = Room.databaseBuilder(context, AgendaDatabase::class.java, "agenda-manicura.db").addMigrations(AgendaDatabase.MIGRATION_1_2, AgendaDatabase.MIGRATION_2_3, AgendaDatabase.MIGRATION_3_4, AgendaDatabase.MIGRATION_4_5).build()
    @Provides @Singleton fun repository(db: AgendaDatabase) = AgendaRepository(db)
    @Provides @Singleton fun reminderPreferences(@ApplicationContext context: Context) = ReminderPreferences(context)
}
