package com.agendamanicura.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
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
            Text("${month.month.getDisplayName(TextStyle.FULL, Locale("es"))} ${month.year}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
        OutlinedTextField(appointmentQuery, { appointmentQuery = it }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("Buscar por clienta o servicio") })
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
@Composable fun ServiceIcon(icon: String, size: androidx.compose.ui.unit.TextUnit = 19.2.sp) = Text(serviceEmoji(icon), modifier = Modifier.padding(end = 3.dp), fontSize = size)

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
            Row {
                IconButton({ edit(item) }) { Icon(Icons.Default.Edit, "Editar cita") }
                when (item.appointment.status) {
                    AppointmentStatus.PENDING -> { IconButton({ setStatus(item.appointment.id, AppointmentStatus.PAID) }) { Icon(Icons.Default.CheckCircle, "Marcar como cobrada") }; IconButton({ setStatus(item.appointment.id, AppointmentStatus.CANCELLED) }) { Icon(Icons.Default.Cancel, "Cancelar") }; IconButton({ delete(item) }) { Icon(Icons.Default.Delete, "Eliminar") } }
                    AppointmentStatus.PAID -> { IconButton({ setStatus(item.appointment.id, AppointmentStatus.PENDING) }) { Icon(Icons.Default.Undo, "Volver a creada") }; IconButton({ setStatus(item.appointment.id, AppointmentStatus.CANCELLED) }) { Icon(Icons.Default.Cancel, "Cancelar") }; IconButton({ delete(item) }) { Icon(Icons.Default.Delete, "Eliminar") } }
                    AppointmentStatus.CANCELLED -> { IconButton({ setStatus(item.appointment.id, AppointmentStatus.PENDING) }) { Icon(Icons.Default.Undo, "Volver a creada") }; IconButton({ delete(item) }) { Icon(Icons.Default.Delete, "Eliminar") } }
                }
            }
        }
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
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Nueva cita" else "Editar cita") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Clienta"); ClientPicker(clients, clientId) { clientId = it; popupError = null } }
            item { OutlinedButton({ showDatePicker = true }, Modifier.fillMaxWidth()) { Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("Fecha: ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}") }; OutlinedButton({ showTimePicker = true }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Schedule, null); Spacer(Modifier.width(8.dp)); Text("Hora: ${time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale("es")))}") } }
            item { Text("Servicios", fontWeight = FontWeight.Bold) }
            items(services.size) { i -> val service = services[i]; Column { Row(Modifier.fillMaxWidth().clickable { selected = selected.toggle(service.id); popupError = null }, verticalAlignment = Alignment.CenterVertically) { Checkbox(selected.contains(service.id), { selected = selected.toggle(service.id); popupError = null }); ServiceIcon(service.icon); Text("${service.name} · ${service.basePriceCents.money()}", modifier = Modifier.padding(start = 6.dp)) }; if (selected.contains(service.id)) OutlinedTextField(priceText[service.id] ?: service.basePriceCents.let { "%.2f".format(it / 100.0) }, { priceText[service.id] = it }, label = { Text("Precio para esta cita (€)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) } }
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
    popupError?.let { message ->
        AlertDialog(onDismissRequest = { popupError = null }, confirmButton = { TextButton({ popupError = null }) { Text("Entendido") } }, title = { Text("Revisa la cita") }, text = { Text(message) })
    }
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
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("Buscar por nombre o teléfono") })
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

@Composable
fun ServicesScreen(services: List<ServiceEntity>, save: (ServiceEntity) -> Unit, delete: (ServiceEntity, (String?) -> Unit) -> Unit) {
    var editing by remember { mutableStateOf<ServiceEntity?>(null) }; var deleting by remember { mutableStateOf<ServiceEntity?>(null) }; var message by remember { mutableStateOf<String?>(null) }; var showCatalog by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val catalogMessage = remember(services) { buildString { appendLine("Erika Nail Art servicios"); appendLine(); services.forEach { appendLine("${serviceEmoji(it.icon)} ${it.name}: ${it.basePriceCents.money()}") }; appendLine(); append("Cualquier duda que tengas, aquí estoy para ayudarte 💕") } }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Servicios", style = MaterialTheme.typography.headlineSmall); FilledTonalButton({ editing = ServiceEntity(name = "", basePriceCents = 0) }) { Icon(Icons.Default.Add, null); Text(" Añadir") } }
        OutlinedButton({ showCatalog = true }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(8.dp)); Text("Compartir catálogo de servicios") }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("Buscar servicio") })
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 6.dp)) }
        val filteredServices = services.filter { query.isBlank() || it.name.contains(query, true) }
        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filteredServices.size) { i -> val service = filteredServices[i]; Card(Modifier.fillMaxWidth().clickable { editing = service }) { Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { ServiceIcon(service.icon); Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(service.name, fontWeight = FontWeight.Bold); Text(service.basePriceCents.money()) }; IconButton({ editing = service }) { Icon(Icons.Default.Edit, "Editar") }; IconButton({ deleting = service }) { Icon(Icons.Default.Delete, "Eliminar") } } } } }
    }
    editing?.let { service -> ServiceDialog(service, { editing = null }) { save(it); editing = null } }
    deleting?.let { service -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Eliminar servicio") }, text = { Text("¿Eliminar ${service.name}? Esta acción no se puede deshacer.") }, confirmButton = { Button({ delete(service) { error -> message = error; if (error == null) deleting = null } }) { Text("Eliminar") } }, dismissButton = { TextButton({ deleting = null }) { Text("Cancelar") } }) }
    if (showCatalog) AlertDialog(onDismissRequest = { showCatalog = false }, title = { Text("Catálogo para compartir") }, text = { Text(catalogMessage) }, confirmButton = { Button({ context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, catalogMessage), "Compartir catálogo")); showCatalog = false }) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Compartir") } }, dismissButton = { TextButton({ showCatalog = false }) { Text("Cerrar") } })
}

@Composable
private fun ServiceDialog(existing: ServiceEntity, dismiss: () -> Unit, save: (ServiceEntity) -> Unit) {
    var name by remember(existing.id) { mutableStateOf(existing.name) }; var price by remember(existing.id) { mutableStateOf(if (existing.basePriceCents == 0L) "" else "%.2f".format(existing.basePriceCents / 100.0)) }; var emoji by remember(existing.id) { mutableStateOf(serviceEmoji(existing.icon)) }; var invalid by remember { mutableStateOf(false) }
    val emojis = listOf("💅", "🦶", "✨", "🎨", "🌸", "👁️", "👀", "🪄", "💄", "💆")
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (existing.id == 0L) "Nuevo servicio" else "Editar servicio") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, isError = invalid && name.isBlank()); OutlinedTextField(price, { price = it }, label = { Text("Precio (€)") }, isError = invalid && cents(price) <= 0); Text("Icono del servicio"); Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { emojis.chunked(5).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { row.forEach { option -> FilterChip(emoji == option, { emoji = option }, { Text(option) }) } } } } } }, confirmButton = { Button({ invalid = true; if (name.isNotBlank() && cents(price) > 0) save(existing.copy(name = name.trim(), icon = emoji, basePriceCents = cents(price))) }) { Text("Guardar") } }, dismissButton = { TextButton(dismiss) { Text("Cancelar") } })
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
        if (range == IncomeRange.MONTH) Text(selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { showMonthPicker = true }.padding(vertical = 4.dp))
        if (range == IncomeRange.PERIOD) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ periodDateToEdit = true; periodWarning = null }) { Text("Desde ${periodStart.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}") }
            OutlinedButton({ periodDateToEdit = false; periodWarning = null }) { Text("Hasta ${periodEnd.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}") }
        }
        periodWarning?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
        Card(Modifier.padding(vertical = 16.dp).fillMaxWidth()) {
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
private fun MonthPickerDialog(initial: YearMonth, confirm: (YearMonth) -> Unit, dismiss: () -> Unit) { var displayed by remember { mutableStateOf(initial) }; AlertDialog(onDismissRequest = dismiss, title = { Text("Elige un mes") }, text = { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { IconButton({ displayed = displayed.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, "Mes anterior") }; Text(displayed.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))), style = MaterialTheme.typography.titleMedium); IconButton({ displayed = displayed.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, "Mes siguiente") } } }, confirmButton = { TextButton({ confirm(displayed) }) { Text("Aplicar") } }, dismissButton = { TextButton(dismiss) { Text("Cancelar") } }) }

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
