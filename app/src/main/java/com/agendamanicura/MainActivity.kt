package com.agendamanicura

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agendamanicura.ui.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AgendaViewModel by viewModels()
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { AgendaTheme { AgendaApp(viewModel) } }
    }
}

private val Pink = Color(0xFFA44264)
private val Cream = Color(0xFFFFF8F9)
@Composable fun AgendaTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = lightColorScheme(primary = Pink, surface = Cream, secondary = Color(0xFF7C5360)), content = content)

@Composable
fun AgendaApp(vm: AgendaViewModel) {
    var screen by rememberSaveable { mutableStateOf("agenda") }
    val clients by vm.clients.collectAsStateWithLifecycle(); val services by vm.services.collectAsStateWithLifecycle()
    Scaffold(bottomBar = { NavigationBar { listOf("agenda" to "Agenda", "clientes" to "Clientas", "servicios" to "Servicios", "ingresos" to "Ingresos", "ajustes" to "Ajustes").forEach { (id, label) -> NavigationBarItem(selected = screen == id, onClick = { screen = id }, icon = { Icon(if (id == "agenda") Icons.Default.CalendarMonth else if (id == "clientes") Icons.Default.Person else if (id == "servicios") Icons.Default.Spa else if (id == "ingresos") Icons.Default.AttachMoney else Icons.Default.Settings, label) }, label = { Text(label) }) } } }) { padding ->
        Surface(modifier = androidx.compose.ui.Modifier.padding(padding)) { when (screen) {
            "agenda" -> CalendarScreen(vm, clients, services)
            "clientes" -> ClientsScreen(clients, vm::saveClient)
            "servicios" -> ServicesScreen(services, vm::saveService)
            "ingresos" -> IncomeScreen(vm, clients)
            else -> SettingsScreen(vm)
        } }
    }
}
