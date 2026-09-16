package com.agendamanicura.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ClientEntity::class, ServiceEntity::class, AppointmentEntity::class, AppointmentServiceEntity::class], version = 3, exportSchema = false)
abstract class AgendaDatabase : RoomDatabase() {
    abstract fun clients(): ClientDao
    abstract fun services(): ServiceDao
    abstract fun appointments(): AppointmentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clients ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `appointment_services_backup` (`appointmentId` INTEGER NOT NULL, `serviceId` INTEGER NOT NULL, `serviceNameSnapshot` TEXT NOT NULL, `iconSnapshot` TEXT NOT NULL, `priceCents` INTEGER NOT NULL, PRIMARY KEY(`appointmentId`, `serviceId`))")
                db.execSQL("INSERT INTO `appointment_services_backup` (`appointmentId`, `serviceId`, `serviceNameSnapshot`, `iconSnapshot`, `priceCents`) SELECT `appointmentId`, `serviceId`, `serviceNameSnapshot`, `iconSnapshot`, `priceCents` FROM `appointment_services`")
                db.execSQL("DROP TABLE `appointment_services`")
                db.execSQL("CREATE TABLE `appointments_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `clientId` INTEGER NOT NULL, `startAt` INTEGER NOT NULL, `notes` TEXT NOT NULL, `tipCents` INTEGER NOT NULL, `status` TEXT NOT NULL, FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("INSERT INTO `appointments_new` (`id`, `clientId`, `startAt`, `notes`, `tipCents`, `status`) SELECT `id`, `clientId`, `startAt`, `notes`, `tipCents`, `status` FROM `appointments`")
                db.execSQL("DROP TABLE `appointments`")
                db.execSQL("ALTER TABLE `appointments_new` RENAME TO `appointments`")
                db.execSQL("CREATE INDEX `index_appointments_clientId` ON `appointments` (`clientId`)")
                db.execSQL("CREATE INDEX `index_appointments_startAt` ON `appointments` (`startAt`)")
                db.execSQL("CREATE TABLE `services_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `basePriceCents` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO `services_new` (`id`, `name`, `icon`, `basePriceCents`) SELECT `id`, `name`, `icon`, `basePriceCents` FROM `services`")
                db.execSQL("DROP TABLE `services`")
                db.execSQL("ALTER TABLE `services_new` RENAME TO `services`")
                db.execSQL("CREATE TABLE `appointment_services` (`appointmentId` INTEGER NOT NULL, `serviceId` INTEGER NOT NULL, `serviceNameSnapshot` TEXT NOT NULL, `iconSnapshot` TEXT NOT NULL, `priceCents` INTEGER NOT NULL, PRIMARY KEY(`appointmentId`, `serviceId`), FOREIGN KEY(`appointmentId`) REFERENCES `appointments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`serviceId`) REFERENCES `services`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("INSERT INTO `appointment_services` (`appointmentId`, `serviceId`, `serviceNameSnapshot`, `iconSnapshot`, `priceCents`) SELECT `appointmentId`, `serviceId`, `serviceNameSnapshot`, `iconSnapshot`, `priceCents` FROM `appointment_services_backup`")
                db.execSQL("CREATE INDEX `index_appointment_services_serviceId` ON `appointment_services` (`serviceId`)")
                db.execSQL("DROP TABLE `appointment_services_backup`")
            }
        }
    }
}
