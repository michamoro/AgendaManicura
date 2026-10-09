package com.agendamanicura.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreview(val createdAt: Long?, val clients: Int, val services: Int, val appointments: Int)

fun parseBackupPreview(raw: String): BackupPreview {
    val backup = JSONObject(raw)
    require(backup.optInt("format") in 1..4) { "Este archivo no es una copia válida de Erika Nail Art." }
    return BackupPreview(
        createdAt = backup.optLong("createdAt").takeIf { it > 0 },
        clients = backup.getJSONArray("clients").length(),
        services = backup.getJSONArray("services").length(),
        appointments = backup.getJSONArray("appointments").length()
    )
}

class AgendaRepository(private val db: AgendaDatabase) {
    private val appointments = db.appointments()
    fun clients(): Flow<List<ClientEntity>> = db.clients().observeAll()
    fun services(): Flow<List<ServiceEntity>> = db.services().observeAll()
    fun appointmentsForMonth(month: LocalDate): Flow<List<AppointmentWithDetails>> = appointments.observeBetween(
        month.withDayOfMonth(1).atStartOfDay().toEpochMillis(), month.plusMonths(1).withDayOfMonth(1).atStartOfDay().toEpochMillis())
    fun paidBetween(from: LocalDate, untilExclusive: LocalDate) = appointments.observePaidBetween(from.atStartOfDay().toEpochMillis(), untilExclusive.atStartOfDay().toEpochMillis())
    suspend fun saveClient(client: ClientEntity) { if (client.id == 0L) db.clients().insert(client) else db.clients().update(client) }
    suspend fun deleteClient(client: ClientEntity) { db.clients().delete(client) }
    suspend fun saveService(service: ServiceEntity) {
        if (service.id == 0L) db.services().insert(service.copy(sortOrder = db.services().nextSortOrder())) else db.services().update(service)
    }
    suspend fun moveService(service: ServiceEntity, direction: Int) = db.withTransaction {
        val services = db.services().all()
        val index = services.indexOfFirst { it.id == service.id }
        val targetIndex = index + direction
        if (index !in services.indices || targetIndex !in services.indices) return@withTransaction
        val target = services[targetIndex]
        db.services().update(service.copy(sortOrder = target.sortOrder))
        db.services().update(target.copy(sortOrder = service.sortOrder))
    }
    suspend fun deleteService(service: ServiceEntity) { db.services().delete(service) }
    suspend fun hasMinimumGap(startAt: Long, excluding: Long = -1) = appointments.minimumGapCount(startAt, 15 * 60_000L, excluding) > 0
    suspend fun saveAppointment(appointment: AppointmentEntity, lines: List<AppointmentServiceEntity>): Long = db.withTransaction {
        val id = if (appointment.id == 0L) appointments.insert(appointment) else { appointments.update(appointment); appointment.id }
        appointments.clearServices(id)
        appointments.insertServices(lines.map { it.copy(appointmentId = id) })
        id
    }
    suspend fun updateStatus(id: Long, status: AppointmentStatus, paymentMethod: PaymentMethod? = null) {
        appointments.getById(id)?.let {
            appointments.update(it.appointment.copy(status = status, paymentMethod = if (status == AppointmentStatus.PAID) paymentMethod else null))
        }
    }
    suspend fun deleteAppointment(id: Long) { appointments.getById(id)?.let { appointments.delete(it.appointment) } }
    suspend fun createBackup(): String = db.withTransaction {
        JSONObject().apply {
            put("format", 4)
            put("createdAt", System.currentTimeMillis())
            put("clients", JSONArray(db.clients().all().map { client -> JSONObject().apply { put("id", client.id); put("name", client.name); put("phone", client.phone); put("contactDetails", client.contactDetails); put("notes", client.notes); put("isActive", client.isActive) } }))
            put("services", JSONArray(db.services().all().map { service -> JSONObject().apply { put("id", service.id); put("name", service.name); put("icon", service.icon); put("basePriceCents", service.basePriceCents); put("sortOrder", service.sortOrder) } }))
            put("appointments", JSONArray(appointments.all().map { item -> JSONObject().apply {
                put("clientId", item.appointment.clientId); put("startAt", item.appointment.startAt); put("notes", item.appointment.notes); put("tipCents", item.appointment.tipCents); put("status", item.appointment.status.name); item.appointment.paymentMethod?.let { put("paymentMethod", it.name) }
                put("services", JSONArray(item.services.map { line -> JSONObject().apply { put("serviceId", line.serviceId); put("name", line.serviceNameSnapshot); put("icon", line.iconSnapshot); put("priceCents", line.priceCents) } }))
            } }))
        }.toString(2)
    }
    fun previewBackup(raw: String): BackupPreview = parseBackupPreview(raw)
    suspend fun restoreBackup(raw: String) = db.withTransaction {
        val backup = JSONObject(raw)
        require(backup.optInt("format") in 1..4) { "Este archivo no es una copia válida de Erika Nail Art." }
        val clientIds = mutableMapOf<Long, Long>(); val serviceIds = mutableMapOf<Long, Long>()
        appointments.deleteAll(); db.clients().deleteAll(); db.services().deleteAll()
        val clients = backup.getJSONArray("clients")
        for (index in 0 until clients.length()) { val item = clients.getJSONObject(index); clientIds[item.getLong("id")] = db.clients().insert(ClientEntity(name = item.getString("name"), phone = item.optString("phone"), contactDetails = item.optString("contactDetails"), notes = item.optString("notes"), isActive = item.optBoolean("isActive", true))) }
        val services = backup.getJSONArray("services")
        for (index in 0 until services.length()) { val item = services.getJSONObject(index); serviceIds[item.getLong("id")] = db.services().insert(ServiceEntity(name = item.getString("name"), icon = item.optString("icon", "manicure"), basePriceCents = item.getLong("basePriceCents"), sortOrder = item.optLong("sortOrder", index.toLong() + 1))) }
        val appointmentsJson = backup.getJSONArray("appointments")
        for (index in 0 until appointmentsJson.length()) { val item = appointmentsJson.getJSONObject(index); val clientId = clientIds[item.getLong("clientId")] ?: continue
            val appointmentId = appointments.insert(AppointmentEntity(clientId = clientId, startAt = item.getLong("startAt"), notes = item.optString("notes"), tipCents = item.optLong("tipCents"), status = runCatching { AppointmentStatus.valueOf(item.optString("status")) }.getOrDefault(AppointmentStatus.PENDING), paymentMethod = runCatching { PaymentMethod.valueOf(item.optString("paymentMethod")) }.getOrNull()))
            val lines = item.getJSONArray("services"); val restoredLines = mutableListOf<AppointmentServiceEntity>()
            for (lineIndex in 0 until lines.length()) { val line = lines.getJSONObject(lineIndex); val serviceId = serviceIds[line.getLong("serviceId")] ?: continue; restoredLines += AppointmentServiceEntity(appointmentId, serviceId, line.optString("name"), line.optString("icon"), line.optLong("priceCents")) }
            if (restoredLines.isNotEmpty()) appointments.insertServices(restoredLines)
        }
    }
}

private fun java.time.LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
