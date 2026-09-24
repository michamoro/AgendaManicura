package com.agendamanicura.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agendamanicura.data.*
import com.agendamanicura.notifications.DailyReminder
import com.agendamanicura.notifications.ReminderPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*
import javax.inject.Inject

@HiltViewModel
class AgendaViewModel @Inject constructor(private val repository: AgendaRepository, private val reminderPreferences: ReminderPreferences, @ApplicationContext private val appContext: Context) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now(AgendaTime.zone))
    val currentMonth = month.asStateFlow()
    val clients = repository.clients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val services = repository.services().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reminderHour = reminderPreferences.hour.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 9)
    val lastBackupAt = reminderPreferences.lastBackupAt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val appointments = month.flatMapLatest { repository.appointmentsForMonth(it.atDay(1)) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun previousMonth() { month.update { it.minusMonths(1) } }
    fun nextMonth() { month.update { it.plusMonths(1) } }
    fun saveClient(client: ClientEntity) = viewModelScope.launch { if (client.name.isNotBlank()) repository.saveClient(client.copy(name = client.name.trim(), phone = client.phone.trim(), contactDetails = client.contactDetails.trim(), notes = client.notes.trim())) }
    fun setClientActive(client: ClientEntity, isActive: Boolean) = viewModelScope.launch { repository.saveClient(client.copy(isActive = isActive)) }
    fun saveService(service: ServiceEntity) = viewModelScope.launch { if (service.name.isNotBlank() && service.basePriceCents > 0) repository.saveService(service.copy(name = service.name.trim())) }
    fun moveService(service: ServiceEntity, direction: Int) = viewModelScope.launch { repository.moveService(service, direction) }
    fun deleteService(service: ServiceEntity, onResult: (String?) -> Unit) = viewModelScope.launch {
        runCatching { repository.deleteService(service) }.onSuccess { onResult(null) }.onFailure { onResult("No se puede eliminar un servicio usado en una cita.") }
    }
    fun saveAppointment(clientId: Long, date: LocalDate, time: LocalTime, selected: List<ServiceEntity>, overridePrices: Map<Long, Long>, notes: String, tipCents: Long, appointmentId: Long = 0, status: AppointmentStatus = AppointmentStatus.PENDING, onResult: (String?) -> Unit) = viewModelScope.launch {
        if (clientId == 0L || selected.isEmpty()) return@launch onResult("Selecciona una clienta y al menos un servicio")
        val start = LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (repository.hasMinimumGap(start, appointmentId)) return@launch onResult("Deja al menos 15 minutos entre citas")
        repository.saveAppointment(AppointmentEntity(id = appointmentId, clientId = clientId, startAt = start, notes = notes, tipCents = tipCents, status = status), selected.map { AppointmentServiceEntity(0, it.id, it.name, it.icon, overridePrices[it.id] ?: it.basePriceCents) })
        onResult(null)
    }
    fun setStatus(id: Long, status: AppointmentStatus) = viewModelScope.launch { repository.updateStatus(id, status) }
    fun deleteAppointment(id: Long) = viewModelScope.launch { repository.deleteAppointment(id) }
    fun setReminderHour(hour: Int) = viewModelScope.launch { reminderPreferences.setHour(hour) }
    fun testDailyReminder() { DailyReminder.test(appContext) }
    fun exportBackup(uri: Uri, onResult: (String) -> Unit) = viewModelScope.launch {
        val result = runCatching { withContext(Dispatchers.IO) { appContext.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(repository.createBackup()) } ?: error("No se pudo crear el archivo") }; reminderPreferences.markBackupSaved() }
        onResult(result.fold({ "Copia de seguridad guardada correctamente." }, { "No se pudo guardar la copia de seguridad." }))
    }
    fun previewBackup(uri: Uri, onResult: (Result<BackupPreview>) -> Unit) = viewModelScope.launch {
        onResult(runCatching { withContext(Dispatchers.IO) { val content = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("No se pudo leer el archivo"); repository.previewBackup(content) } })
    }
    fun importBackup(uri: Uri, onResult: (String) -> Unit) = viewModelScope.launch {
        val result = runCatching { withContext(Dispatchers.IO) { val content = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("No se pudo leer el archivo"); repository.restoreBackup(content) } }
        onResult(result.fold({ "Copia de seguridad restaurada correctamente." }, { it.message ?: "No se pudo restaurar la copia de seguridad." }))
    }
    fun income(from: LocalDate, until: LocalDate) = repository.paidBetween(from, until.plusDays(1))
}

fun cents(text: String): Long = ((text.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).toLong()
fun Long.money(): String = "%.2f €".format(java.util.Locale("es", "ES"), this / 100.0)
