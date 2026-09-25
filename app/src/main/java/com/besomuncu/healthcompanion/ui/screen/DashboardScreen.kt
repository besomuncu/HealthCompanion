package com.besomuncu.healthcompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.besomuncu.healthcompanion.R
import com.besomuncu.healthcompanion.data.model.DailySummary
import com.besomuncu.healthcompanion.data.model.LiquidIntake
import com.besomuncu.healthcompanion.data.model.MedicationLog
import com.besomuncu.healthcompanion.ui.viewmodel.HealthViewModel
import com.besomuncu.healthcompanion.ui.component.AutoScrollingText
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: HealthViewModel, 
    onBack: (() -> Unit)? = null,
    onSwitchUser: (() -> Unit)? = null
) {
    val user by viewModel.currentUser.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val nextMed by viewModel.nextMedication.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val intakes by viewModel.intakes.collectAsState()
    val bpHistory by viewModel.bloodPressureHistory.collectAsState()
    val pastSummaries by viewModel.dailySummaries.collectAsState()
    val isDebugMode by viewModel.isDebugMode.collectAsState()
    
    val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val todayIntakes = intakes.filter { it.timestamp >= todayStart }
    val waterIntake = todayIntakes.filter { it.liquidTypeId == null }.sumOf { it.amountMl }
    val otherIntake = todayIntakes.filter { it.liquidTypeId != null }.sumOf { it.amountMl }

    val datePattern = if (settings.dateFormat == "DD/MM/YYYY") "dd/MM/yyyy" else "MM/dd/yyyy"
    val dateFormatter = SimpleDateFormat(datePattern, Locale.getDefault())

    var selectedSummary by remember { mutableStateOf<DailySummary?>(null) }
    var selectedIntakeLabel by remember { mutableStateOf<String?>(null) }
    var summaryToDelete by remember { mutableStateOf<DailySummary?>(null) }
    var showDeleteAllConfirmation by remember { mutableStateOf(false) }

    val lastBp = bpHistory.firstOrNull()
    val lastBpText = if (lastBp != null) "${lastBp.systolic}/${lastBp.diastolic}" else "--"

    val groupedIntakes = remember(todayIntakes) {
        todayIntakes.groupBy { if (it.liquidTypeId == null) "Water" else it.label }
    }

    val timeFormatter = remember(settings.use24HourFormat) {
        val pattern = if (settings.use24HourFormat) "HH:mm" else "hh:mm a"
        SimpleDateFormat(pattern, Locale.getDefault())
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
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Header (ONLY in Normal mode)
                if (settings.layoutMode == "Normal") {
                    item {
                        CompositionLocalProvider(LocalDensity provides customDensity) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                AutoScrollingText(
                                    text = stringResource(R.string.welcome_guest, user?.name ?: ""),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onSwitchUser?.invoke() }) {
                                    Icon(Icons.Default.Person, contentDescription = stringResource(R.string.switch_user), modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // Dashboard Summary Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.today_summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummarySmallCard(
                                title = stringResource(R.string.water_label),
                                value = "$waterIntake ml",
                                icon = Icons.Default.WaterDrop,
                                color = Screen.Dashboard.iconColor,
                                modifier = Modifier.weight(1f)
                            )
                            SummarySmallCard(
                                title = stringResource(R.string.summary_other_liquids),
                                value = "$otherIntake ml",
                                icon = Icons.Default.LocalDrink,
                                color = Screen.Liquids.iconColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummarySmallCard(
                                title = stringResource(R.string.summary_last_bp),
                                value = lastBpText,
                                icon = Icons.Default.Favorite,
                                color = Screen.BloodPressure.iconColor,
                                modifier = Modifier.weight(1f)
                            )
                            SummarySmallCard(
                                title = stringResource(R.string.summary_next_med),
                                value = nextMed?.name ?: "--",
                                icon = Icons.Default.Medication,
                                color = Screen.Medications.iconColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Checklist Section
                item {
                    Text(stringResource(R.string.today_activities), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                val actionableMeds = medications.filter { it.pillCount > 0 || it.isTakenToday || it.initialPillCount == 0 }
                if (actionableMeds.isEmpty() && todayIntakes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                stringResource(R.string.no_activities_today),
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    if (actionableMeds.isNotEmpty()) {
                        item {
                            Text(stringResource(R.string.med_checklist), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        items(actionableMeds) { med ->
                            val foodResId = when(med.foodContext) {
                                "empty" -> R.string.food_empty
                                "full" -> R.string.food_full
                                else -> R.string.food_any
                            }
                            val isFinishedNow = med.initialPillCount > 0 && med.pillCount == 0 && med.isTakenToday

                            val displayTime = if (settings.use24HourFormat) {
                                med.scheduledTime
                            } else {
                                val parts = med.scheduledTime.split(":")
                                val h = parts[0].toInt()
                                val m = parts[1]
                                val suffix = if (h >= 12) stringResource(R.string.pm) else stringResource(R.string.am)
                                val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
                                String.format(Locale.getDefault(), "%d:%s %s", h12, m, suffix)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (med.isTakenToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = med.isTakenToday,
                                        onCheckedChange = { viewModel.toggleMedicationTaken(med) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        AutoScrollingText(
                                            text = med.name,
                                            style = (if (med.isTakenToday) MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline) else MaterialTheme.typography.bodyLarge).copy(
                                                textDecoration = if (isFinishedNow) TextDecoration.LineThrough else null
                                            )
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("$displayTime - ${stringResource(foodResId)}", style = MaterialTheme.typography.bodySmall)
                                            if (med.initialPillCount > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (med.pillCount > 0) stringResource(R.string.pill_stock, med.pillCount) else stringResource(R.string.pill_status_over),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (med.pillCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (groupedIntakes.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.nav_liquids), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        items(groupedIntakes.keys.toList()) { label ->
                            val totalForLabel = groupedIntakes[label]?.sumOf { it.amountMl } ?: 0
                            val displayLabel = if (label == "Water") stringResource(R.string.water_label) else label
                            Card(
                                onClick = { selectedIntakeLabel = label },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    AutoScrollingText(
                                        text = "• $displayLabel ($totalForLabel ml)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }

                // History Section
                if (pastSummaries.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.past_summaries),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { showDeleteAllConfirmation = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(max = 36.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_delete_all),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    items(pastSummaries) { summary ->
                        val dateText = dateFormatter.format(Date(summary.date))
                        Card(
                            onClick = { selectedSummary = summary },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(dateText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        HistoryStat(Icons.Default.WaterDrop, "${summary.totalWaterMl}ml", Color(0xFF2196F3))
                                        HistoryStat(Icons.Default.LocalDrink, "${summary.totalOtherMl}ml", Color(0xFF4CAF50))
                                        HistoryStat(Icons.Default.Medication, "${summary.medicationsTakenCount}", Color(0xFF9C27B0))
                                    }
                                }
                                IconButton(
                                    onClick = { summaryToDelete = summary },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_delete), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                // Static Debug Menu at the bottom
                if (isDebugMode) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(stringResource(R.string.debug_menu), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.simulateDayPassed() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(stringResource(R.string.btn_simulate_day), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(
                                        onClick = { viewModel.simulateBpReminder() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(stringResource(R.string.btn_simulate_bp), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Button(
                                    onClick = { viewModel.simulateWaterReminder() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(stringResource(R.string.btn_simulate_water), style = MaterialTheme.typography.bodySmall)
                                }
                                Button(
                                    onClick = { viewModel.simulateMedicationReminder() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Simulate Med", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(if (settings.layoutMode == "Simplified") 16.dp else 80.dp)) }
            }
        }
    }

    if (summaryToDelete != null) {
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { summaryToDelete = null },
                title = { Text(stringResource(R.string.confirm_delete_summary_title)) },
                text = { Text(stringResource(R.string.confirm_delete_summary)) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteDailySummary(summaryToDelete!!)
                        summaryToDelete = null
                    }) { Text(stringResource(R.string.btn_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { summaryToDelete = null }) { Text(stringResource(R.string.btn_cancel)) }
                }
            )
        }
    }

    if (showDeleteAllConfirmation) {
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirmation = false },
                title = { Text(stringResource(R.string.confirm_delete_all_summaries_title)) },
                text = { Text(stringResource(R.string.confirm_delete_all_summaries_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAllDailySummaries()
                            showDeleteAllConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.btn_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllConfirmation = false }) { Text(stringResource(R.string.btn_cancel)) }
                }
            )
        }
    }

    if (selectedSummary != null) {
        val dateText = dateFormatter.format(Date(selectedSummary!!.date))
        var summaryIntakes by remember { mutableStateOf<List<LiquidIntake>>(emptyList()) }
        var summaryMeds by remember { mutableStateOf<List<MedicationLog>>(emptyList()) }
        
        LaunchedEffect(selectedSummary) {
            summaryIntakes = viewModel.getIntakesForSummary(selectedSummary!!)
            summaryMeds = viewModel.getMedicationLogsForSummary(selectedSummary!!)
        }

        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { selectedSummary = null },
                title = { Text(stringResource(R.string.summary_detail_title, dateText)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        Text(stringResource(R.string.summary_water, selectedSummary!!.totalWaterMl), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.summary_other, selectedSummary!!.totalOtherMl), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.meds_taken_label, selectedSummary!!.medicationsTakenCount), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.avg_bp_label, selectedSummary!!.averageSystolic, selectedSummary!!.averageDiastolic), style = MaterialTheme.typography.titleSmall)
                        
                        if (summaryIntakes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.summary_liquids_title), style = MaterialTheme.typography.labelLarge)
                            summaryIntakes.forEach { intake ->
                                val label = if (intake.liquidTypeId == null) stringResource(R.string.water_label) else intake.label
                                Text("• ${intake.amountMl}ml $label", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        if (summaryMeds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.taken_meds_label), style = MaterialTheme.typography.labelLarge)
                            summaryMeds.forEach { log ->
                                Text("• ${log.medName}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        
                        if (selectedSummary!!.medicationsUntakenNames.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.untaken_meds_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                            selectedSummary!!.medicationsUntakenNames.split(", ").forEach { name ->
                                Text("• $name", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { selectedSummary = null }) { Text(stringResource(R.string.btn_ok)) }
                }
            )
        }
    }

    if (selectedIntakeLabel != null) {
        val intakesForLabel = groupedIntakes[selectedIntakeLabel] ?: emptyList()
        val totalForLabel = intakesForLabel.sumOf { it.amountMl }
        val displayLabel = if (selectedIntakeLabel == "Water") stringResource(R.string.water_label) else selectedIntakeLabel!!

        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { selectedIntakeLabel = null },
                title = { Text(displayLabel) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        Text(
                            text = stringResource(R.string.today_log) + ": $totalForLabel ml",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        intakesForLabel.forEach { intake ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${intake.amountMl} ml", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    timeFormatter.format(Date(intake.timestamp)), 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            HorizontalDivider(modifier = Modifier.alpha(0.3f))
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { selectedIntakeLabel = null }) { Text(stringResource(R.string.btn_ok)) }
                }
            )
        }
    }
}

@Composable
fun SummarySmallCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
                Spacer(modifier = Modifier.width(4.dp))
                AutoScrollingText(
                    text = title, 
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f)
                )
            }
            AutoScrollingText(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun HistoryStat(icon: ImageVector, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
