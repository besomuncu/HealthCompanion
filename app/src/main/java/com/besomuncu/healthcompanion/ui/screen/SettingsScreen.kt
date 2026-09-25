package com.besomuncu.healthcompanion.ui.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.besomuncu.healthcompanion.R
import com.besomuncu.healthcompanion.ui.viewmodel.HealthViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: HealthViewModel, onBack: (() -> Unit)? = null) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var pendingLanguage by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importData(context, it) }
    }

    val density = LocalDensity.current
    val customDensity = remember(density, settings.textSizeMultiplier) {
        val targetScale = if (settings.textSizeMultiplier >= 1.2f) 1.2f else settings.textSizeMultiplier
        val currentScale = settings.textSizeMultiplier
        val correction = if (currentScale > 0) targetScale / currentScale else 1f
        Density(density = density.density, fontScale = density.fontScale * correction)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            bottomBar = {
                if (settings.layoutMode == "Simplified" && onBack != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = Color.Transparent
                    ) {
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_back), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.nav_settings), 
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Appearance Group
                SettingsGroup(title = stringResource(R.string.settings_text_size)) {
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = settings.textSizeMultiplier == 0.8f, onClick = { viewModel.setTextSize(0.8f) }, label = { Text("S") })
                        FilterChip(selected = settings.textSizeMultiplier == 1.0f, onClick = { viewModel.setTextSize(1.0f) }, label = { Text("M") })
                        FilterChip(selected = settings.textSizeMultiplier == 1.2f, onClick = { viewModel.setTextSize(1.2f) }, label = { Text("L") })
                        FilterChip(selected = settings.textSizeMultiplier == 1.5f, onClick = { viewModel.setTextSize(1.5f) }, label = { Text("XL") })
                    }
                }

                SettingsGroup(title = stringResource(R.string.settings_theme)) {
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = settings.theme == "System", onClick = { viewModel.updateTheme("System") }, label = { Text(stringResource(R.string.theme_system)) })
                        FilterChip(selected = settings.theme == "Light", onClick = { viewModel.updateTheme("Light") }, label = { Text(stringResource(R.string.theme_light)) })
                        FilterChip(selected = settings.theme == "Dark", onClick = { viewModel.updateTheme("Dark") }, label = { 
                            Text(stringResource(R.string.theme_dark), softWrap = false, maxLines = 1) 
                        })
                    }
                }

                SettingsGroup(title = stringResource(R.string.settings_layout)) {
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = settings.layoutMode == "Normal", onClick = { viewModel.setLayoutMode("Normal") }, label = { Text(stringResource(R.string.layout_normal)) })
                        FilterChip(selected = settings.layoutMode == "Simplified", onClick = { viewModel.setLayoutMode("Simplified") }, label = { Text(stringResource(R.string.layout_simplified)) })
                    }
                }

                // Localization & Region Group
                SettingsGroup(title = stringResource(R.string.settings_language)) {
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.language == "English", 
                            onClick = { if (settings.language != "English") pendingLanguage = "English" }, 
                            label = { Text("English") }
                        )
                        FilterChip(
                            selected = settings.language == "Turkish", 
                            onClick = { if (settings.language != "Turkish") pendingLanguage = "Turkish" }, 
                            label = { Text("Türkçe") }
                        )
                    }
                }

                SettingsGroup(title = stringResource(R.string.settings_date_format)) {
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = settings.dateFormat == "DD/MM/YYYY", onClick = { viewModel.updateDateFormat("DD/MM/YYYY") }, label = { Text(stringResource(R.string.date_format_dmy)) })
                        FilterChip(selected = settings.dateFormat == "MM/DD/YYYY", onClick = { viewModel.updateDateFormat("MM/DD/YYYY") }, label = { Text(stringResource(R.string.date_format_mdy)) })
                    }
                }

                // Backup & Restore Group
                SettingsGroup(title = stringResource(R.string.backup_restore_title)) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = settings.autoBackupEnabled,
                                onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.setting_auto_backup))
                        }

                        val lastBackupText = if (settings.lastBackupTimestamp > 0) {
                            val pattern = if (settings.dateFormat == "DD/MM/YYYY") "dd/MM/yyyy HH:mm" else "MM/dd/yyyy HH:mm"
                            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                            stringResource(R.string.last_backup_format, sdf.format(Date(settings.lastBackupTimestamp)))
                        } else {
                            stringResource(R.string.last_backup_never)
                        }
                        
                        Text(
                            text = lastBackupText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.shareLatestData(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.btn_share_latest_data))
                        }
                        OutlinedButton(
                            onClick = { viewModel.exportData(context, settings.userId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.btn_export_my_data))
                        }
                        OutlinedButton(
                            onClick = { viewModel.exportData(context, null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.btn_export_all_data))
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch("application/json") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.btn_import_data))
                        }
                    }
                }

                // System Group
                val showNotifWarning = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                } else false

                if (showNotifWarning || viewModel.isBatteryOptimizing()) {
                    SettingsGroup(title = stringResource(R.string.battery_optimize_title)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (showNotifWarning) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.notif_warning_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                                        Text(stringResource(R.string.notif_warning_message), style = MaterialTheme.typography.bodySmall)
                                        Button(
                                            onClick = {
                                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                }
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Text(stringResource(R.string.btn_fix_notifs))
                                        }
                                    }
                                }
                            }

                            if (viewModel.isBatteryOptimizing()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.battery_optimize_message), style = MaterialTheme.typography.bodySmall)
                                        Button(
                                            onClick = {
                                                val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Text(stringResource(R.string.btn_fix_battery))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Credits
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(com.besomuncu.healthcompanion.R.string.credits_text),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        viewModel.toggleDebugMode()
                                    }
                                )
                            }
                        )
                    }
                }
                
                // Final safety spacer to ensure scrollability is obvious
                Spacer(modifier = Modifier.height(if (settings.layoutMode == "Simplified") 100.dp else 32.dp))
            }
        }
    }

    val scope = rememberCoroutineScope()

    if (pendingLanguage != null) {
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { pendingLanguage = null },
                title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.restart_title)) } },
                text = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.restart_message)) } },
                confirmButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Button(onClick = {
                            scope.launch {
                                viewModel.updateLanguage(pendingLanguage!!)
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                (context as? Activity)?.finishAffinity()
                                pendingLanguage = null
                            }
                        }) { Text(stringResource(R.string.btn_apply)) }
                    }
                },
                dismissButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        TextButton(onClick = { pendingLanguage = null }) { Text(stringResource(R.string.btn_cancel)) }
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = title, 
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(modifier = Modifier.alpha(0.2f))
    }
}
