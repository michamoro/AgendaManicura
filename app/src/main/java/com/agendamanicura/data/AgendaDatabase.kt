package com.agendamanicura.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ClientEntity::class, ServiceEntity::class, AppointmentEntity::class, AppointmentServiceEntity::class], version = 1, exportSchema = false)
abstract class AgendaDatabase : RoomDatabase() {
    abstract fun clients(): ClientDao
    abstract fun services(): ServiceDao
    abstract fun appointments(): AppointmentDao
}
