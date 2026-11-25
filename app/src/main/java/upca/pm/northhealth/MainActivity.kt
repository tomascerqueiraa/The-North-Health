package upca.pm.northhealth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import upca.pm.northhealth.ui.components.MessageToast
import upca.pm.northhealth.ui.components.Screen
import upca.pm.northhealth.ui.components.allScreens
import upca.pm.northhealth.ui.components.baseScreens
import upca.pm.northhealth.ui.components.rememberSpeaker
import upca.pm.northhealth.ui.home.AddExamScreen
import upca.pm.northhealth.ui.home.AddMedScreen
import upca.pm.northhealth.ui.home.AlarmSignsScreen
import upca.pm.northhealth.ui.home.ChecklistUiState
import upca.pm.northhealth.ui.home.ExamDetailScreen
import upca.pm.northhealth.ui.home.ExamDetailViewModel
import upca.pm.northhealth.ui.home.ExamsScreen
import upca.pm.northhealth.ui.home.HealthUiState
import upca.pm.northhealth.ui.home.HealthViewModel
import upca.pm.northhealth.ui.home.LoginScreen
import upca.pm.northhealth.ui.home.LoginViewModel
import upca.pm.northhealth.ui.home.MedDetailScreen
import upca.pm.northhealth.ui.home.MedsScreen
import upca.pm.northhealth.ui.home.ProfileScreen
import upca.pm.northhealth.ui.home.ReminderScreen
import upca.pm.northhealth.ui.home.SettingsViewModel
import upca.pm.northhealth.ui.home.TodayScreen
import upca.pm.northhealth.ui.theme.NorthHealthTheme

class MainActivity : ComponentActivity() {

    /** Destino pedido por uma notificação: (tipo, id). Ex.: ("intake", "abc123"). */
    private var target by mutableStateOf<Pair<String, String>?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* sem ação */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        // Android 13+: pedir permissão para mostrar notificações (lembretes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settings.collectAsState()

            NorthHealthTheme(fontScale = settings.fontScale, highContrast = settings.highContrast) {
                NorthHealthApp(
                    settingsViewModel = settingsViewModel,
                    target = target,
                    onTargetHandled = { target = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val kind = intent?.getStringExtra(EXTRA_KIND)
        val id = intent?.getStringExtra(EXTRA_ID)
        if (kind != null && id != null) {
            target = kind to id
            intent?.removeExtra(EXTRA_KIND)
            intent?.removeExtra(EXTRA_ID)
        }
    }
}

private data class BottomItem(val screen: Screen, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Screen.Today, Icons.Default.Home),
    BottomItem(Screen.Meds, Icons.Default.Favorite),
    BottomItem(Screen.Exams, Icons.Default.DateRange),
    BottomItem(Screen.Profile, Icons.Default.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NorthHealthApp(
    settingsViewModel: SettingsViewModel,
    target: Pair<String, String>?,
    onTargetHandled: () -> Unit
) {
    val navController = rememberNavController()
    val healthViewModel: HealthViewModel = viewModel() // partilhado por todos os ecrãs
    val settings by settingsViewModel.settings.collectAsState()
    val uiState by healthViewModel.uiState.collectAsState()
    val message by healthViewModel.message.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // uid != null significa que há sessão iniciada (currentUser)
    var uid by remember { mutableStateOf(FirebaseHelper.currentUid) }
    val startDestination = remember {
        if (FirebaseHelper.currentUid != null) Screen.Today.route else Screen.Login.route
    }

    val isLogin = currentRoute == Screen.Login.route
    val isReminder = currentRoute == Screen.Reminder.route
    val isBaseScreen = baseScreens.any { it.route == currentRoute }

    val data = (uiState as? HealthUiState.Success)?.data

    // Começa/termina a sincronização em tempo real conforme a sessão
    LaunchedEffect(uid) {
        val current = uid
        if (current != null) healthViewModel.start(current) else healthViewModel.stop()
    }

    // Abrir o ecrã certo quando o utilizador toca numa notificação
    LaunchedEffect(target, uid) {
        if (target != null && uid != null) {
            val (kind, id) = target
            when (kind) {
                KIND_INTAKE -> navController.navigate(Screen.Reminder.createRoute(id))
                KIND_EXAM -> navController.navigate(Screen.ExamDetail.createRoute(id))
            }
            onTargetHandled()
        }
    }

    MessageToast(message = message, onShown = healthViewModel::clearMessage)

    Scaffold(
        topBar = {
            if (!isLogin && !isReminder) {
                TopAppBar(
                    title = {
                        Text(allScreens.firstOrNull { it.route == currentRoute }?.title ?: "The North Health")
                    },
                    navigationIcon = {
                        if (!isBaseScreen) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isBaseScreen) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Today.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.screen.title) },
                            label = { Text(item.screen.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                val viewModel: LoginViewModel = viewModel()
                val loginState by viewModel.uiState.collectAsState()
                val goToToday = {
                    uid = FirebaseHelper.currentUid
                    navController.navigate(Screen.Today.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                    Unit
                }
                LoginScreen(
                    uiState = loginState,
                    onLogin = { email, password -> viewModel.login(email, password) { goToToday() } },
                    onRegister = { name, email, password, consent ->
                        viewModel.register(name, email, password, consent) { goToToday() }
                    }
                )
            }

            composable(Screen.Today.route) {
                TodayScreen(
                    uiState = uiState,
                    userName = FirebaseHelper.currentName,
                    onRetry = healthViewModel::retry,
                    onConfirmTaken = healthViewModel::confirmTaken,
                    onOpenIntake = { intake ->
                        navController.navigate(Screen.Reminder.createRoute(intake.id))
                    },
                    onOpenExam = { exam ->
                        navController.navigate(Screen.ExamDetail.createRoute(exam.id))
                    },
                    onOpenMeds = {
                        navController.navigate(Screen.Meds.route) {
                            popUpTo(Screen.Today.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenAlarmSigns = { navController.navigate(Screen.AlarmSigns.route) }
                )
            }

            composable(Screen.Meds.route) {
                MedsScreen(
                    uiState = uiState,
                    onRetry = healthViewModel::retry,
                    onOpenIntake = { intake ->
                        navController.navigate(Screen.Reminder.createRoute(intake.id))
                    },
                    onOpenMedication = { medication ->
                        navController.navigate(Screen.MedDetail.createRoute(medication.id))
                    },
                    onAddMedication = { navController.navigate(Screen.AddMed.route) }
                )
            }

            composable(Screen.Exams.route) {
                ExamsScreen(
                    uiState = uiState,
                    onRetry = healthViewModel::retry,
                    onOpen = { exam -> navController.navigate(Screen.ExamDetail.createRoute(exam.id)) },
                    onAdd = { navController.navigate(Screen.AddExam.route) }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    name = FirebaseHelper.currentName,
                    email = FirebaseHelper.currentEmail,
                    settings = settings,
                    onFontScale = settingsViewModel::setFontScale,
                    onHighContrast = settingsViewModel::setHighContrast,
                    onLogout = {
                        healthViewModel.stop()
                        FirebaseHelper.logout()
                        uid = null
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Today.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.AddMed.route) {
                AddMedScreen(
                    existing = data?.medications.orEmpty(),
                    onSave = { name, substance, form, dose, times, days ->
                        healthViewModel.addMedication(name, substance, form, dose, times, days) {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(Screen.AlarmSigns.route) {
                AlarmSignsScreen()
            }

            composable(
                route = Screen.MedDetail.route,
                arguments = listOf(navArgument(Screen.MedDetail.ARG_ID) { type = NavType.StringType })
            ) { entry ->
                val medId = entry.arguments?.getString(Screen.MedDetail.ARG_ID)
                val medication = data?.medications?.firstOrNull { it.id == medId }
                val speaker = rememberSpeaker()
                MedDetailScreen(
                    medication = medication,
                    speaker = speaker,
                    onDelete = {
                        medication?.let { healthViewModel.deleteMedication(it) }
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.AddExam.route) {
                AddExamScreen(
                    onSave = { type, place, dateTime ->
                        healthViewModel.addAppointment(type, place, dateTime) { appointmentId ->
                            // Substitui o ecrã de criação pela checklist de preparação
                            navController.navigate(Screen.ExamDetail.createRoute(appointmentId)) {
                                popUpTo(Screen.AddExam.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.ExamDetail.route,
                arguments = listOf(navArgument(Screen.ExamDetail.ARG_ID) { type = NavType.StringType })
            ) { entry ->
                val examId = entry.arguments?.getString(Screen.ExamDetail.ARG_ID)
                val viewModel: ExamDetailViewModel = viewModel()
                val checklist by viewModel.uiState.collectAsState()
                val examMessage by viewModel.message.collectAsState()
                val appointment = data?.appointments?.firstOrNull { it.id == examId }
                val speaker = rememberSpeaker()

                MessageToast(message = examMessage, onShown = viewModel::clearMessage)
                ExamDetailScreen(
                    appointment = appointment,
                    checklist = checklist,
                    speaker = speaker,
                    onToggle = viewModel::toggle,
                    onRetry = viewModel::startListening,
                    onDelete = {
                        appointment?.let { healthViewModel.deleteAppointment(it) }
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Reminder.route,
                arguments = listOf(navArgument(Screen.Reminder.ARG_ID) { type = NavType.StringType })
            ) { entry ->
                val intakeId = entry.arguments?.getString(Screen.Reminder.ARG_ID)
                val intake = data?.intakes?.firstOrNull { it.id == intakeId }
                val speaker = rememberSpeaker()
                ReminderScreen(
                    intake = intake,
                    speaker = speaker,
                    onTaken = {
                        intake?.let { healthViewModel.confirmTaken(it) }
                        navController.popBackStack()
                    },
                    onSnooze = {
                        intake?.let { healthViewModel.snooze(it) }
                        navController.popBackStack()
                    },
                    onSkip = {
                        intake?.let { healthViewModel.skip(it) }
                        navController.popBackStack()
                    },
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}
