package com.agendamanicura.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agendamanicura.data.*
import com.agendamanicura.notifications.ReminderPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import javax.inject.Inject

@HiltViewModel
class AgendaViewModel @Inject constructor(private val repository: AgendaRepository, private val reminderPreferences: ReminderPreferences) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    val currentMonth = month.asStateFlow()
    val clients = repository.clients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val services = repository.services().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reminderHour = reminderPreferences.hour.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 9)
    val appointments = month.flatMapLatest { repository.appointmentsForMonth(it.atDay(1)) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun previousMonth() { month.update { it.minusMonths(1) } }
    fun nextMonth() { month.update { it.plusMonths(1) } }
    fun saveClient(name: String, phone: String, contact: String, notes: String) = viewModelScope.launch { if (name.isNotBlank()) repository.saveClient(ClientEntity(name = name.trim(), phone = phone, contactDetails = contact, notes = notes)) }
    fun saveService(name: String, icon: String, minutes: Int, cents: Long) = viewModelScope.launch { if (name.isNotBlank() && minutes > 0) repository.saveService(ServiceEntity(name = name.trim(), icon = icon, durationMinutes = minutes, basePriceCents = cents)) }
    fun saveAppointment(clientId: Long, date: LocalDate, time: LocalTime, selected: List<ServiceEntity>, overridePrices: Map<Long, Long>, notes: String, tipCents: Long, appointmentId: Long = 0, status: AppointmentStatus = AppointmentStatus.PENDING, onResult: (String?) -> Unit) = viewModelScope.launch {
        if (clientId == 0L || selected.isEmpty()) return@launch onResult("Selecciona una clienta y al menos un servicio")
        val minutes = selected.sumOf { it.durationMinutes }; val start = LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (repository.hasConflict(start, minutes, appointmentId)) return@launch onResult("Ese horario se cruza con otra cita")
        repository.saveAppointment(AppointmentEntity(id = appointmentId, clientId = clientId, startAt = start, durationMinutes = minutes, notes = notes, tipCents = tipCents, status = status), selected.map { AppointmentServiceEntity(0, it.id, it.name, it.icon, it.durationMinutes, overridePrices[it.id] ?: it.basePriceCents) })
        onResult(null)
    }
    fun setStatus(id: Long, status: AppointmentStatus) = viewModelScope.launch { repository.updateStatus(id, status) }
    fun setReminderHour(hour: Int) = viewModelScope.launch { reminderPreferences.setHour(hour) }
    fun income(from: LocalDate, until: LocalDate) = repository.paidBetween(from, until.plusDays(1))
}

fun cents(text: String): Long = ((text.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).toLong()
fun Long.money(): String = "%.2f €".format(java.util.Locale("es", "ES"), this / 100.0)
