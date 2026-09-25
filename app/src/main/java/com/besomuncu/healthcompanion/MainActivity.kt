package com.besomuncu.healthcompanion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.besomuncu.healthcompanion.ui.screen.*
import com.besomuncu.healthcompanion.ui.component.AutoScrollingText
import com.besomuncu.healthcompanion.ui.theme.MyHealthCompanion2Theme
import com.besomuncu.healthcompanion.ui.viewmodel.HealthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> 
        if (!isGranted) {
            Toast.makeText(this, getString(R.string.error_notif_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val viewModel: HealthViewModel = viewModel()
            val allUsers by viewModel.allUsers.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val settings by viewModel.settings.collectAsState()

            val darkTheme = when (settings.theme) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
            }

            val context = LocalContext.current
            val locale = remember(settings.language) {
                if (settings.language == "Turkish") Locale("tr") else Locale("en")
            }
            
            val configuration = LocalConfiguration.current
            val localizedContext = remember(locale, configuration) {
                val config = Configuration(configuration)
                config.setLocale(locale)
                Locale.setDefault(locale)
                context.createConfigurationContext(config)
            }

            LaunchedEffect(locale) {
                val resources = context.resources
                val config = resources.configuration
                config.setLocale(locale)
                resources.updateConfiguration(config, resources.displayMetrics)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides (context as ActivityResultRegistryOwner)
            ) {
                MyHealthCompanion2Theme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(
                            density = LocalDensity.current.density,
                            fontScale = (LocalDensity.current.fontScale * settings.textSizeMultiplier).coerceAtMost(1.6f)
                        )
                    ) {
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val showOnboarding by viewModel.showOnboarding.collectAsState()
                            if (allUsers.isEmpty() || showOnboarding) {
                                OnboardingScreen(viewModel)
                            } else {
                                key(settings.language, settings.layoutMode) {
                                    val navigateTo = intent.getStringExtra("navigate_to")
                                    val medName = intent.getStringExtra("med_name")
                                    val clearMed = intent.getStringExtra("clear_med_notif")
                                    val targetMed = medName ?: clearMed
                                    
                                    if (targetMed != null) {
                                        clearMedNotifications(localizedContext, targetMed)
                                    }

                                    MainContent(viewModel, navigateTo)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val medName = intent.getStringExtra("med_name")
        val clearMed = intent.getStringExtra("clear_med_notif")
        val targetMed = medName ?: clearMed
        
        if (targetMed != null) {
            clearMedNotifications(this, targetMed)
        }
    }
}

fun clearMedNotifications(context: android.content.Context, medName: String) {
    val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    notificationManager.cancel(medName.hashCode())
    notificationManager.cancel(medName.hashCode() + 100)
}

@Composable
fun MainContent(viewModel: HealthViewModel, navigateTo: String?) {
    val settings by viewModel.settings.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var showUserDialog by remember { mutableStateOf(false) }
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var showEditUserDialog by remember { mutableStateOf<com.besomuncu.healthcompanion.data.model.User?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        pageCount = { if (settings.layoutMode == "Simplified") 6 else 5 }
    )

    // Back Button Logic
    BackHandler {
        if (settings.layoutMode == "Simplified") {
            if (pagerState.currentPage == 0) {
                showExitDialog = true
            } else {
                scope.launch { pagerState.scrollToPage(0) }
            }
        } else {
            if (pagerState.currentPage == 0) {
                showExitDialog = true
            } else {
                scope.launch { pagerState.animateScrollToPage(0) }
            }
        }
    }

    LaunchedEffect(navigateTo) {
        val targetPage = when (navigateTo) {
            "blood_pressure" -> if (settings.layoutMode == "Simplified") 4 else 3
            "dashboard" -> if (settings.layoutMode == "Simplified") 1 else 0
            else -> null
        }
        targetPage?.let { pagerState.scrollToPage(it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (settings.layoutMode == "Normal") {
                    NavigationBar {
                    bottomNavItems.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            modifier = Modifier.padding(horizontal = 0.dp),
                            icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes), modifier = Modifier.size(24.dp), tint = if (pagerState.currentPage == index) screen.iconColor else LocalContentColor.current) },
                            label = { 
                                AutoScrollingText(
                                    text = stringResource(screen.titleRes), 
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    color = if (pagerState.currentPage == index) screen.iconColor else Color.Unspecified
                                ) 
                            },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding),
            beyondViewportPageCount = 2,
            userScrollEnabled = false 
        ) { page ->
            val onBack = {
                scope.launch {
                    if (settings.layoutMode == "Simplified") {
                        pagerState.scrollToPage(0)
                    } else {
                        pagerState.animateScrollToPage(0)
                    }
                }
                Unit
            }
            
            if (settings.layoutMode == "Simplified") {
                when (page) {
                    0 -> SimplifiedMenu(viewModel, pagerState) { showUserDialog = true }
                    1 -> DashboardScreen(viewModel, onBack)
                    2 -> LiquidsScreen(viewModel, onBack)
                    3 -> MedicationScreen(viewModel, onBack)
                    4 -> BloodPressureScreen(viewModel, onBack)
                    5 -> SettingsScreen(viewModel, onBack)
                }
            } else {
                when (bottomNavItems[page]) {
                    Screen.Dashboard -> DashboardScreen(viewModel, onSwitchUser = { showUserDialog = true })
                    Screen.Liquids -> LiquidsScreen(viewModel)
                    Screen.Medications -> MedicationScreen(viewModel)
                    Screen.BloodPressure -> BloodPressureScreen(viewModel)
                    Screen.Settings -> SettingsScreen(viewModel)
                }
            }
        }
    }

    // Shared UI Logic (Resized Dialogs)
    val density = LocalDensity.current
    val customDensity = remember(density, settings.textSizeMultiplier) {
        val targetScale = if (settings.textSizeMultiplier >= 1.2f) 1.2f else settings.textSizeMultiplier
        val currentScale = settings.textSizeMultiplier
        val correction = if (currentScale > 0) targetScale / currentScale else 1f
        Density(density = density.density, fontScale = density.fontScale * correction)
    }

    if (showExitDialog) {
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text(stringResource(R.string.exit_title)) },
                text = { Text(stringResource(R.string.exit_message)) },
                confirmButton = {
                    Button(onClick = { 
                        (context as? Activity)?.finishAffinity()
                        exitProcess(0)
                    }) {
                        Text(stringResource(R.string.btn_exit))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
                }
            )
        }
    }

    if (showUserDialog) {
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { showUserDialog = false },
                title = { Text(stringResource(R.string.dialog_switch_user)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        allUsers.forEach { u ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { 
                                    viewModel.selectUser(u)
                                    showUserDialog = false 
                                }, modifier = Modifier.weight(1f)) {
                                    Text(u.name, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                                }
                                IconButton(onClick = { 
                                    showEditUserDialog = u
                                    showUserDialog = false
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit User Name")
                                }
                                IconButton(onClick = { viewModel.removeUser(u) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_user), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Button(
                            onClick = { 
                                viewModel.setShowOnboarding(true)
                                showUserDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.btn_create_user))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showUserDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
                }
            )
        }
    }

    if (showCreateUserDialog) {
        var name by remember { mutableStateOf("") }
        var weight by remember { mutableStateOf("") }
        var height by remember { mutableStateOf("") }
        var waterGoal by remember { mutableStateOf("") }
        
        val recommended = remember(weight) {
            val w = weight.toFloatOrNull() ?: 0f
            if (w > 0) (w * 35).toInt() else 2000
        }

        LaunchedEffect(recommended) {
            if (waterGoal.isEmpty() || waterGoal == "2000") waterGoal = recommended.toString()
        }

        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { showCreateUserDialog = false },
                title = { Text(stringResource(R.string.btn_create_user)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.new_user_name)) }, modifier = Modifier.fillMaxWidth())
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = weight, 
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() || it == '.' }) weight = input
                                }, 
                                label = { Text(stringResource(R.string.weight_hint) + " " + stringResource(R.string.optional_hint)) }, 
                                placeholder = { Text(stringResource(R.string.optional_hint), color = Color.Gray) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = height, 
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() || it == '.' }) height = input
                                }, 
                                label = { Text(stringResource(R.string.height_hint) + " " + stringResource(R.string.optional_hint)) }, 
                                placeholder = { Text(stringResource(R.string.optional_hint), color = Color.Gray) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = waterGoal, 
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) waterGoal = input
                            }, 
                            label = { Text(stringResource(R.string.water_goal_hint)) }, 
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text(stringResource(R.string.recommended_goal, recommended)) }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addUser(name, weight.toFloatOrNull() ?: 70f, height.toFloatOrNull() ?: 170f, waterGoal.toIntOrNull() ?: recommended)
                            showCreateUserDialog = false
                        }
                    }) { Text(stringResource(R.string.btn_add_user)) }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateUserDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
                }
            )
        }
    }

    if (showEditUserDialog != null) {
        var newName by remember { mutableStateOf(showEditUserDialog!!.name) }
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { showEditUserDialog = null },
                title = { Text("Edit User Name") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.updateUserName(showEditUserDialog!!, newName)
                            showEditUserDialog = null
                        }
                    }) { Text(stringResource(R.string.btn_save)) }
                },
                dismissButton = {
                    TextButton(onClick = { showEditUserDialog = null }) { Text(stringResource(R.string.btn_cancel)) }
                }
            )
        }
    }
}

@Composable
fun SimplifiedMenu(viewModel: HealthViewModel, pagerState: PagerState, onSwitchUser: () -> Unit) {
    val user by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()

    val density = LocalDensity.current
    val customDensity = remember(density, settings.textSizeMultiplier) {
        val targetScale = if (settings.textSizeMultiplier >= 1.2f) 1.2f else settings.textSizeMultiplier
        val currentScale = settings.textSizeMultiplier
        val correction = if (currentScale > 0) targetScale / currentScale else 1f
        Density(density = density.density, fontScale = density.fontScale * correction)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome Header
        CompositionLocalProvider(LocalDensity provides customDensity) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.welcome_guest, user?.name ?: ""),
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onSwitchUser) {
                    Icon(Icons.Default.Person, contentDescription = stringResource(R.string.switch_user))
                }
            }
        }

        // CORRECT MAPPINGS FOR SIMPLIFIED LAYOUT (Step 3 Retry)
        // Menu -> 0
        // Dashboard -> 1
        // Liquids -> 2
        // Meds -> 3
        // BP -> 4
        // Settings -> 5
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(min = 500.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MenuCard(Screen.Dashboard, Screen.Dashboard.iconColor, Modifier.weight(1f).fillMaxHeight()) {
                    scope.launch { pagerState.scrollToPage(1) }
                }
                MenuCard(Screen.Liquids, Screen.Liquids.iconColor, Modifier.weight(1f).fillMaxHeight()) {
                    scope.launch { pagerState.scrollToPage(2) }
                }
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MenuCard(Screen.Medications, Screen.Medications.iconColor, Modifier.weight(1f).fillMaxHeight()) {
                    scope.launch { pagerState.scrollToPage(3) }
                }
                MenuCard(Screen.BloodPressure, Screen.BloodPressure.iconColor, Modifier.weight(1f).fillMaxHeight()) {
                    scope.launch { pagerState.scrollToPage(4) }
                }
            }
        }
        
        // Settings at the bottom
        MenuCard(Screen.Settings, Screen.Settings.iconColor, Modifier.fillMaxWidth().height(140.dp)) {
            scope.launch { pagerState.scrollToPage(5) }
        }
    }
}

@Composable
fun MenuCard(screen: Screen, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = null,
                modifier = Modifier.size(if (LocalDensity.current.fontScale > 1.3f) 48.dp else 64.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            AutoScrollingText(
                text = stringResource(screen.titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}



