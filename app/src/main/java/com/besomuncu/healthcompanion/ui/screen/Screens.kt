package com.besomuncu.healthcompanion.ui.screen

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.besomuncu.healthcompanion.R

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: ImageVector, val iconColor: Color) {
    object Dashboard : Screen("dashboard", R.string.nav_dashboard, Icons.Default.Dashboard, Color(0xFF2196F3))
    object Liquids : Screen("liquids", R.string.nav_liquids, Icons.Default.LocalDrink, Color(0xFF4CAF50))
    object Medications : Screen("medications", R.string.nav_meds, Icons.Default.Medication, Color(0xFF9C27B0))
    object BloodPressure : Screen("blood_pressure", R.string.nav_bp, Icons.Default.Bloodtype, Color(0xFFE91E63))
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings, Color(0xFF607D8B))
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Liquids,
    Screen.Medications,
    Screen.BloodPressure,
    Screen.Settings
)
