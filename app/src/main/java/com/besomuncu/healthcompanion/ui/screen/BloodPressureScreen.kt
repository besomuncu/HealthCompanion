package com.besomuncu.healthcompanion.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.besomuncu.healthcompanion.R
import com.besomuncu.healthcompanion.data.model.BloodPressure
import com.besomuncu.healthcompanion.ui.viewmodel.HealthViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BloodPressureScreen(viewModel: HealthViewModel, onBack: (() -> Unit)? = null) {
    val history by viewModel.bloodPressureHistory.collectAsState()
    val settings by viewModel.settings.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var editingReading by remember { mutableStateOf<BloodPressure?>(null) }
    
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }

    var filterType by remember { mutableStateOf("DateNewest") }
    var showFilterMenu by remember { mutableStateOf(false) }

    var readingToDelete by remember { mutableStateOf<BloodPressure?>(null) }

    val sortedHistory = remember(history, filterType) {
        when (filterType) {
            "DateOldest" -> history.sortedBy { it.timestamp }
            "BPHigh" -> history.sortedByDescending { it.systolic + it.diastolic }
            "BPLow" -> history.sortedBy { it.systolic + it.diastolic }
            else -> history.sortedByDescending { it.timestamp }
        }
    }

    val dateFormatter = remember(settings.dateFormat) {
        val pattern = if (settings.dateFormat == "DD/MM/YYYY") "dd/MM/yyyy HH:mm" else "MM/dd/yyyy HH:mm"
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
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.bp_history), 
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
                                DropdownMenuItem(text = { Text(stringResource(R.string.filter_date_no)) }, onClick = { filterType = "DateNewest"; showFilterMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.filter_date_on)) }, onClick = { filterType = "DateOldest"; showFilterMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.filter_bp_hl)) }, onClick = { filterType = "BPHigh"; showFilterMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.filter_bp_lh)) }, onClick = { filterType = "BPLow"; showFilterMenu = false })
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                    // Interval + Add Button Block
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = if (settings.layoutMode == "Simplified") 4.dp else 16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.bp_reminder), style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = settings.bpReminderInterval == 1,
                                    onClick = { viewModel.setBpReminderInterval(1) },
                                    label = { Text("1h") },
                                    leadingIcon = if (settings.bpReminderInterval == 1) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                                )
                                FilterChip(
                                    selected = settings.bpReminderInterval == 3,
                                    onClick = { viewModel.setBpReminderInterval(3) },
                                    label = { Text("3h") },
                                    leadingIcon = if (settings.bpReminderInterval == 3) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                                )
                                FilterChip(
                                    selected = settings.bpReminderInterval == 6,
                                    onClick = { viewModel.setBpReminderInterval(6) },
                                    label = { Text("6h") },
                                    leadingIcon = if (settings.bpReminderInterval == 6) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                        
                        FloatingActionButton(
                            onClick = {
                                editingReading = null
                                systolic = ""; diastolic = ""; pulse = ""
                                showDialog = true
                            },
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_reading))
                        }
                    }

                    if (settings.layoutMode == "Simplified" && onBack != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(sortedHistory) { reading ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(R.string.bp_format, reading.systolic, reading.diastolic), style = MaterialTheme.typography.titleLarge)
                                    Text(stringResource(R.string.pulse_format, reading.pulse), style = MaterialTheme.typography.bodyMedium)
                                    Text(dateFormatter.format(Date(reading.timestamp)), style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingReading = reading
                                        systolic = reading.systolic.toString()
                                        diastolic = reading.diastolic.toString()
                                        pulse = reading.pulse.toString()
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = {
                                        readingToDelete = reading
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showDialog) {
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(if (editingReading == null) stringResource(R.string.add_reading) else stringResource(R.string.dialog_edit_reading)) } },
                text = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            OutlinedTextField(value = systolic, onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) systolic = it }, label = { Text(stringResource(R.string.systolic)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = diastolic, onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) diastolic = it }, label = { Text(stringResource(R.string.diastolic)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = pulse, onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) pulse = it }, label = { Text(stringResource(R.string.pulse)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Button(
                            onClick = {
                                val sys = systolic.toIntOrNull() ?: 120
                                val dia = diastolic.toIntOrNull() ?: 80
                                val pul = pulse.toIntOrNull() ?: 70
                                if (editingReading == null) {
                                    viewModel.addBloodPressure(sys, dia, pul)
                                } else {
                                    viewModel.updateBloodPressure(editingReading!!.copy(systolic = sys, diastolic = dia, pulse = pul))
                                }
                                showDialog = false
                            },
                            enabled = systolic.isNotBlank() && diastolic.isNotBlank() && pulse.isNotBlank()
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
    }

    if (readingToDelete != null) {
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { readingToDelete = null },
                title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.confirm_delete_reading_title)) } },
                text = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.confirm_delete_reading)) } },
                confirmButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Button(onClick = {
                            viewModel.deleteBloodPressure(readingToDelete!!)
                            readingToDelete = null
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text(stringResource(R.string.btn_delete))
                        }
                    }
                },
                dismissButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        TextButton(onClick = { readingToDelete = null }) { Text(stringResource(R.string.btn_cancel)) }
                    }
                }
            )
        }
    }
}
