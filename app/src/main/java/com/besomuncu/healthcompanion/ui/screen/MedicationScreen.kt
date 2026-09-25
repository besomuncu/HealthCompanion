package com.besomuncu.healthcompanion.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.besomuncu.healthcompanion.R
import com.besomuncu.healthcompanion.data.model.Medication
import com.besomuncu.healthcompanion.ui.viewmodel.HealthViewModel
import com.besomuncu.healthcompanion.util.ValidationUtils
import java.util.Locale

@Composable
fun QuantityStepper(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    minValue: Int = 0,
    maxValue: Int = Int.MAX_VALUE
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    val current = value.toIntOrNull() ?: 0
                    if (current > minValue) onValueChange((current - 1).toString())
                },
                enabled = enabled && (value.toIntOrNull() ?: 0) > minValue
            ) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
            }
            
            Text(
                text = if (value.isEmpty()) "0" else value,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 24.dp),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )

            IconButton(
                onClick = {
                    val current = value.toIntOrNull() ?: 0
                    if (current < maxValue) onValueChange((current + 1).toString())
                },
                enabled = enabled && (value.toIntOrNull() ?: 0) < maxValue
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
            }
        }
    }
}

@Composable
fun MedicationScreen(viewModel: HealthViewModel, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val medications by viewModel.medications.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingMed by remember { mutableStateOf<Medication?>(null) }
    
    var name by remember { mutableStateOf("") }
    var illness by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("") }
    var timeInput by remember { mutableStateOf(TextFieldValue("")) }
    var isPm by remember { mutableStateOf(false) }
    var pillCount by remember { mutableStateOf("") }
    var dosageInput by remember { mutableStateOf("1") }
    var foodContextIndex by remember { mutableIntStateOf(0) }
    
    var showRefillDialog by remember { mutableStateOf<Medication?>(null) }
    var showManualCancelDialog by remember { mutableStateOf<Medication?>(null) }
    var medToDelete by remember { mutableStateOf<Medication?>(null) }

    var filterType by remember { mutableStateOf("Name AZ") }
    var showFilterMenu by remember { mutableStateOf(false) }

    val sortedMeds = remember(medications, filterType) {
        when (filterType) {
            "Name ZA" -> medications.sortedByDescending { it.name.lowercase() }
            "Pills HL" -> medications.sortedByDescending { it.pillCount }
            "Pills LH" -> medications.sortedBy { it.pillCount }
            "Time EL" -> medications.sortedBy { it.scheduledTime }
            "Time LE" -> medications.sortedByDescending { it.scheduledTime }
            else -> medications.sortedBy { it.name.lowercase() }
        }
    }

    val foodOptions = listOf(
        R.string.food_any,
        R.string.food_empty,
        R.string.food_full
    )

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
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.medications), 
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        val iconSize = with(LocalDensity.current) { 32.sp.toDp() }
                        IconButton(
                            onClick = { showFilterMenu = true },
                            modifier = Modifier.size(with(LocalDensity.current) { 48.sp.toDp() })
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(iconSize))
                        }
                        CompositionLocalProvider(LocalDensity provides customDensity) {
                            DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.filter_name_az)) }, onClick = { filterType = "Name AZ"; showFilterMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.filter_name_za)) }, onClick = { filterType = "Name ZA"; showFilterMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.filter_pills_hl)) }, onClick = { filterType = "Pills HL"; showFilterMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.filter_pills_lh)) }, onClick = { filterType = "Pills LH"; showFilterMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.filter_time_el)) }, onClick = { filterType = "Time EL"; showFilterMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.filter_time_le)) }, onClick = { filterType = "Time LE"; showFilterMenu = false })
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { 
                        editingMed = null
                        name = ""; illness = ""; method = ""; timeInput = TextFieldValue(""); foodContextIndex = 0; pillCount = ""; isPm = false
                        showDialog = true 
                    },
                    modifier = Modifier.padding(bottom = if (settings.layoutMode == "Simplified") 16.dp else 0.dp) // Sit just above the back button bar
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_medication))
                }
            },
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
            Column(modifier = Modifier.padding(padding).padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(sortedMeds) { med ->
                        val foodResId = when(med.foodContext) {
                            "empty" -> R.string.food_empty
                            "full" -> R.string.food_full
                            else -> R.string.food_any
                        }
                        val isOut = med.initialPillCount > 0 && med.pillCount <= 0
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
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(if (isOut) 0.6f else 1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (med.isTakenToday) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        med.name, 
                                        style = MaterialTheme.typography.titleLarge,
                                        textDecoration = if (isOut) TextDecoration.LineThrough else null
                                    )
                                    Text(stringResource(R.string.method_at, med.intakeMethod, displayTime), style = MaterialTheme.typography.bodyMedium)
                                    if (med.illness.isNotBlank()) {
                                        Text(stringResource(R.string.for_illness, med.illness), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(stringResource(foodResId), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    if (med.initialPillCount > 0) {
                                        Text(
                                            text = if (!isOut) stringResource(R.string.pill_stock, med.pillCount) else stringResource(R.string.pill_status_over),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (!isOut) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row {
                                        if (med.initialPillCount > 0) {
                                            if (isOut) {
                                                IconButton(onClick = { showRefillDialog = med }) {
                                                    Icon(Icons.Default.Refresh, contentDescription = "Refill", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = { showManualCancelDialog = med }) {
                                                    Icon(Icons.Default.AlarmOff, contentDescription = stringResource(R.string.cancel_os_alarm), tint = MaterialTheme.colorScheme.error)
                                                }
                                            } else {
                                                IconButton(onClick = { viewModel.createSystemAlarm(med) }) {
                                                    Icon(Icons.Default.Alarm, contentDescription = stringResource(R.string.create_os_alarm), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        } else {
                                             IconButton(onClick = { viewModel.createSystemAlarm(med) }) {
                                                Icon(Icons.Default.Alarm, contentDescription = stringResource(R.string.create_os_alarm), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        IconButton(onClick = {
                                            editingMed = med
                                            name = med.name
                                            illness = med.illness
                                            method = med.intakeMethod
                                            val parts = med.scheduledTime.split(":")
                                            val h = parts[0].toInt()
                                            val initialTime = if (!settings.use24HourFormat) {
                                                isPm = h >= 12
                                                val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
                                                String.format(Locale.getDefault(), "%02d:%s", h12, parts[1])
                                            } else {
                                                med.scheduledTime
                                            }
                                            timeInput = TextFieldValue(initialTime, TextRange(initialTime.length))
                                            pillCount = if (med.initialPillCount > 0) med.pillCount.toString() else ""
                                            dosageInput = med.dosagePerIntake.toString()
                                            foodContextIndex = when(med.foodContext) {
                                                "empty" -> 1
                                                "full" -> 2
                                                else -> 0
                                            }
                                            showDialog = true 
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                    }
                                    IconButton(onClick = { medToDelete = med }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(if (editingMed == null) stringResource(R.string.add_medication) else stringResource(R.string.dialog_edit_med)) } },
            text = {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.med_name)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            value = illness, 
                            onValueChange = { illness = it }, 
                            label = { Text(stringResource(R.string.illness) + " " + stringResource(R.string.optional_hint)) }, 
                            placeholder = { Text(stringResource(R.string.optional_hint), color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text(stringResource(R.string.method_hint)) }, modifier = Modifier.fillMaxWidth())
                        
                        val time = timeInput.text
                        val hourValue = if (time.contains(":")) time.split(":").first().toIntOrNull() ?: 0 else time.toIntOrNull() ?: 0
                        val isHourInvalid = if (settings.use24HourFormat) hourValue > 23 else hourValue > 12

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = timeInput,
                                onValueChange = { input ->
                                    val currentClean = timeInput.text.filter { it.isDigit() }
                                    val newClean = input.text.filter { it.isDigit() }
                                    
                                    if (newClean.length > 4) return@OutlinedTextField
                                    
                                    val isDeleting = input.text.length < timeInput.text.length
                                    
                                    var formatted = newClean
                                    if (newClean.length >= 2) {
                                        formatted = newClean.substring(0, 2) + ":" + newClean.substring(2)
                                    }
                                    
                                    var selectionIndex = input.selection.start
                                    if (!isDeleting && newClean.length == 2 && currentClean.length == 1) {
                                        selectionIndex = 3
                                    }
                                    
                                    timeInput = TextFieldValue(formatted, TextRange(selectionIndex.coerceIn(0, formatted.length)))
                                },
                                label = { Text(stringResource(R.string.time_hint)) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = (time.isNotEmpty() && !ValidationUtils.isTimeValid(time)) || isHourInvalid
                            )
                            if (!settings.use24HourFormat) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FilterChip(
                                        selected = !isPm, 
                                        onClick = { isPm = false }, 
                                        label = { Text(stringResource(R.string.am)) }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    FilterChip(
                                        selected = isPm, 
                                        onClick = { isPm = true }, 
                                        label = { Text(stringResource(R.string.pm)) }
                                    )
                                }
                            }
                        }
                        if (isHourInvalid) {
                            Text(
                                text = if (settings.use24HourFormat) stringResource(R.string.error_hour_limit_24) else stringResource(R.string.error_hour_limit_12),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else if (time.isNotEmpty() && !ValidationUtils.isTimeValid(time)) {
                            Text(stringResource(R.string.error_invalid_time), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stepper for Pill Count
                        QuantityStepper(
                            value = pillCount,
                            onValueChange = { 
                                pillCount = it 
                                val newPillCount = it.toIntOrNull() ?: 0
                                val currentDosage = dosageInput.toIntOrNull() ?: 1
                                if (currentDosage > newPillCount && newPillCount > 0) {
                                    dosageInput = newPillCount.toString()
                                }
                            },
                            label = stringResource(R.string.pill_count_hint) + " " + stringResource(R.string.optional_hint)
                        )

                        val maxDosage = pillCount.toIntOrNull() ?: 0
                        val isDosageEnabled = maxDosage > 0
                        if (!isDosageEnabled) {
                            dosageInput = "1" // Reset to default when disabled
                        }

                        // Stepper for Dosage
                        QuantityStepper(
                            value = dosageInput,
                            onValueChange = { dosageInput = it },
                            label = stringResource(R.string.dosage_per_intake),
                            enabled = isDosageEnabled,
                            minValue = 1,
                            maxValue = maxDosage
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.food_context), style = MaterialTheme.typography.labelMedium)
                        foodOptions.forEachIndexed { index, resId ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = foodContextIndex == index, onClick = { foodContextIndex = index })
                                Text(stringResource(resId))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    val time = timeInput.text
                    val hourValue = if (time.contains(":")) time.split(":").first().toIntOrNull() ?: 0 else time.toIntOrNull() ?: 0
                    val isHourInvalid = if (settings.use24HourFormat) hourValue > 23 else hourValue > 12

                    Button(
                        onClick = {
                            val foodValue = when(foodContextIndex) {
                                1 -> "empty"
                                2 -> "full"
                                else -> "any"
                            }
                            val count = pillCount.toIntOrNull() ?: 0
                            val dosage = dosageInput.toIntOrNull() ?: 1
                            
                            val finalTime = if (!settings.use24HourFormat) {
                                val parts = time.split(":")
                                if (parts.size == 2) {
                                    var h = parts[0].toIntOrNull() ?: 0
                                    if (isPm && h < 12) h += 12
                                    if (!isPm && h == 12) h = 0
                                    String.format(Locale.getDefault(), "%02d:%s", h, parts[1])
                                } else time
                            } else time

                            if (editingMed == null) {
                                viewModel.addMedication(name, illness, method, finalTime, foodValue, count, dosage)
                            } else {
                                viewModel.updateMedication(editingMed!!.copy(name = name, illness = illness, intakeMethod = method, scheduledTime = finalTime, foodContext = foodValue, initialPillCount = count, pillCount = count, dosagePerIntake = dosage))
                            }
                            showDialog = false
                        },
                        enabled = name.isNotBlank() && method.isNotBlank() && ValidationUtils.isTimeValid(time) && !isHourInvalid && (dosageInput.toIntOrNull() ?: 0) > 0
                    ) { Text(stringResource(R.string.btn_save)) }
                }
            },
            dismissButton = {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
                }
            }
        )
    }

    if (showRefillDialog != null) {
        AlertDialog(
            onDismissRequest = { showRefillDialog = null },
            title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.btn_refill)) } },
            text = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.refill_warning, showRefillDialog!!.initialPillCount)) } },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    Button(onClick = {
                        viewModel.refillMedication(showRefillDialog!!)
                        showRefillDialog = null
                    }) { Text(stringResource(R.string.btn_apply)) }
                }
            },
            dismissButton = {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    TextButton(onClick = { showRefillDialog = null }) { Text(stringResource(R.string.btn_cancel)) }
                }
            }
        )
    }

    if (showManualCancelDialog != null) {
        AlertDialog(
            onDismissRequest = { showManualCancelDialog = null },
            title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.cancel_alarm_manual_title)) } },
            text = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.cancel_alarm_manual_message)) } },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    Button(onClick = {
                        viewModel.toggleAlarmManually(showManualCancelDialog!!, true)
                        val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        showManualCancelDialog = null
                    }) {
                        Text(stringResource(R.string.btn_ok))
                    }
                }
            },
            dismissButton = {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    TextButton(onClick = { showManualCancelDialog = null }) { Text(stringResource(R.string.btn_cancel)) }
                }
            }
        )
    }

    if (medToDelete != null) {
        AlertDialog(
            onDismissRequest = { medToDelete = null },
            title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.btn_delete_med)) } },
            text = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.confirm_delete_med)) } },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    Button(onClick = {
                        viewModel.deleteMedication(medToDelete!!)
                        medToDelete = null
                    }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text(stringResource(R.string.btn_delete))
                    }
                }
            },
            dismissButton = {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    TextButton(onClick = { medToDelete = null }) { Text(stringResource(R.string.btn_cancel)) }
                }
            }
        )
    }
}
