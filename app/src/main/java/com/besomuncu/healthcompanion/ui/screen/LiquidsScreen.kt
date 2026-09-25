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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.besomuncu.healthcompanion.R
import com.besomuncu.healthcompanion.ui.component.AutoScrollingText
import com.besomuncu.healthcompanion.ui.viewmodel.HealthViewModel
import com.besomuncu.healthcompanion.util.ValidationUtils
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiquidsScreen(viewModel: HealthViewModel, onBack: (() -> Unit)? = null) {
    val dailyTotal by viewModel.dailyWaterTotal.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val favoriteIntakes by viewModel.favoriteIntakes.collectAsState()
    val history by viewModel.intakes.collectAsState()
    
    var showCustomMlDialog by remember { mutableStateOf(false) }
    var showCustomDrinkDialog by remember { mutableStateOf(false) }
    var customMlAmount by remember { mutableStateOf("") }
    var customDrinkName by remember { mutableStateOf("") }
    var customDrinkAmount by remember { mutableStateOf("") }
    var hydrationFactor by remember { mutableStateOf("1.0") }
    
    var showGoalDialog by remember { mutableStateOf(false) }
    var newGoal by remember { mutableStateOf(user?.dailyWaterGoal?.toString() ?: "2000") }

    var showResetConfirmation by remember { mutableStateOf(false) }
    var favoriteToDelete by remember { mutableStateOf<com.besomuncu.healthcompanion.data.model.FavoriteIntake?>(null) }
    var intakeToDelete by remember { mutableStateOf<com.besomuncu.healthcompanion.data.model.LiquidIntake?>(null) }

    var filterType by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf("NewToOld") }
    var showFilterMenu by remember { mutableStateOf(false) }

    val waterLabel = stringResource(R.string.water_label)
    val todayStart = Calendar.getInstance().apply { 
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val filteredHistory = remember(history, filterType, sortOrder, waterLabel) {
        history.filter { it.timestamp >= todayStart }
            .filter { 
                when (filterType) {
                    "Water" -> it.liquidTypeId == null
                    "All" -> true
                    else -> it.label == filterType && it.liquidTypeId != null
                }
            }
            .let { list ->
                if (sortOrder == "NewToOld") list.sortedByDescending { it.timestamp }
                else list.sortedBy { it.timestamp }
            }
    }

    val customDrinkNames = remember(favoriteIntakes) {
        favoriteIntakes.filter { it.liquidTypeId != null }.map { it.label }.distinct()
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
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                    // Static Reminder Interval Setting
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = if (settings.layoutMode == "Simplified") 4.dp else 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.water_reminder_interval), style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.waterReminderInterval == 1,
                                onClick = { viewModel.setWaterReminderInterval(1) },
                                label = { Text("1h") },
                                leadingIcon = if (settings.waterReminderInterval == 1) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                            )
                            FilterChip(
                                selected = settings.waterReminderInterval == 3,
                                onClick = { viewModel.setWaterReminderInterval(3) },
                                label = { Text("3h") },
                                leadingIcon = if (settings.waterReminderInterval == 3) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                            )
                            FilterChip(
                                selected = settings.waterReminderInterval == 6,
                                onClick = { viewModel.setWaterReminderInterval(6) },
                                label = { Text("6h") },
                                leadingIcon = if (settings.waterReminderInterval == 6) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                            )
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
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.daily_liquid_intake), 
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val iconSize = with(LocalDensity.current) { 32.sp.toDp() }
                            val buttonSize = with(LocalDensity.current) { 48.sp.toDp() }
                            
                            IconButton(
                                onClick = { showResetConfirmation = true },
                                modifier = Modifier.size(buttonSize)
                            ) {
                                Icon(
                                    Icons.Default.RestartAlt, 
                                    contentDescription = stringResource(R.string.btn_reset_intake), 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                            IconButton(
                                onClick = { 
                                    newGoal = user?.dailyWaterGoal?.toString() ?: "2000"
                                    showGoalDialog = true 
                                },
                                modifier = Modifier.size(buttonSize)
                            ) {
                                Icon(
                                    Icons.Default.Edit, 
                                    contentDescription = "Edit Goal",
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                        }
                    }
                }

                // Progress Info
                item {
                    Column {
                        LinearProgressIndicator(
                            progress = { 
                                val goal = user?.dailyWaterGoal ?: 2000
                                (dailyTotal.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth().height(12.dp),
                            strokeCap = StrokeCap.Round
                        )
                        
                        Text(
                            text = stringResource(R.string.water_goal_format, dailyTotal, user?.dailyWaterGoal ?: 2000),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Quick Buttons
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val buttonShape = RoundedCornerShape(24.dp)
                        val buttonModifier = Modifier.heightIn(min = 48.dp)

                        Button(onClick = { viewModel.addLiquidIntake(200, null, waterLabel) }, shape = buttonShape, modifier = buttonModifier) { Text(stringResource(R.string.btn_200ml)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { viewModel.addLiquidIntake(330, null, waterLabel) }, shape = buttonShape, modifier = buttonModifier) { Text(stringResource(R.string.btn_330ml)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { viewModel.addLiquidIntake(500, null, waterLabel) }, shape = buttonShape, modifier = buttonModifier) { Text(stringResource(R.string.btn_500ml)) }
                        
                        favoriteIntakes.forEach { fav ->
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.addLiquidIntake(fav.amountMl, fav.liquidTypeId, fav.label) },
                                shape = buttonShape,
                                modifier = buttonModifier,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val label = if (fav.liquidTypeId == null) stringResource(R.string.water_label) else fav.label
                                    if (fav.liquidTypeId == null) {
                                        Text("${fav.amountMl}ml")
                                    } else {
                                        Text("$label (${fav.amountMl}ml)")
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { favoriteToDelete = fav },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom Entry Buttons
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Button(
                            onClick = { showCustomMlDialog = true },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            AutoScrollingText(
                                text = stringResource(R.string.btn_custom), 
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showCustomDrinkDialog = true },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            AutoScrollingText(
                                text = stringResource(R.string.btn_new_drink), 
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Today Log Header
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.today_log), style = MaterialTheme.typography.titleMedium)
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                            CompositionLocalProvider(LocalDensity provides customDensity) {
                                DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.filter_type_all)) }, onClick = { filterType = "All"; showFilterMenu = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.filter_type_label, waterLabel)) }, onClick = { filterType = "Water"; showFilterMenu = false })
                                    customDrinkNames.forEach { name ->
                                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_type_label, name)) }, onClick = { filterType = name; showFilterMenu = false })
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(text = { Text(stringResource(R.string.filter_order_no)) }, onClick = { sortOrder = "NewToOld"; showFilterMenu = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.filter_order_on)) }, onClick = { sortOrder = "OldToNew"; showFilterMenu = false })
                                }
                            }
                        }
                    }
                }

                // Log History List
                items(filteredHistory) { intake ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val displayLabel = if (intake.liquidTypeId == null) stringResource(R.string.water_label) else intake.label
                                Text("${intake.amountMl} ml", style = MaterialTheme.typography.bodyLarge)
                                Text(displayLabel, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { intakeToDelete = intake }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showCustomMlDialog) {
        val amount = customMlAmount.toIntOrNull() ?: 0
        val isDuplicate = favoriteIntakes.any { it.label == waterLabel && it.amountMl == amount }
        
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { showCustomMlDialog = false },
                title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.custom_ml_title)) } },
                text = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            OutlinedTextField(
                                value = customMlAmount,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() }) customMlAmount = input
                                },
                                label = { Text(stringResource(R.string.liquid_ml_hint)) },
                                placeholder = { Text(stringResource(R.string.optional_hint), color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                isError = (customMlAmount.isNotEmpty() && !ValidationUtils.isNumberValid(customMlAmount)) || isDuplicate
                            )
                            if (customMlAmount.isNotEmpty() && !ValidationUtils.isNumberValid(customMlAmount)) {
                                Text(stringResource(R.string.error_must_be_number), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                            if (isDuplicate) {
                                Text(stringResource(R.string.error_duplicate_button), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Button(onClick = {
                            if (amount > 0) {
                                viewModel.addFavoriteIntake(amount, waterLabel)
                                showCustomMlDialog = false
                                customMlAmount = ""
                            }
                        }, enabled = ValidationUtils.isNumberValid(customMlAmount) && !isDuplicate) { Text(stringResource(R.string.btn_save)) }
                    }
                },
                dismissButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        TextButton(onClick = { showCustomMlDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
                    }
                }
            )
        }
    }

    if (showCustomDrinkDialog) {
        val amount = customDrinkAmount.toIntOrNull() ?: 0
        val isDuplicate = favoriteIntakes.any { it.label == customDrinkName && it.amountMl == amount }
        val hydrationFactorStr = hydrationFactor
        val factorVal = hydrationFactorStr.toFloatOrNull() ?: -1f
        val isFactorValid = factorVal in 0f..1.0f

        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { showCustomDrinkDialog = false },
                title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.custom_drink_title)) } },
                text = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            OutlinedTextField(
                                value = customDrinkName, 
                                onValueChange = { customDrinkName = it }, 
                                label = { Text(stringResource(R.string.liquid_name_hint)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = customDrinkAmount,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() }) customDrinkAmount = input
                                },
                                label = { Text(stringResource(R.string.liquid_ml_hint)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                isError = (customDrinkAmount.isNotEmpty() && !ValidationUtils.isNumberValid(customDrinkAmount)) || isDuplicate
                            )
                            if (isDuplicate) {
                                Text(stringResource(R.string.error_duplicate_button), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = hydrationFactor,
                                onValueChange = { hydrationFactor = it },
                                label = { Text(stringResource(R.string.hydration_factor)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                isError = hydrationFactor.isNotEmpty() && !isFactorValid,
                                trailingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            Text(stringResource(R.string.hydration_factor_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (hydrationFactor.isNotEmpty() && !isFactorValid) {
                                Text(stringResource(R.string.error_invalid_factor), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Button(onClick = {
                            if (amount > 0 && customDrinkName.isNotBlank() && isFactorValid) {
                                viewModel.addLiquidTypeWithFavorite(customDrinkName, factorVal, amount)
                                showCustomDrinkDialog = false
                                customDrinkName = ""; customDrinkAmount = ""
                            }
                        }, enabled = ValidationUtils.isNumberValid(customDrinkAmount) && customDrinkName.isNotBlank() && !isDuplicate && isFactorValid) { Text(stringResource(R.string.btn_save)) }
                    }
                },
                dismissButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        TextButton(onClick = { showCustomDrinkDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
                    }
                }
            )
        }
    }

    if (showGoalDialog) {
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { showGoalDialog = false },
                title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.water_goal_hint)) } },
                text = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            OutlinedTextField(
                                value = newGoal,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() }) newGoal = input
                                },
                                label = { Text(stringResource(R.string.water_goal_hint)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            val recommended = ((user?.weight ?: 0f) * 35).toInt()
                            Text(stringResource(R.string.recommended_goal, recommended), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                confirmButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Button(onClick = {
                            val goal = newGoal.toIntOrNull() ?: 2000
                            viewModel.updateWaterGoal(goal)
                            showGoalDialog = false
                        }) { Text(stringResource(R.string.btn_save)) }
                    }
                },
                dismissButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        TextButton(onClick = { showGoalDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
                    }
                }
            )
        }
    }

    if (showResetConfirmation) {
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { showResetConfirmation = false },
                title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.confirm_reset_title)) } },
                text = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.confirm_reset_intake)) } },
                confirmButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Button(onClick = {
                            viewModel.resetDailyIntake()
                            showResetConfirmation = false
                        }) { Text(stringResource(R.string.btn_reset)) }
                    }
                },
                dismissButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        TextButton(onClick = { showResetConfirmation = false }) { Text(stringResource(R.string.btn_cancel)) }
                    }
                }
            )
        }
    }

    if (favoriteToDelete != null) {
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { favoriteToDelete = null },
                title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.confirm_delete_fav_title)) } },
                text = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.confirm_delete_fav) + " (${favoriteToDelete!!.label})") } },
                confirmButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Button(onClick = {
                            viewModel.deleteFavoriteIntake(favoriteToDelete!!)
                            favoriteToDelete = null
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text(stringResource(R.string.btn_delete))
                        }
                    }
                },
                dismissButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        TextButton(onClick = { favoriteToDelete = null }) { Text(stringResource(R.string.btn_cancel)) }
                    }
                }
            )
        }
    }

    if (intakeToDelete != null) {
        CompositionLocalProvider(LocalDensity provides customDensity) {
            AlertDialog(
                onDismissRequest = { intakeToDelete = null },
                title = { CompositionLocalProvider(LocalDensity provides customDensity) { Text(stringResource(R.string.btn_delete)) } },
                text = { CompositionLocalProvider(LocalDensity provides customDensity) { Text("${intakeToDelete!!.amountMl}ml ${intakeToDelete!!.label}") } },
                confirmButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        Button(onClick = {
                            viewModel.deleteIntake(intakeToDelete!!)
                            intakeToDelete = null
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text(stringResource(R.string.btn_delete)) }
                    }
                },
                dismissButton = {
                    CompositionLocalProvider(LocalDensity provides customDensity) {
                        TextButton(onClick = { intakeToDelete = null }) { Text(stringResource(R.string.btn_cancel)) }
                    }
                }
            )
        }
    }
}
