package com.agendamanicura.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val contactDetails: String = "",
    val notes: String = "",
    @ColumnInfo(defaultValue = "1") val isActive: Boolean = true
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "manicure",
    val basePriceCents: Long,
    @ColumnInfo(defaultValue = "0") val sortOrder: Long = 0
)

enum class AppointmentStatus { PENDING, PAID, CANCELLED }
enum class PaymentMethod { CASH, CARD }

@Entity(tableName = "appointments", foreignKeys = [ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["clientId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("clientId"), Index("startAt")])
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val startAt: Long,
    val notes: String = "",
    val tipCents: Long = 0,
    val status: AppointmentStatus = AppointmentStatus.PENDING,
    val paymentMethod: PaymentMethod? = null
)

@Entity(tableName = "appointment_services", primaryKeys = ["appointmentId", "serviceId"], indices = [Index("serviceId")], foreignKeys = [
    ForeignKey(entity = AppointmentEntity::class, parentColumns = ["id"], childColumns = ["appointmentId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = ServiceEntity::class, parentColumns = ["id"], childColumns = ["serviceId"], onDelete = ForeignKey.RESTRICT)
])
data class AppointmentServiceEntity(
    val appointmentId: Long,
    val serviceId: Long,
    val serviceNameSnapshot: String,
    val iconSnapshot: String,
    val priceCents: Long
)
