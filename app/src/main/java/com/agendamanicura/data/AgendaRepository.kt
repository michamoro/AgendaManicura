package com.agendamanicura.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AgendaRepository(private val db: AgendaDatabase) {
    private val appointments = db.appointments()
    fun clients(): Flow<List<ClientEntity>> = db.clients().observeAll()
    fun services(): Flow<List<ServiceEntity>> = db.services().observeAll()
    fun appointmentsForMonth(month: LocalDate): Flow<List<AppointmentWithDetails>> = appointments.observeBetween(
        month.withDayOfMonth(1).atStartOfDay().toEpochMillis(), month.plusMonths(1).withDayOfMonth(1).atStartOfDay().toEpochMillis())
    fun paidBetween(from: LocalDate, untilExclusive: LocalDate) = appointments.observePaidBetween(from.atStartOfDay().toEpochMillis(), untilExclusive.atStartOfDay().toEpochMillis())
    suspend fun saveClient(client: ClientEntity) { if (client.id == 0L) db.clients().insert(client) else db.clients().update(client) }
    suspend fun saveService(service: ServiceEntity) { if (service.id == 0L) db.services().insert(service) else db.services().update(service) }
    suspend fun hasConflict(startAt: Long, minutes: Int, excluding: Long = -1) = appointments.overlappingCount(startAt, startAt + minutes * 60_000L, excluding) > 0
    suspend fun saveAppointment(appointment: AppointmentEntity, lines: List<AppointmentServiceEntity>): Long = db.withTransaction {
        val id = if (appointment.id == 0L) appointments.insert(appointment) else { appointments.update(appointment); appointment.id }
        appointments.clearServices(id)
        appointments.insertServices(lines.map { it.copy(appointmentId = id) })
        id
    }
    suspend fun updateStatus(id: Long, status: AppointmentStatus) { appointments.getById(id)?.let { appointments.update(it.appointment.copy(status = status)) } }
}

private fun java.time.LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
