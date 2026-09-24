package com.agendamanicura.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.agendamanicura.R
import com.agendamanicura.data.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CalendarScreen(vm: AgendaViewModel, clients: List<ClientEntity>, services: List<ServiceEntity>) {
    val month by vm.currentMonth.collectAsState()
    val appointments by vm.appointments.collectAsState(initial = emptyList())
    val todayInSpain = AgendaTime.today()
    var selectedDay by remember(month) { mutableStateOf(if (month == YearMonth.from(todayInSpain)) todayInSpain else month.atDay(1)) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AppointmentWithDetails?>(null) }
    var deleting by remember { mutableStateOf<AppointmentWithDetails?>(null) }
    var appointmentQuery by rememberSaveable { mutableStateOf("") }
    val byDay = appointments.groupBy { Instant.ofEpochMilli(it.appointment.startAt).atZone(ZoneId.systemDefault()).toLocalDate() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(vm::previousMonth) { Icon(Icons.Default.ChevronLeft, "Mes anterior") }
            Text(month.displayMonthYear(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(vm::nextMonth) { Icon(Icons.Default.ChevronRight, "Mes siguiente") }
        }
        Row(Modifier.fillMaxWidth()) { listOf("L", "M", "X", "J", "V", "S", "D").forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) } }
        val leading = month.atDay(1).dayOfWeek.value - 1
        val cells = List(leading) { null } + (1..month.lengthOfMonth()).map(month::atDay)
        val weekCount = (cells.size + 6) / 7
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height((weekCount * 62).dp)) { items(cells) { day -> if (day == null) Box(Modifier.aspectRatio(.86f)) else DayCell(day, selectedDay == day, byDay[day].orEmpty().filter { it.appointment.status != AppointmentStatus.CANCELLED }) { selectedDay = day } } }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Citas del ${selectedDay.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es")))}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = { editing = null; showEditor = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Cita") }
        }
        OutlinedTextField(appointmentQuery, { appointmentQuery = it }, modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 4.dp), singleLine = true, textStyle = MaterialTheme.typography.bodySmall, leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) }, label = { Text("Buscar por clienta o servicio", style = MaterialTheme.typography.labelSmall) })
        val matching = byDay[selectedDay].orEmpty().filter { item ->
            appointmentQuery.isBlank() || item.client.name.contains(appointmentQuery, true) || item.services.any { it.serviceNameSnapshot.contains(appointmentQuery, true) }
        }
        val active = matching.filter { it.appointment.status != AppointmentStatus.CANCELLED }
        val cancelled = matching.filter { it.appointment.status == AppointmentStatus.CANCELLED }
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (active.isNotEmpty()) item { Text("Citas activas", style = MaterialTheme.typography.labelLarge) }
            items(active.size) { index -> AppointmentCard(active[index], vm::setStatus, { deleting = it }) { editing = it; showEditor = true } }
            if (cancelled.isNotEmpty()) item { Text("Canceladas", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp)) }
            items(cancelled.size) { index -> AppointmentCard(cancelled[index], vm::setStatus, { deleting = it }) { editing = it; showEditor = true } }
        }
    }
    if (showEditor) AppointmentEditor(clients, services, selectedDay, editing, onDismiss = { showEditor = false; editing = null }) { client, date, time, items, prices, notes, tip, result ->
        vm.saveAppointment(client, date, time, items, prices, notes, tip, editing?.appointment?.id ?: 0, editing?.appointment?.status ?: AppointmentStatus.PENDING) { error -> if (error == null) { showEditor = false; editing = null }; result(error) }
    }
    deleting?.let { item ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Eliminar cita") }, text = { Text("¿Quieres borrar permanentemente esta cita de ${item.client.name}?") }, confirmButton = {
            Button({ vm.deleteAppointment(item.appointment.id); deleting = null }) { Text("Eliminar") }
        }, dismissButton = { TextButton({ deleting = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun DayCell(day: LocalDate, selected: Boolean, items: List<AppointmentWithDetails>, onClick: () -> Unit) {
    Column(Modifier.padding(2.dp).aspectRatio(.86f).background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(4.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text(day.dayOfMonth.toString(), fontWeight = FontWeight.Bold)
        if (items.isNotEmpty()) {
            Text("${items.size} ${if (items.size == 1) "cita" else "citas"}", modifier = Modifier.align(Alignment.CenterHorizontally).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp), fontSize = 12.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
        }
    }
}

private fun serviceEmoji(icon: String) = when (icon) { "manicure" -> "💅"; "pedicure" -> "🦶"; "nails" -> "✨"; else -> icon.ifBlank { "✨" } }
@Composable fun ServiceIcon(icon: String, size: androidx.compose.ui.unit.TextUnit = 19.2.sp) = Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { Text(serviceEmoji(icon), fontSize = size) }

@Composable
private fun AppointmentStatusBadge(status: AppointmentStatus) {
    val (icon, label, tint) = when (status) {
        AppointmentStatus.PENDING -> Triple(Icons.Default.Schedule, "Creada", MaterialTheme.colorScheme.primary)
        AppointmentStatus.PAID -> Triple(Icons.Default.Paid, "Cobrada", Color(0xFF388E3C))
        AppointmentStatus.CANCELLED -> Triple(Icons.Default.Cancel, "Cancelada", MaterialTheme.colorScheme.error)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(icon, label, tint = tint, modifier = Modifier.size(18.dp)); Text(label, style = MaterialTheme.typography.labelSmall, color = tint) }
}

@Composable
fun AppointmentCard(item: AppointmentWithDetails, setStatus: (Long, AppointmentStatus) -> Unit, delete: (AppointmentWithDetails) -> Unit, edit: (AppointmentWithDetails) -> Unit) {
    val context = LocalContext.current
    var showReminder by remember(item.appointment.id) { mutableStateOf(false) }
    var reminderText by remember(item.appointment.id) { mutableStateOf(TextFieldValue(appointmentReminderText(item), selection = TextRange(0))) }
    var reminderError by remember(item.appointment.id) { mutableStateOf<String?>(null) }
    Card(Modifier.fillMaxWidth().clickable { edit(item) }) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${Instant.ofEpochMilli(item.appointment.startAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))} · ${item.client.name}", fontWeight = FontWeight.Bold)
                Text(item.services.joinToString { it.serviceNameSnapshot }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.appointment.notes.isNotBlank()) Text("Nota: ${item.appointment.notes}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.totalCents.money(), color = MaterialTheme.colorScheme.primary)
                    if (item.appointment.tipCents > 0) {
                        Text("(Propina ${item.appointment.tipCents.money()})", color = Color(0xFF388E3C), style = MaterialTheme.typography.labelSmall)
                    }
                }
                AppointmentStatusBadge(item.appointment.status)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton({ showReminder = true }, modifier = Modifier.size(40.dp)) { Icon(painterResource(R.drawable.ic_whatsapp), "Enviar recordatorio por WhatsApp") }
                when (item.appointment.status) {
                    AppointmentStatus.PENDING -> { IconButton({ setStatus(item.appointment.id, AppointmentStatus.PAID) }, Modifier.size(40.dp)) { Icon(Icons.Default.CheckCircle, "Marcar como cobrada") }; IconButton({ setStatus(item.appointment.id, AppointmentStatus.CANCELLED) }, Modifier.size(40.dp)) { Icon(Icons.Default.Cancel, "Cancelar") }; IconButton({ delete(item) }, Modifier.size(40.dp)) { Icon(Icons.Default.Delete, "Eliminar") } }
                    AppointmentStatus.PAID -> { IconButton({ setStatus(item.appointment.id, AppointmentStatus.PENDING) }, Modifier.size(40.dp)) { Icon(Icons.Default.Undo, "Volver a creada") }; IconButton({ setStatus(item.appointment.id, AppointmentStatus.CANCELLED) }, Modifier.size(40.dp)) { Icon(Icons.Default.Cancel, "Cancelar") }; IconButton({ delete(item) }, Modifier.size(40.dp)) { Icon(Icons.Default.Delete, "Eliminar") } }
                    AppointmentStatus.CANCELLED -> { IconButton({ setStatus(item.appointment.id, AppointmentStatus.PENDING) }, Modifier.size(40.dp)) { Icon(Icons.Default.Undo, "Volver a creada") }; IconButton({ delete(item) }, Modifier.size(40.dp)) { Icon(Icons.Default.Delete, "Eliminar") } }
                }
            }
        }
    }
    if (showReminder) AlertDialog(
        onDismissRequest = { showReminder = false },
        title = { Text("Recordatorio para ${item.client.name}") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Puedes ajustar el mensaje antes de enviarlo.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(reminderText, { reminderText = it }, modifier = Modifier.fillMaxWidth(), minLines = 8, maxLines = 12)
            reminderError?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { Button({
            val phone = whatsappPhone(item.client.phone)
            if (phone == null) reminderError = "Añade un teléfono válido para enviar por WhatsApp."
            else runCatching {
                val url = "https://wa.me/$phone?text=${Uri.encode(reminderText.text)}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.whatsapp"))
            }.onSuccess { showReminder = false }.onFailure { reminderError = "No se pudo abrir WhatsApp en este dispositivo." }
        }) { Icon(Icons.Default.Send, null); Spacer(Modifier.width(6.dp)); Text("Abrir WhatsApp") } },
        dismissButton = { TextButton({ showReminder = false }) { Text("Cancelar") } }
    )
}

fun appointmentReminderText(item: AppointmentWithDetails): String {
    val dateTime = Instant.ofEpochMilli(item.appointment.startAt).atZone(AgendaTime.zone)
        .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'a las' hh:mm a", Locale("es")))
    return """Hola, buen día 🌸😊 ${item.client.name}

Este es un recordatorio para tu cita:

Fecha y hora del servicio:
$dateTime

Servicios:
${item.services.joinToString("\n") { "• ${it.serviceNameSnapshot}" }}

Dirección:
Calle Miguel De Cervantes #3 1A
Alhaurín el Grande (edificio)

¡Nos vemos pronto! 😊"""
}

private fun whatsappPhone(value: String): String? {
    val digits = value.filter(Char::isDigit).removePrefix("00")
    if (digits.length < 7) return null
    return when {
        value.trim().startsWith("+") -> digits
        digits.length == 9 -> "34$digits"
        else -> digits
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentEditor(clients: List<ClientEntity>, services: List<ServiceEntity>, initialDate: LocalDate, existing: AppointmentWithDetails?, onDismiss: () -> Unit, save: (Long, LocalDate, LocalTime, List<ServiceEntity>, Map<Long, Long>, String, Long, (String?) -> Unit) -> Unit) {
    val existingDateTime = existing?.appointment?.startAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
    val key = existing?.appointment?.id
    var clientId by remember(key) { mutableLongStateOf(existing?.client?.id ?: 0) }
    var date by remember(key) { mutableStateOf(existingDateTime?.toLocalDate() ?: initialDate) }
    var time by remember(key) { mutableStateOf(existingDateTime?.toLocalTime()?.withSecond(0)?.withNano(0) ?: LocalTime.of(10, 0)) }
    var selected by remember(key) { mutableStateOf(existing?.services?.map { it.serviceId }?.toSet() ?: setOf()) }
    val priceText = remember(key) { mutableStateMapOf<Long, String>().also { map -> existing?.services?.forEach { map[it.serviceId] = "%.2f".format(it.priceCents / 100.0) } } }
    var notes by remember(key) { mutableStateOf(existing?.appointment?.notes ?: "") }
    var tip by remember(key) { mutableStateOf(existing?.appointment?.tipCents?.let { "%.2f".format(it / 100.0) } ?: "") }
    var popupError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showServicePicker by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Nueva cita" else "Editar cita") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Clienta"); ClientPicker(clients, clientId) { clientId = it; popupError = null } }
            item { OutlinedButton({ showDatePicker = true }, Modifier.fillMaxWidth()) { Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("Fecha: ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}") }; OutlinedButton({ showTimePicker = true }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Schedule, null); Spacer(Modifier.width(8.dp)); Text("Hora: ${time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale("es")))}") } }
            item {
                Text("Servicios", fontWeight = FontWeight.Bold)
                OutlinedButton({ showServicePicker = true }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selected.isEmpty()) "Añadir servicios" else "Servicios seleccionados (${selected.size})")
                }
            }
            items(services.filter { selected.contains(it.id) }) { service ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ServiceIcon(service.icon)
                        Text(service.name, modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(priceText[service.id] ?: service.basePriceCents.let { "%.2f".format(it / 100.0) }, { priceText[service.id] = it }, label = { Text("Precio para esta cita (€)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
            item { OutlinedTextField(tip, { tip = it }, label = { Text("Propina (€)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(notes, { notes = it }, label = { Text("Notas del servicio") }, modifier = Modifier.fillMaxWidth()) }
        }
    }, confirmButton = { Button(onClick = {
        popupError = when {
            clientId == 0L && selected.isEmpty() -> "Elige una clienta y un servicio"
            clientId == 0L -> "Elige una clienta"
            selected.isEmpty() -> "Elige al menos un servicio"
            else -> null
        }
        if (popupError == null) save(clientId, date, time, services.filter { selected.contains(it.id) }, priceText.mapValues { cents(it.value) }, notes, cents(tip)) { popupError = it }
    }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
    if (showDatePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("Aceptar") } }) { DatePicker(picker) }
    }
    if (showTimePicker) {
        val picker = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = false)
        AlertDialog(onDismissRequest = { showTimePicker = false }, title = { Text("Selecciona la hora") }, text = { TimePicker(picker) }, confirmButton = { TextButton(onClick = { time = LocalTime.of(picker.hour, picker.minute); showTimePicker = false }) { Text("Aceptar") } }, dismissButton = { TextButton({ showTimePicker = false }) { Text("Cancelar") } })
    }
    if (showServicePicker) {
        ServicePickerDialog(services, selected, { selected = it; popupError = null }) { showServicePicker = false }
    }
    popupError?.let { message ->
        AlertDialog(onDismissRequest = { popupError = null }, confirmButton = { TextButton({ popupError = null }) { Text("Entendido") } }, title = { Text("Revisa la cita") }, text = { Text(message) })
    }
}

@Composable
private fun ServicePickerDialog(services: List<ServiceEntity>, selected: Set<Long>, onSelectedChange: (Set<Long>) -> Unit, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Añadir servicios") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(services) { service ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelectedChange(selected.toggle(service.id)) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(selected.contains(service.id), { onSelectedChange(selected.toggle(service.id)) })
                        ServiceIcon(service.icon)
                        Column(Modifier.padding(start = 6.dp)) {
                            Text(service.name, fontWeight = FontWeight.Bold)
                            Text(service.basePriceCents.money(), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(dismiss) { Text("Listo") } }
    )
}

@Composable
private fun ClientPicker(clients: List<ClientEntity>, selected: Long, select: (Long) -> Unit) { var expanded by remember { mutableStateOf(false) }; Box { OutlinedButton({ expanded = true }, Modifier.fillMaxWidth()) { Text(clients.find { it.id == selected }?.name ?: "Selecciona una clienta") }; DropdownMenu(expanded, { expanded = false }) { clients.filter { it.isActive || it.id == selected }.forEach { DropdownMenuItem({ Text(it.name) }, { select(it.id); expanded = false }) } } } }
private fun Set<Long>.toggle(id: Long) = if (contains(id)) minus(id) else plus(id)
private fun validClientName(value: String) = Regex("^[\\p{L}][\\p{L} .'-]{1,59}$").matches(value.trim())
private fun validPhone(value: String): Boolean = Regex("^\\+?[0-9]{7,15}$").matches(value.trim().replace(Regex("[ ()-]"), ""))

@Composable
fun ClientsScreen(clients: List<ClientEntity>, save: (ClientEntity) -> Unit, setActive: (ClientEntity, Boolean) -> Unit) {
    var editing by remember { mutableStateOf<ClientEntity?>(null) }
    var changingStatus by remember { mutableStateOf<ClientEntity?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val filteredClients = clients.filter { query.isBlank() || it.name.contains(query, true) || it.phone.contains(query) }
    val activeClients = filteredClients.filter { it.isActive }
    val inactiveClients = filteredClients.filterNot { it.isActive }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Clientas", style = MaterialTheme.typography.headlineSmall); FilledTonalButton({ editing = ClientEntity(name = "") }) { Icon(Icons.Default.PersonAdd, null); Text(" Añadir") } }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 6.dp), singleLine = true, textStyle = MaterialTheme.typography.bodySmall, leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) }, label = { Text("Buscar por nombre o teléfono", style = MaterialTheme.typography.labelSmall) })
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Activas (${activeClients.size})", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp)) }
            item { ClientGrid(activeClients, isActive = true, onEdit = { editing = it }, onStatus = { changingStatus = it }) }
            if (inactiveClients.isNotEmpty()) {
                item { Text("Desactivadas (${inactiveClients.size})", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp)) }
                item { ClientGrid(inactiveClients, isActive = false, onEdit = { editing = it }, onStatus = { changingStatus = it }) }
            }
        }
    }
    editing?.let { client -> ClientDialog(client, { editing = null }) { save(it); editing = null } }
    changingStatus?.let { client ->
        val targetActive = !client.isActive
        AlertDialog(onDismissRequest = { changingStatus = null }, title = { Text(if (targetActive) "Reactivar clienta" else "Desactivar clienta") }, text = { Text(if (targetActive) "${client.name} volverá a aparecer al crear citas." else "${client.name} se conservará con todo su historial, pero no aparecerá para nuevas citas.") }, confirmButton = { Button({ setActive(client, targetActive); changingStatus = null }) { Text(if (targetActive) "Reactivar" else "Desactivar") } }, dismissButton = { TextButton({ changingStatus = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun ClientGrid(clients: List<ClientEntity>, isActive: Boolean, onEdit: (ClientEntity) -> Unit, onStatus: (ClientEntity) -> Unit) {
    val rowCount = (clients.size + 1) / 2
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height((rowCount * 102).dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), userScrollEnabled = false) {
        items(clients) { client ->
            Card(Modifier.fillMaxWidth().height(94.dp).clickable { onEdit(client) }) {
                Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(client.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        IconButton({ onStatus(client) }, Modifier.size(28.dp)) { Icon(if (isActive) Icons.Default.PersonOff else Icons.Default.PersonAdd, if (isActive) "Desactivar" else "Reactivar", modifier = Modifier.size(18.dp)) }
                    }
                    Text(client.phone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (client.contactDetails.isNotBlank()) Text(client.contactDetails, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ClientDialog(existing: ClientEntity, dismiss: () -> Unit, save: (ClientEntity) -> Unit) {
    var name by remember(existing.id) { mutableStateOf(existing.name) }; var phone by remember(existing.id) { mutableStateOf(existing.phone) }; var contact by remember(existing.id) { mutableStateOf(existing.contactDetails) }; var notes by remember(existing.id) { mutableStateOf(existing.notes) }; var attempted by remember { mutableStateOf(false) }
    val invalidName = attempted && !validClientName(name); val invalidPhone = attempted && !validPhone(phone)
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (existing.id == 0L) "Nueva clienta" else "Editar clienta") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Nombre y apellido *") }, isError = invalidName, supportingText = { if (invalidName) Text("Indica nombre y apellido válidos") }); OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono *") }, isError = invalidPhone, supportingText = { if (invalidPhone) Text("Usa entre 7 y 15 dígitos") }); OutlinedTextField(contact, { contact = it }, label = { Text("Otros datos de contacto") }); OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }) } }, confirmButton = { Button({ attempted = true; if (validClientName(name) && validPhone(phone)) save(existing.copy(name = name, phone = phone, contactDetails = contact, notes = notes)) }) { Text("Guardar") } }, dismissButton = { TextButton(dismiss) { Text("Cancelar") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServicesScreen(services: List<ServiceEntity>, save: (ServiceEntity) -> Unit, move: (ServiceEntity, Int) -> Unit, delete: (ServiceEntity, (String?) -> Unit) -> Unit) {
    var editing by remember { mutableStateOf<ServiceEntity?>(null) }; var deleting by remember { mutableStateOf<ServiceEntity?>(null) }; var message by remember { mutableStateOf<String?>(null) }; var showCatalog by remember { mutableStateOf(false) }; var selectedEmoji by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var catalogMessage by remember(services) { mutableStateOf(buildServiceCatalog(services)) }
    val emojiFilters = remember(services) { services.map { serviceEmoji(it.icon) }.distinct() }
    val visibleServices = services.filter { selectedEmoji == null || serviceEmoji(it.icon) == selectedEmoji }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Servicios", style = MaterialTheme.typography.headlineSmall); FilledTonalButton({ editing = ServiceEntity(name = "", basePriceCents = 0) }) { Icon(Icons.Default.Add, null); Text(" Añadir") } }
        OutlinedButton({ showCatalog = true }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(8.dp)); Text("Compartir catálogo de servicios") }
        if (emojiFilters.isNotEmpty()) FlowRow(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(selectedEmoji == null, { selectedEmoji = null }, { Text("Todos") })
            emojiFilters.forEach { emoji -> FilterChip(selectedEmoji == emoji, { selectedEmoji = if (selectedEmoji == emoji) null else emoji }, { Text(emoji) }) }
        }
        Text("Usa las flechas para ordenar el catálogo.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 6.dp)) }
        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(visibleServices.size) { i -> val service = visibleServices[i]; val orderIndex = services.indexOfFirst { it.id == service.id }; Card(Modifier.fillMaxWidth().clickable { editing = service }) { Row(Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { ServiceIcon(service.icon); Column(Modifier.weight(1f).padding(start = 6.dp)) { Text(service.name, fontWeight = FontWeight.Bold); Text(service.basePriceCents.money()) }; Column { IconButton({ move(service, -1) }, enabled = orderIndex > 0) { Icon(Icons.Default.KeyboardArrowUp, "Subir") }; IconButton({ move(service, 1) }, enabled = orderIndex in 0 until services.lastIndex) { Icon(Icons.Default.KeyboardArrowDown, "Bajar") } }; IconButton({ editing = service }) { Icon(Icons.Default.Edit, "Editar") }; IconButton({ deleting = service }) { Icon(Icons.Default.Delete, "Eliminar") } } } } }
    }
    editing?.let { service -> ServiceDialog(service, { editing = null }) { save(it); editing = null } }
    deleting?.let { service -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Eliminar servicio") }, text = { Text("¿Eliminar ${service.name}? Esta acción no se puede deshacer.") }, confirmButton = { Button({ delete(service) { error -> message = error; if (error == null) deleting = null } }) { Text("Eliminar") } }, dismissButton = { TextButton({ deleting = null }) { Text("Cancelar") } }) }
    if (showCatalog) AlertDialog(onDismissRequest = { showCatalog = false }, title = { Text("Editar catálogo antes de compartir") }, text = { OutlinedTextField(catalogMessage, { catalogMessage = it }, modifier = Modifier.fillMaxWidth(), minLines = 8, maxLines = 14, label = { Text("Mensaje para la clienta") }) }, confirmButton = { Button({ context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, catalogMessage), "Compartir catálogo")); showCatalog = false }) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Compartir") } }, dismissButton = { TextButton({ showCatalog = false }) { Text("Cerrar") } })
}

private fun buildServiceCatalog(services: List<ServiceEntity>) = buildString {
    appendLine("Erika Nail Art servicios")
    appendLine()
    services.forEach { appendLine("${serviceEmoji(it.icon)} ${it.name}: ${it.basePriceCents.money()}") }
    appendLine()
    appendLine("En los precios incluye la decoración (preguntar precios para decoración muy trabajada)")
    appendLine()
    append("Cualquier duda que tengas, aquí estoy para ayudarte 💕")
}

@Composable
private fun ServiceDialog(existing: ServiceEntity, dismiss: () -> Unit, save: (ServiceEntity) -> Unit) {
    var name by remember(existing.id) { mutableStateOf(existing.name) }
    var price by remember(existing.id) { mutableStateOf(if (existing.basePriceCents == 0L) "" else "%.2f".format(existing.basePriceCents / 100.0)) }
    var emoji by remember(existing.id) { mutableStateOf(serviceEmoji(existing.icon)) }
    var invalid by remember { mutableStateOf(false) }
    val emojis = listOf("💅", "🦶", "✨", "🎨", "🌸", "👁️", "👀", "💋", "💄", "💆")
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (existing.id == 0L) "Nuevo servicio" else "Editar servicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, isError = invalid && name.isBlank())
                OutlinedTextField(price, { price = it }, label = { Text("Precio (€)") }, isError = invalid && cents(price) <= 0)
                Text("Icono del servicio")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    emojis.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            row.forEach { option ->
                                FilterChip(
                                    selected = emoji == option,
                                    onClick = { emoji = option },
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                    label = { Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) { Text(option) } }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button({
                invalid = true
                if (name.isNotBlank() && cents(price) > 0) save(existing.copy(name = name.trim(), icon = emoji, basePriceCents = cents(price)))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(dismiss) { Text("Cancelar") } }
    )
}

private enum class IncomeRange { TODAY, MONTH, PERIOD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(vm: AgendaViewModel, clients: List<ClientEntity>) {
    val today = LocalDate.now(); var range by remember { mutableStateOf(IncomeRange.MONTH) }; var selectedMonth by remember { mutableStateOf(YearMonth.now()) }; var periodStart by remember { mutableStateOf(today.minusDays(30)) }; var periodEnd by remember { mutableStateOf(today) }; var showMonthPicker by remember { mutableStateOf(false) }; var periodDateToEdit by remember { mutableStateOf<Boolean?>(null) }; var periodWarning by remember { mutableStateOf<String?>(null) }
    val from = when (range) { IncomeRange.TODAY -> today; IncomeRange.MONTH -> selectedMonth.atDay(1); IncomeRange.PERIOD -> periodStart }; val until = when (range) { IncomeRange.TODAY -> today; IncomeRange.MONTH -> selectedMonth.atEndOfMonth(); IncomeRange.PERIOD -> periodEnd }; val paid by vm.income(from, until).collectAsState(initial = emptyList()); val serviceTotal = paid.sumOf { it.serviceTotalCents }; val tipsTotal = paid.sumOf { it.appointment.tipCents }; val total = serviceTotal + tipsTotal
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ingresos", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { FilterChip(range == IncomeRange.TODAY, { range = IncomeRange.TODAY }, { Text("Hoy") }); FilterChip(range == IncomeRange.MONTH, { range = IncomeRange.MONTH; showMonthPicker = true }, { Text("Mes") }); FilterChip(range == IncomeRange.PERIOD, { range = IncomeRange.PERIOD }, { Text("Periodo") }) }
        if (range == IncomeRange.MONTH) Text(selectedMonth.displayMonthYear(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { showMonthPicker = true }.padding(top = 2.dp, bottom = 0.dp))
        if (range == IncomeRange.PERIOD) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ periodDateToEdit = true; periodWarning = null }) { Text("Desde ${periodStart.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}") }
            OutlinedButton({ periodDateToEdit = false; periodWarning = null }) { Text("Hasta ${periodEnd.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}") }
        }
        periodWarning?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
        Card(Modifier.padding(top = 6.dp, bottom = 10.dp).fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Servicios", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(serviceTotal.money(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    if (tipsTotal > 0) Text("(Propinas ${tipsTotal.money()})", color = Color(0xFF388E3C), style = MaterialTheme.typography.labelMedium)
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Total cobrado", style = MaterialTheme.typography.labelLarge)
                Text(total.money(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(paid.size) { i -> val item = paid[i]; Card { Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(item.client.name, fontWeight = FontWeight.Bold); Text(Instant.ofEpochMilli(item.appointment.startAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()); Text(item.services.joinToString { it.serviceNameSnapshot }, style = MaterialTheme.typography.bodySmall) }; Column(horizontalAlignment = Alignment.End) { Text(item.serviceTotalCents.money(), color = MaterialTheme.colorScheme.primary); if (item.appointment.tipCents > 0) Text("(Propina ${item.appointment.tipCents.money()})", color = Color(0xFF388E3C), style = MaterialTheme.typography.labelSmall) } } } } }
    }
    if (showMonthPicker) MonthPickerDialog(selectedMonth, { selectedMonth = it; showMonthPicker = false }, { showMonthPicker = false })
    periodDateToEdit?.let { editingStart ->
        val current = if (editingStart) periodStart else periodEnd
        val picker = rememberDatePickerState(initialSelectedDateMillis = current.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { periodDateToEdit = null }, confirmButton = { TextButton({
            picker.selectedDateMillis?.let { millis ->
                val chosen = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                if (editingStart && chosen.isAfter(periodEnd)) periodWarning = "La fecha inicial no puede ser posterior a la final."
                else if (!editingStart && chosen.isBefore(periodStart)) periodWarning = "La fecha final no puede ser anterior a la inicial."
                else if (editingStart) periodStart = chosen else periodEnd = chosen
            }
            periodDateToEdit = null
        }) { Text("Aplicar") } }) { DatePicker(picker) }
    }
}

@Composable
private fun MonthPickerDialog(initial: YearMonth, confirm: (YearMonth) -> Unit, dismiss: () -> Unit) { var displayed by remember { mutableStateOf(initial) }; AlertDialog(onDismissRequest = dismiss, title = { Text("Elige un mes") }, text = { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { IconButton({ displayed = displayed.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, "Mes anterior") }; Text(displayed.displayMonthYear(), style = MaterialTheme.typography.titleMedium); IconButton({ displayed = displayed.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, "Mes siguiente") } } }, confirmButton = { TextButton({ confirm(displayed) }) { Text("Aplicar") } }, dismissButton = { TextButton(dismiss) { Text("Cancelar") } }) }

private fun YearMonth.displayMonthYear(): String = format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))).replaceFirstChar { character -> character.titlecase(Locale("es")) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AgendaViewModel) {
    val hour by vm.reminderHour.collectAsState()
    val lastBackupAt by vm.lastBackupAt.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    var testStarted by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var backupPreview by remember { mutableStateOf<BackupPreview?>(null) }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { vm.exportBackup(it) { message -> backupMessage = message } } }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { selectedUri ->
        pendingImport = selectedUri
        vm.previewBackup(selectedUri) { result -> result.onSuccess { backupPreview = it }.onFailure { error -> pendingImport = null; backupMessage = error.message ?: "No se pudo leer la copia de seguridad." } }
    } }
    LaunchedEffect(testStarted) {
        if (testStarted) {
            delay(3_000)
            testStarted = false
        }
    }
    val displayTime = LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("hh:mm a", Locale("es")))
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)
        Text("Aviso diario", style = MaterialTheme.typography.titleMedium)
        OutlinedButton({ showTimePicker = true }) { Icon(Icons.Default.Schedule, null); Spacer(Modifier.width(8.dp)); Text(displayTime) }
        Button({ vm.testDailyReminder(); testStarted = true }) { Icon(Icons.Default.Notifications, null); Spacer(Modifier.width(8.dp)); Text("Probar aviso ahora") }
        if (testStarted) Text("Enviando aviso de prueba…", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
        Text("Recibirás un aviso diario con tus citas o indicando que tienes el día libre.", style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Text("Copia de seguridad", style = MaterialTheme.typography.titleMedium)
        Text("Guarda tus clientas, servicios y citas para recuperarlos en otro teléfono.", style = MaterialTheme.typography.bodyMedium)
        val lastBackupText = lastBackupAt?.let { Instant.ofEpochMilli(it).atZone(AgendaTime.zone).format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm", Locale("es"))) }
        Text(lastBackupText?.let { "Última copia guardada: $it" } ?: "Aún no has guardado una copia en este dispositivo.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        OutlinedButton({ exportBackup.launch("erika-nail-art-backup.json") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Guardar copia de seguridad") }
        Button({ importBackup.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(8.dp)); Text("Importar copia de seguridad") }
        backupMessage?.let { Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium) }
    }
    if (showTimePicker) {
        val picker = rememberTimePickerState(initialHour = hour, initialMinute = 0, is24Hour = false)
        AlertDialog(onDismissRequest = { showTimePicker = false }, title = { Text("Hora del aviso") }, text = { TimePicker(picker) }, confirmButton = { TextButton({ vm.setReminderHour(picker.hour); showTimePicker = false }) { Text("Aceptar") } }, dismissButton = { TextButton({ showTimePicker = false }) { Text("Cancelar") } })
    }
    if (pendingImport != null && backupPreview != null) {
        val uri = pendingImport!!
        val preview = backupPreview!!
        val created = preview.createdAt?.let { Instant.ofEpochMilli(it).atZone(AgendaTime.zone).format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm", Locale("es"))) } ?: "fecha no disponible"
        AlertDialog(
            onDismissRequest = { pendingImport = null; backupPreview = null },
            title = { Text("Revisar copia de seguridad") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Copia creada: $created")
                Text("${preview.clients} ${if (preview.clients == 1) "clienta" else "clientas"} · ${preview.services} ${if (preview.services == 1) "servicio" else "servicios"} · ${preview.appointments} ${if (preview.appointments == 1) "cita" else "citas"}", fontWeight = FontWeight.Bold)
                Text("Al restaurarla se reemplazarán las clientas, los servicios y las citas que tienes ahora. Esta acción no se puede deshacer.")
            } },
            confirmButton = { Button({ vm.importBackup(uri) { message -> backupMessage = message }; pendingImport = null; backupPreview = null }) { Text("Restaurar") } },
            dismissButton = { TextButton({ pendingImport = null; backupPreview = null }) { Text("Cancelar") } }
        )
    }
}
