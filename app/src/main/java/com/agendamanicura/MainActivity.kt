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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agendamanicura.ui.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AgendaViewModel by viewModels()
    private var agendaRequest by mutableIntStateOf(0)
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_OPEN_AGENDA, false)) agendaRequest++
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { AgendaTheme { AppEntry(viewModel, agendaRequest) } }
    }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); setIntent(intent); if (intent.getBooleanExtra(EXTRA_OPEN_AGENDA, false)) agendaRequest++ }
    companion object { const val EXTRA_OPEN_AGENDA = "com.agendamanicura.OPEN_AGENDA" }
}

private val Violet = Color(0xFF7259A5)
private val Sky = Color(0xFF4D91B7)
private val SoftViolet = Color(0xFFE9E0F7)
private val SoftSky = Color(0xFFDCEFF8)
private val WarmWhite = Color(0xFFFFFAFE)
private val BaseTypography = Typography()
private val AgendaTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.displayLarge.fontSize * 1.2f, lineHeight = BaseTypography.displayLarge.lineHeight * 1.2f, letterSpacing = 0.6.sp),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.displayMedium.fontSize * 1.2f, lineHeight = BaseTypography.displayMedium.lineHeight * 1.2f, letterSpacing = 0.6.sp),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.displaySmall.fontSize * 1.2f, lineHeight = BaseTypography.displaySmall.lineHeight * 1.2f, letterSpacing = 0.6.sp),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.headlineLarge.fontSize * 1.2f, lineHeight = BaseTypography.headlineLarge.lineHeight * 1.2f, letterSpacing = 0.5.sp),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.headlineMedium.fontSize * 1.2f, lineHeight = BaseTypography.headlineMedium.lineHeight * 1.2f, letterSpacing = 0.5.sp),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.headlineSmall.fontSize * 1.2f, lineHeight = BaseTypography.headlineSmall.lineHeight * 1.2f, letterSpacing = 0.5.sp),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.titleLarge.fontSize * 1.2f, lineHeight = BaseTypography.titleLarge.lineHeight * 1.2f, letterSpacing = 0.4.sp),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.titleMedium.fontSize * 1.2f, lineHeight = BaseTypography.titleMedium.lineHeight * 1.2f, letterSpacing = 0.4.sp),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.titleSmall.fontSize * 1.2f, lineHeight = BaseTypography.titleSmall.lineHeight * 1.2f, letterSpacing = 0.4.sp),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.bodyLarge.fontSize * 1.2f, lineHeight = BaseTypography.bodyLarge.lineHeight * 1.2f, letterSpacing = 0.4.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.bodyMedium.fontSize * 1.2f, lineHeight = BaseTypography.bodyMedium.lineHeight * 1.2f, letterSpacing = 0.4.sp),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.bodySmall.fontSize * 1.2f, lineHeight = BaseTypography.bodySmall.lineHeight * 1.2f, letterSpacing = 0.4.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.labelLarge.fontSize * 1.2f, lineHeight = BaseTypography.labelLarge.lineHeight * 1.2f, letterSpacing = 0.4.sp),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.labelMedium.fontSize * 1.2f, lineHeight = BaseTypography.labelMedium.lineHeight * 1.2f, letterSpacing = 0.4.sp),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = BaseTypography.labelSmall.fontSize * 1.2f, lineHeight = BaseTypography.labelSmall.lineHeight * 1.2f, letterSpacing = 0.35.sp)
)
private val AgendaShapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(26.dp))
@Composable fun AgendaTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = lightColorScheme(primary = Violet, onPrimary = Color.White, primaryContainer = SoftViolet, secondary = Sky, secondaryContainer = SoftSky, surface = WarmWhite, background = WarmWhite, surfaceVariant = Color(0xFFF0EAF4)), typography = AgendaTypography, shapes = AgendaShapes, content = content)

@Composable
fun AppEntry(vm: AgendaViewModel, agendaRequest: Int) {
    var showWelcome by remember { mutableStateOf(agendaRequest == 0) }
    if (showWelcome) WelcomeScreen { showWelcome = false } else AgendaApp(vm, agendaRequest)
}

@Composable
private fun WelcomeScreen(continueToAgenda: () -> Unit) {
    LaunchedEffect(Unit) { delay(2_800); continueToAgenda() }
    Box(Modifier.fillMaxSize()) {
        Image(painter = painterResource(R.drawable.welcome_background), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(Modifier.align(Alignment.Center).padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Erika Nail Art", fontFamily = FontFamily.Cursive, fontSize = 57.6.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB30B5E))
            Text("LATINA NAIL TECHNICIAN", letterSpacing = 3.sp, fontSize = 15.6.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B286E), modifier = Modifier.padding(top = 8.dp))
            Text("Manicure · Pedicure · Nail Art", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC32067), modifier = Modifier.padding(top = 22.dp))
        }
        Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 42.dp), color = Color.White.copy(alpha = 0.88f), shape = RoundedCornerShape(14.dp), shadowElevation = 3.dp) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Desarrollado por", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4B286E))
                Text("Michael Moncada", style = MaterialTheme.typography.titleSmall, color = Color(0xFFB30B5E))
            }
        }
    }
}

@Composable
fun AgendaApp(vm: AgendaViewModel, agendaRequest: Int = 0) {
    var screen by rememberSaveable { mutableStateOf("agenda") }
    LaunchedEffect(agendaRequest) { if (agendaRequest > 0) screen = "agenda" }
    val clients by vm.clients.collectAsStateWithLifecycle(); val services by vm.services.collectAsStateWithLifecycle()
    Scaffold(bottomBar = { NavigationBar { listOf("agenda" to "Agenda", "clientes" to "Clientas", "servicios" to "Servicios", "ingresos" to "Ingresos", "ajustes" to "Ajustes").forEach { (id, label) -> NavigationBarItem(selected = screen == id, onClick = { screen = id }, icon = { Icon(if (id == "agenda") Icons.Default.CalendarMonth else if (id == "clientes") Icons.Default.Person else if (id == "servicios") Icons.Default.Spa else if (id == "ingresos") Icons.Default.AttachMoney else Icons.Default.Settings, label) }, label = { Text(label) }) } } }) { padding ->
        Surface(modifier = androidx.compose.ui.Modifier.padding(padding)) { when (screen) {
            "agenda" -> CalendarScreen(vm, clients, services)
            "clientes" -> ClientsScreen(clients, vm::saveClient, vm::setClientActive)
            "servicios" -> ServicesScreen(services, vm::saveService, vm::moveService, vm::deleteService)
            "ingresos" -> IncomeScreen(vm, clients)
            else -> SettingsScreen(vm)
        } }
    }
}
