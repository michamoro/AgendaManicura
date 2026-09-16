package com.agendamanicura.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agendamanicura.data.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(vm: AgendaViewModel, clients: List<ClientEntity>, services: List<ServiceEntity>) {
    val month by vm.currentMonth.collectAsState(); val appointments by vm.appointments.collectAsState(initial = emptyList())
    var selectedDay by remember(month) { mutableStateOf(month.atDay(1)) }; var showEditor by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<AppointmentWithDetails?>(null) }
    val byDay = appointments.groupBy { Instant.ofEpochMilli(it.appointment.startAt).atZone(ZoneId.systemDefault()).toLocalDate() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { IconButton(vm::previousMonth) { Icon(Icons.Default.ChevronLeft, "Mes anterior") }; Text("${month.month.getDisplayName(TextStyle.FULL, Locale("es"))} ${month.year}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); IconButton(vm::nextMonth) { Icon(Icons.Default.ChevronRight, "Mes siguiente") } }
        Row(Modifier.fillMaxWidth()) { listOf("L", "M", "X", "J", "V", "S", "D").forEach { Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) } }
        val leading = (month.atDay(1).dayOfWeek.value - 1); val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(310.dp)) { items(cells) { day -> if (day == null) Box(Modifier.aspectRatio(0.86f)) else DayCell(day, selectedDay == day, byDay[day].orEmpty()) { selectedDay = day } } }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Citas del ${selectedDay.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es")))}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); FilledTonalButton(onClick = { showEditor = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Cita") } }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().weight(1f)) { items(byDay[selectedDay].orEmpty().size) { index -> AppointmentCard(byDay[selectedDay].orEmpty()[index], vm::setStatus) { editing = it; showEditor = true } } }
    }
    if (showEditor) AppointmentEditor(clients, services, selectedDay, editing, onDismiss = { showEditor = false; editing = null }) { client, date, time, items, prices, notes, tip, result -> vm.saveAppointment(client, date, time, items, prices, notes, tip, editing?.appointment?.id ?: 0, editing?.appointment?.status ?: AppointmentStatus.PENDING) { error -> if (error == null) { showEditor = false; editing = null }; result(error) } }
}

@Composable private fun DayCell(day: LocalDate, selected: Boolean, items: List<AppointmentWithDetails>, onClick: () -> Unit) {
    Column(Modifier.padding(2.dp).aspectRatio(.86f).background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(4.dp)) { Text(day.dayOfMonth.toString(), fontWeight = if (day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal); items.take(2).forEach { Row(verticalAlignment = Alignment.CenterVertically) { it.services.take(2).forEach { service -> ServiceIcon(service.iconSnapshot) }; Text(it.client.name.take(7), style = MaterialTheme.typography.labelSmall, maxLines = 1) } }; if (items.size > 2) Text("+${items.size - 2}", style = MaterialTheme.typography.labelSmall) }
}
@Composable fun ServiceIcon(icon: String) { Icon(when (icon) { "pedicure" -> Icons.Default.DirectionsWalk; "nails" -> Icons.Default.Brush; else -> Icons.Default.Spa }, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) }
@Composable fun AppointmentCard(item: AppointmentWithDetails, setStatus: (Long, AppointmentStatus) -> Unit, edit: (AppointmentWithDetails) -> Unit) { Card(Modifier.clickable { edit(item) }) { Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${Instant.ofEpochMilli(item.appointment.startAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))} · ${item.client.name}", fontWeight = FontWeight.Bold); Text(item.services.joinToString { it.serviceNameSnapshot } + if (item.appointment.tipCents > 0) " + propina" else "", style = MaterialTheme.typography.bodySmall); Text(item.totalCents.money(), color = MaterialTheme.colorScheme.primary) }; when (item.appointment.status) { AppointmentStatus.PENDING -> { IconButton({ setStatus(item.appointment.id, AppointmentStatus.PAID) }) { Icon(Icons.Default.CheckCircle, "Marcar como cobrada") }; IconButton({ setStatus(item.appointment.id, AppointmentStatus.CANCELLED) }) { Icon(Icons.Default.Cancel, "Cancelar") } }; AppointmentStatus.PAID -> Icon(Icons.Default.Paid, "Cobrada", tint = Color(0xFF388E3C)); AppointmentStatus.CANCELLED -> Text("Cancelada", style = MaterialTheme.typography.labelSmall) } } } }

@Composable
fun AppointmentEditor(clients: List<ClientEntity>, services: List<ServiceEntity>, initialDate: LocalDate, existing: AppointmentWithDetails?, onDismiss: () -> Unit, save: (Long, LocalDate, LocalTime, List<ServiceEntity>, Map<Long, Long>, String, Long, (String?) -> Unit) -> Unit) {
    val existingDateTime = existing?.appointment?.startAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }; val key = existing?.appointment?.id
    var clientId by remember(key) { mutableLongStateOf(existing?.client?.id ?: 0) }; var dateText by remember(key) { mutableStateOf(existingDateTime?.toLocalDate()?.toString() ?: initialDate.toString()) }; var timeText by remember(key) { mutableStateOf(existingDateTime?.toLocalTime()?.toString() ?: "10:00") }; var selected by remember(key) { mutableStateOf(existing?.services?.map { it.serviceId }?.toSet() ?: setOf()) }; val priceText = remember(key) { mutableStateMapOf<Long, String>().also { map -> existing?.services?.forEach { map[it.serviceId] = "%.2f".format(it.priceCents / 100.0) } } }; var notes by remember(key) { mutableStateOf(existing?.appointment?.notes ?: "") }; var tip by remember(key) { mutableStateOf(existing?.appointment?.tipCents?.let { "%.2f".format(it / 100.0) } ?: "") }; var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Nueva cita" else "Editar cita") }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Clienta"); ClientPicker(clients, clientId) { clientId = it } }
        item { OutlinedTextField(dateText, { dateText = it }, label = { Text("Fecha (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(timeText, { timeText = it }, label = { Text("Hora (HH:MM)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { Text("Servicios", fontWeight = FontWeight.Bold) }
        items(services.size) { i -> val service = services[i]; Column { Row(Modifier.fillMaxWidth().clickable { selected = selected.toggle(service.id) }, verticalAlignment = Alignment.CenterVertically) { Checkbox(selected.contains(service.id), { selected = selected.toggle(service.id) }); ServiceIcon(service.icon); Text("${service.name} · ${service.basePriceCents.money()} · ${service.durationMinutes} min", modifier = Modifier.padding(start = 6.dp)) }; if (selected.contains(service.id)) OutlinedTextField(priceText[service.id] ?: service.basePriceCents.let { "%.2f".format(it / 100.0) }, { priceText[service.id] = it }, label = { Text("Precio para esta cita (€)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) } }
        item { OutlinedTextField(tip, { tip = it }, label = { Text("Propina (€)") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(notes, { notes = it }, label = { Text("Notas del servicio") }, modifier = Modifier.fillMaxWidth()) }
        if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
    } }, confirmButton = { Button(onClick = { val date = runCatching { LocalDate.parse(dateText) }.getOrNull(); val time = runCatching { LocalTime.parse(timeText) }.getOrNull(); if (date == null || time == null) error = "Revisa fecha y hora" else save(clientId, date, time, services.filter { selected.contains(it.id) }, priceText.mapValues { cents(it.value) }, notes, cents(tip)) { error = it } }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
@Composable private fun ClientPicker(clients: List<ClientEntity>, selected: Long, select: (Long) -> Unit) { var expanded by remember { mutableStateOf(false) }; Box { OutlinedButton({ expanded = true }, Modifier.fillMaxWidth()) { Text(clients.find { it.id == selected }?.name ?: "Selecciona una clienta") }; DropdownMenu(expanded, { expanded = false }) { clients.forEach { DropdownMenuItem({ Text(it.name) }, { select(it.id); expanded = false }) } } } }
private fun Set<Long>.toggle(id: Long) = if (contains(id)) minus(id) else plus(id)

@Composable
fun ClientsScreen(clients: List<ClientEntity>, save: (String, String, String, String) -> Unit) {
    var add by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Clientas", style = MaterialTheme.typography.headlineSmall); FilledTonalButton({ add = true }) { Icon(Icons.Default.PersonAdd, null); Text(" Añadir") } }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(clients.size) { i -> val c = clients[i]; Card { Column(Modifier.padding(12.dp)) { Text(c.name, fontWeight = FontWeight.Bold); if (c.phone.isNotBlank()) Text(c.phone); if (c.contactDetails.isNotBlank()) Text(c.contactDetails, style = MaterialTheme.typography.bodySmall); if (c.notes.isNotBlank()) Text(c.notes, style = MaterialTheme.typography.bodySmall) } } } } }
    if (add) ClientDialog({ add = false }, save)
}
@Composable private fun ClientDialog(dismiss: () -> Unit, save: (String, String, String, String) -> Unit) { var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var contact by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = dismiss, title = { Text("Nueva clienta") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }); OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") }); OutlinedTextField(contact, { contact = it }, label = { Text("Otros datos de contacto") }); OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }) } }, confirmButton = { Button({ save(name, phone, contact, notes); dismiss() }) { Text("Guardar") } }, dismissButton = { TextButton(dismiss) { Text("Cancelar") } }) }

@Composable
fun ServicesScreen(services: List<ServiceEntity>, save: (String, String, Int, Long) -> Unit) {
    var add by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Servicios", style = MaterialTheme.typography.headlineSmall); FilledTonalButton({ add = true }) { Icon(Icons.Default.Add, null); Text(" Añadir") } }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(services.size) { i -> val service = services[i]; Card { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { ServiceIcon(service.icon); Column(Modifier.padding(start = 10.dp)) { Text(service.name, fontWeight = FontWeight.Bold); Text("${service.basePriceCents.money()} · ${service.durationMinutes} min") } } } } } }
    if (add) ServiceDialog({ add = false }, save)
}
@Composable
private fun ServiceDialog(dismiss: () -> Unit, save: (String, String, Int, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("60") }
    var icon by remember { mutableStateOf("manicure") }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Nuevo servicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre *") })
                OutlinedTextField(price, { price = it }, label = { Text("Precio (€)") })
                OutlinedTextField(minutes, { minutes = it }, label = { Text("Duración (minutos)") })
                Row { listOf("manicure" to "Manicura", "pedicure" to "Pedicura", "nails" to "Otro").forEach { (value, label) -> FilterChip(icon == value, { icon = value }, { Text(label) }) } }
            }
        },
        confirmButton = { Button({ save(name, icon, minutes.toIntOrNull() ?: 60, cents(price)); dismiss() }) { Text("Guardar") } },
        dismissButton = { TextButton(dismiss) { Text("Cancelar") } }
    )
}

@Composable
fun IncomeScreen(vm: AgendaViewModel, clients: List<ClientEntity>) {
    var range by remember { mutableStateOf("month") }; val today = LocalDate.now(); val from = when (range) { "day" -> today; "month" -> today.withDayOfMonth(1); else -> today.minusDays(30) }; val paid by vm.income(from, today).collectAsState(initial = emptyList()); val total = paid.sumOf { it.totalCents }
    Column(Modifier.fillMaxSize().padding(16.dp)) { Text("Ingresos", style = MaterialTheme.typography.headlineSmall); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("day" to "Hoy", "month" to "Mes", "range" to "30 días").forEach { (id, label) -> FilterChip(range == id, { range = id }, { Text(label) }) } }; Card(Modifier.padding(vertical = 16.dp).fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("Cobrado", style = MaterialTheme.typography.labelLarge); Text(total.money(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary) } }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(paid.size) { i -> val item = paid[i]; Card { Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(item.client.name, fontWeight = FontWeight.Bold); Text(Instant.ofEpochMilli(item.appointment.startAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()); Text(item.services.joinToString { it.serviceNameSnapshot }, style = MaterialTheme.typography.bodySmall) }; Text(item.totalCents.money(), color = MaterialTheme.colorScheme.primary) } } } } }
}

@Composable
fun SettingsScreen(vm: AgendaViewModel) {
    val hour by vm.reminderHour.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Ajustes", style = MaterialTheme.typography.headlineSmall); Text("Aviso diario de citas pendientes", style = MaterialTheme.typography.titleMedium); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { IconButton({ vm.setReminderHour((hour + 23) % 24) }) { Icon(Icons.Default.Remove, "Hora anterior") }; Text("%02d:00".format(hour), style = MaterialTheme.typography.headlineMedium); IconButton({ vm.setReminderHour((hour + 1) % 24) }) { Icon(Icons.Default.Add, "Hora siguiente") } }; Text("La notificación aparece solo cuando haya citas pendientes ese día.", style = MaterialTheme.typography.bodyMedium) }
}
