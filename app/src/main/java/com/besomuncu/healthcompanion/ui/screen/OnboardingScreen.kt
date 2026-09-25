package com.besomuncu.healthcompanion.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.besomuncu.healthcompanion.R
import com.besomuncu.healthcompanion.ui.component.AutoScrollingText
import com.besomuncu.healthcompanion.ui.viewmodel.HealthViewModel

@Composable
fun OnboardingScreen(viewModel: HealthViewModel) {
    var currentStep by remember { mutableIntStateOf(1) }
    val settings by viewModel.settings.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val isInitialSetup = allUsers.isEmpty()
    val context = LocalContext.current

    // Hoisted state for Step 3
    var name by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var waterGoal by remember { mutableStateOf("") }

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Icon(
                imageVector = Icons.Default.HealthAndSafety,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { currentStep.toFloat() / 3f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "OnboardingStep"
                ) { step ->
                    when (step) {
                        1 -> StepTextSize(viewModel)
                        2 -> StepLayoutMode(viewModel)
                        3 -> StepProfileCreation(
                            viewModel = viewModel,
                            name = name,
                            onNameChange = { name = it },
                            weight = weight,
                            onWeightChange = { weight = it },
                            height = height,
                            onHeightChange = { height = it },
                            waterGoal = waterGoal,
                            onWaterGoalChange = { waterGoal = it }
                        )
                    }
                }
            }

            // Standardized Bottom Navigation for All Steps
            Column(modifier = Modifier.fillMaxWidth()) {
                // "Returning User" button logic
                if (isInitialSetup) {
                    if (currentStep == 1) {
                        // In Step 1, it will be handled in the Row below (placeholder for Back)
                    } else {
                        // In Step 2 & 3, it sits above the Back/Next row
                        TextButton(
                            onClick = { viewModel.importLatestOrPicker(context) { importLauncher.launch("application/json") } },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_import_returning),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Button Slot
                    Box(modifier = Modifier.weight(1f)) {
                        if (currentStep > 1) {
                            TextButton(onClick = { currentStep-- }) {
                                Text(stringResource(R.string.btn_back))
                            }
                        } else if (isInitialSetup) {
                            TextButton(
                                onClick = { viewModel.importLatestOrPicker(context) { importLauncher.launch("application/json") } },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AutoScrollingText(
                                    text = stringResource(R.string.btn_import_returning),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        } else if (allUsers.isNotEmpty()) {
                            TextButton(onClick = { viewModel.setShowOnboarding(false) }) {
                                Text(stringResource(R.string.btn_cancel))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Button Slot
                    val isLoading by viewModel.isLoading.collectAsState()
                    Box(modifier = Modifier.weight(if (currentStep < 3) 0.6f else 1f), contentAlignment = Alignment.CenterEnd) {
                        if (currentStep < 3) {
                            Button(
                                onClick = { currentStep++ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.btn_next))
                            }
                        } else {
                            // Finish Button for Step 3
                            Button(
                                onClick = {
                                    if (name.isNotBlank()) {
                                        val w = weight.toFloatOrNull() ?: 70f
                                        val h = height.toFloatOrNull() ?: 170f
                                        val g = waterGoal.toIntOrNull() ?: 2000
                                        viewModel.addUser(name, w, h, g)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading && name.isNotBlank()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                } else {
                                    Text(stringResource(R.string.btn_finish))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepTextSize(viewModel: HealthViewModel) {
    val settings by viewModel.settings.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_step_text_size),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Live Preview Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_text_preview),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ABC 123",
                    fontSize = (24 * settings.textSizeMultiplier).sp
                )
                Text(
                    text = "Sample sentence for readability.",
                    fontSize = (16 * settings.textSizeMultiplier).sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val sizes = listOf(0.8f to "S", 1.0f to "M", 1.2f to "L", 1.5f to "XL")
            sizes.forEach { (scale, label) ->
                FilterChip(
                    selected = settings.textSizeMultiplier == scale,
                    onClick = { viewModel.setTextSize(scale) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
fun StepLayoutMode(viewModel: HealthViewModel) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_step_layout),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedCard(
                onClick = { viewModel.setLayoutMode("Normal") },
                border = if (settings.layoutMode == "Normal") CardDefaults.outlinedCardBorder(true) else CardDefaults.outlinedCardBorder(false),
                modifier = Modifier.fillMaxWidth(),
                colors = if (settings.layoutMode == "Normal") CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.outlinedCardColors()
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = settings.layoutMode == "Normal", onClick = { viewModel.setLayoutMode("Normal") })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.layout_normal), style = MaterialTheme.typography.titleLarge)
                }
            }

            OutlinedCard(
                onClick = { viewModel.setLayoutMode("Simplified") },
                border = if (settings.layoutMode == "Simplified") CardDefaults.outlinedCardBorder(true) else CardDefaults.outlinedCardBorder(false),
                modifier = Modifier.fillMaxWidth(),
                colors = if (settings.layoutMode == "Simplified") CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.outlinedCardColors()
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = settings.layoutMode == "Simplified", onClick = { viewModel.setLayoutMode("Simplified") })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.layout_simplified), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.layout_simplified_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun StepProfileCreation(
    viewModel: HealthViewModel,
    name: String,
    onNameChange: (String) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    waterGoal: String,
    onWaterGoalChange: (String) -> Unit
) {
    var lastRecommendedGoal by remember { mutableIntStateOf(2000) }

    val recommendedGoal = remember(weight) {
        val w = weight.toFloatOrNull() ?: 0f
        if (w > 0) (w * 35).toInt() else 2000
    }

    LaunchedEffect(recommendedGoal) {
        if (waterGoal.isEmpty() || waterGoal == "2000" || waterGoal == lastRecommendedGoal.toString()) {
            onWaterGoalChange(recommendedGoal.toString())
        }
        lastRecommendedGoal = recommendedGoal
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.new_user_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = weight,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' }) onWeightChange(input)
                },
                label = { Text(stringResource(R.string.weight_hint)) },
                placeholder = { Text(stringResource(R.string.optional_hint), color = Color.Gray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = height,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' }) onHeightChange(input)
                },
                label = { Text(stringResource(R.string.height_hint)) },
                placeholder = { Text(stringResource(R.string.optional_hint), color = Color.Gray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = waterGoal,
            onValueChange = { input ->
                if (input.all { it.isDigit() }) onWaterGoalChange(input)
            },
            label = { Text(stringResource(R.string.water_goal_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                Text(
                    stringResource(R.string.recommended_goal, recommendedGoal),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
