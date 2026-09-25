package com.besomuncu.healthcompanion.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.util.Log
import android.os.PowerManager
import androidx.lifecycle.*
import androidx.work.*
import com.besomuncu.healthcompanion.R
import com.besomuncu.healthcompanion.data.HealthDatabase
import com.besomuncu.healthcompanion.data.model.*
import com.besomuncu.healthcompanion.data.repository.HealthRepository
import com.besomuncu.healthcompanion.receiver.MedicationReceiver
import com.google.gson.Gson
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.text.SimpleDateFormat
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.*

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HealthRepository
    val allUsers: StateFlow<List<User>>
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _isDebugMode = MutableStateFlow(false)
    val isDebugMode: StateFlow<Boolean> = _isDebugMode.asStateFlow()

    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    fun setShowOnboarding(show: Boolean) {
        _showOnboarding.value = show
        if (show) {
            _settings.value = AppSettings()
        } else {
            _currentUser.value?.let { selectUser(it) }
        }
    }

    fun toggleDebugMode() {
        _isDebugMode.value = !_isDebugMode.value
    }

    fun updateTheme(theme: String) {
        val newSettings = _settings.value.copy(theme = theme)
        _settings.value = newSettings
        val user = _currentUser.value
        if (user != null && !_showOnboarding.value) {
            viewModelScope.launch {
                repository.saveSettings(newSettings)
            }
        }
    }

    suspend fun updateLanguage(lang: String) {
        val newSettings = _settings.value.copy(language = lang)
        _settings.value = newSettings
        val user = _currentUser.value
        if (user != null && !_showOnboarding.value) {
            repository.saveSettings(newSettings)
        }
    }

    fun updateTimeFormat(use24Hour: Boolean) {
        val newSettings = _settings.value.copy(use24HourFormat = use24Hour)
        _settings.value = newSettings
        val user = _currentUser.value
        if (user != null && !_showOnboarding.value) {
            viewModelScope.launch {
                repository.saveSettings(newSettings)
            }
        }
    }

    fun updateDateFormat(format: String) {
        val newSettings = _settings.value.copy(dateFormat = format)
        _settings.value = newSettings
        val user = _currentUser.value
        if (user != null && !_showOnboarding.value) {
            viewModelScope.launch {
                repository.saveSettings(newSettings)
            }
        }
    }

    fun setTextSize(multiplier: Float) {
        val newSettings = _settings.value.copy(textSizeMultiplier = multiplier)
        _settings.value = newSettings
        val user = _currentUser.value
        if (user != null && !_showOnboarding.value) {
            viewModelScope.launch {
                repository.saveSettings(newSettings)
            }
        }
    }

    fun setBpReminderInterval(interval: Int) {
        val newSettings = _settings.value.copy(bpReminderInterval = interval)
        _settings.value = newSettings
        val user = _currentUser.value
        if (user != null && !_showOnboarding.value) {
            viewModelScope.launch {
                repository.saveSettings(newSettings)
                scheduleBpReminders(user.id, forceUpdate = true)
            }
        }
    }

    fun setWaterReminderInterval(interval: Int) {
        val newSettings = _settings.value.copy(waterReminderInterval = interval)
        _settings.value = newSettings
        val user = _currentUser.value
        if (user != null && !_showOnboarding.value) {
            viewModelScope.launch {
                repository.saveSettings(newSettings)
                scheduleWaterReminders(user.id, forceUpdate = true)
            }
        }
    }

    fun setLayoutMode(mode: String) {
        val newSettings = _settings.value.copy(layoutMode = mode)
        _settings.value = newSettings
        val user = _currentUser.value
        if (user != null && !_showOnboarding.value) {
            viewModelScope.launch {
                repository.saveSettings(newSettings)
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("global_auto_backup_enabled", enabled).apply()
        
        // Also update current state so UI reflects it immediately
        _settings.value = _settings.value.copy(autoBackupEnabled = enabled)
    }

    private fun isAutoBackupEnabledGlobal(): Boolean {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("global_auto_backup_enabled", false)
    }

    private fun saveGlobalBackupTimestamp(timestamp: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("global_last_backup_timestamp", timestamp).apply()
    }

    private fun getGlobalBackupTimestamp(): Long {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("global_last_backup_timestamp", 0L)
    }

    private fun getActiveUserId(): Long {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("active_user_id", -1L)
    }

    private fun setActiveUserId(userId: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("active_user_id", userId).apply()
    }

    private fun saveLastBackupUri(uri: String) {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_backup_uri", uri).apply()
    }

    private fun getLastBackupUri(): Uri? {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val uriString = prefs.getString("last_backup_uri", null)
        return if (uriString != null) Uri.parse(uriString) else null
    }

    init {
        val dao = HealthDatabase.getDatabase(application).healthDao()
        repository = HealthRepository(dao)
        
        // Detect system defaults for initial launch/onboarding
        val is24h = android.text.format.DateFormat.is24HourFormat(application)
        val systemLang = Locale.getDefault().language
        val defaultLang = if (systemLang == "tr") "Turkish" else "English"
        val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        
        _settings.value = AppSettings(
            userId = 0,
            use24HourFormat = is24h,
            language = defaultLang,
            lastResetTimestamp = todayStart,
            autoBackupEnabled = isAutoBackupEnabledGlobal(),
            lastBackupTimestamp = getGlobalBackupTimestamp()
        )

        allUsers = repository.getAllUsers().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            val activeId = getActiveUserId()
            val users = repository.getAllUsers().first()
            
            if (activeId != -1L) {
                val activeUser = users.find { it.id == activeId }
                if (activeUser != null) {
                    selectUser(activeUser)
                } else if (users.isNotEmpty()) {
                    selectUser(users.first())
                } else {
                    _isLoading.value = false
                }
            } else if (users.isNotEmpty()) {
                selectUser(users.first())
            } else {
                _isLoading.value = false
            }
            
            // Periodic Day Change Check
            while (true) {
                delay(60000) // Check every minute
                _currentUser.value?.let { checkAndArchiveMissedDays(it) }
            }
        }
    }

    private suspend fun checkAndArchiveMissedDays(user: User) {
        val s = repository.getSettingsForUserOnce(user.id) ?: return
        var lastReset = s.lastResetTimestamp
        val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        
        if (lastReset > 0 && lastReset < today) {
            while (lastReset < today) {
                archiveAndResetDay(user.id, lastReset)
                lastReset += 86400000
            }
            val updatedSettings = s.copy(lastResetTimestamp = today)
            repository.saveSettings(updatedSettings)
            _settings.value = updatedSettings
            triggerAutoBackup()
        }
    }

    private fun calculateInitialDelay(interval: Int): Long {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val nextSlotHour = ((currentHour / interval) + 1) * interval
        
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, nextSlotHour % 24)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (nextSlotHour >= 24) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val delay = targetCalendar.timeInMillis - calendar.timeInMillis
        return if (delay < 0) 0 else delay
    }

    private fun scheduleWaterReminders(userId: Long, forceUpdate: Boolean = false) {
        val interval = _settings.value.waterReminderInterval
        val delayMs = calculateInitialDelay(interval)
        
        val data = Data.Builder().putLong("user_id", userId).build()
        val request = PeriodicWorkRequestBuilder<com.besomuncu.healthcompanion.worker.WaterReminderWorker>(interval.toLong(), java.util.concurrent.TimeUnit.HOURS)
            .setInputData(data)
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        
        val policy = if (forceUpdate) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.UPDATE

        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            "water_reminder_$userId",
            policy,
            request
        )
    }

    private fun scheduleBpReminders(userId: Long, forceUpdate: Boolean = false) {
        val interval = _settings.value.bpReminderInterval
        val delayMs = calculateInitialDelay(interval)

        val data = Data.Builder().putLong("user_id", userId).build()
        val request = PeriodicWorkRequestBuilder<com.besomuncu.healthcompanion.worker.BpReminderWorker>(interval.toLong(), java.util.concurrent.TimeUnit.HOURS)
            .setInputData(data)
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        val policy = if (forceUpdate) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.UPDATE

        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            "bp_reminder_$userId",
            policy,
            request
        )
    }

    private var settingsJob: Job? = null

    fun selectUser(user: User) {
        _currentUser.value = user
        setActiveUserId(user.id)
        settingsJob?.cancel()
        settingsJob = viewModelScope.launch {
            repository.getSettingsForUser(user.id).collect { s ->
                if (s != null) {
                    val globalAutoBackup = isAutoBackupEnabledGlobal()
                    val globalTimestamp = getGlobalBackupTimestamp()
                    
                    val combinedSettings = s.copy(
                        autoBackupEnabled = globalAutoBackup,
                        lastBackupTimestamp = globalTimestamp
                    )
                    
                    _settings.value = combinedSettings
                    _isLoading.value = false
                    
                    checkAndArchiveMissedDays(user)
                    
                    scheduleWaterReminders(user.id)
                    scheduleBpReminders(user.id)
                    scheduleAllMedicationAlarms(user.id)
                } else {
                    val is24h = android.text.format.DateFormat.is24HourFormat(getApplication())
                    val defaultLang = if (Locale.getDefault().language == "tr") "Turkish" else "English"
                    val newSettings = AppSettings(
                        userId = user.id,
                        use24HourFormat = is24h,
                        language = defaultLang,
                        lastResetTimestamp = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    )
                    repository.saveSettings(newSettings)
                    _settings.value = newSettings
                    _isLoading.value = false
                    
                    scheduleWaterReminders(user.id)
                    scheduleBpReminders(user.id)
                    scheduleAllMedicationAlarms(user.id)
                }
            }
        }
    }

    private fun scheduleAllMedicationAlarms(userId: Long) {
        viewModelScope.launch {
            val meds = repository.getMedicationsForUserOnce(userId)
            meds.forEach { med ->
                if (med.pillCount > 0 || med.initialPillCount == 0) {
                    scheduleMedication(med.name, med.scheduledTime)
                }
            }
        }
    }

    fun addUser(name: String, weight: Float, height: Float, waterGoal: Int) {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val newUser = User(name = name, weight = weight, height = height, dailyWaterGoal = waterGoal)
                val id = repository.insertUser(newUser)
                val createdUser = newUser.copy(id = id)
                
                // Save the settings established during onboarding for this user
                val currentOnboardingSettings = _settings.value.copy(userId = id)
                repository.saveSettings(currentOnboardingSettings)
                
                _showOnboarding.value = false
                selectUser(createdUser)
                triggerAutoBackup()
            } catch (e: Exception) {
                Log.e("HealthViewModel", "Failed to add user: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    fun updateWaterGoal(newGoal: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(dailyWaterGoal = newGoal)
            repository.insertUser(updatedUser)
            _currentUser.value = updatedUser
            triggerAutoBackup()
        }
    }

    fun resetDailyIntake() {
        val user = _currentUser.value ?: return
        val startDay = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        viewModelScope.launch {
            repository.resetDailyIntake(user.id, startDay)
            triggerAutoBackup()
        }
    }

    fun removeUser(user: User) {
        viewModelScope.launch {
            repository.deleteUser(user)
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = null
                val others = repository.getUsersOnce()
                if (others.isNotEmpty()) {
                    selectUser(others.first())
                }
            }
        }
    }

    val liquidTypes: StateFlow<List<LiquidType>> = repository.getAllLiquidTypes().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun addLiquidIntake(amountMl: Int, liquidTypeId: Long?, label: String = "Water") {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val type = if (liquidTypeId != null) {
                repository.getAllLiquidTypes().first().find { it.id == liquidTypeId }
            } else null
            
            val factor = type?.hydrationFactor ?: 1.0f
            val effective = (amountMl * factor).toInt()
            
            repository.insertIntake(
                LiquidIntake(
                    userId = user.id, 
                    liquidTypeId = liquidTypeId, 
                    amountMl = amountMl, 
                    effectiveHydrationMl = effective,
                    label = if (liquidTypeId == null) "Water" else label
                )
            )
            triggerAutoBackup()
        }
    }

    fun deleteIntake(intake: LiquidIntake) {
        viewModelScope.launch {
            repository.deleteIntake(intake)
            triggerAutoBackup()
        }
    }

    fun deleteFavoriteIntake(favorite: FavoriteIntake) {
        viewModelScope.launch {
            repository.deleteFavoriteIntake(favorite)
            triggerAutoBackup()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val intakes: StateFlow<List<LiquidIntake>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getIntakesForUser(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val favoriteIntakes: StateFlow<List<FavoriteIntake>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getFavoriteIntakes(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFavoriteIntake(amountMl: Int, label: String, liquidTypeId: Long? = null) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertFavoriteIntake(
                FavoriteIntake(
                    userId = user.id, 
                    amountMl = amountMl, 
                    label = if (liquidTypeId == null) "Water" else label, 
                    liquidTypeId = liquidTypeId
                )
            )
            triggerAutoBackup()
        }
    }

    fun addLiquidTypeWithFavorite(name: String, hydrationFactor: Float, amountMl: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val typeId = repository.insertLiquidType(LiquidType(name = name, hydrationFactor = hydrationFactor))
            repository.insertFavoriteIntake(FavoriteIntake(userId = user.id, amountMl = amountMl, label = name, liquidTypeId = typeId))
            triggerAutoBackup()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dailyWaterTotal: StateFlow<Int> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getDailyTotal(user.id) }
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val medications: StateFlow<List<Medication>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getMedicationsForUser(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nextMedication: StateFlow<Medication?> = medications.map { list ->
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val (overdue, upcoming) = list.filter { !it.isTakenToday && (it.pillCount > 0 || it.initialPillCount == 0) }
            .partition { it.scheduledTime <= currentTime }
        
        overdue.sortedBy { it.scheduledTime }.firstOrNull() 
            ?: upcoming.sortedBy { it.scheduledTime }.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addMedication(name: String, illness: String, method: String, time: String, foodContext: String, pillCount: Int, dosage: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertMedication(Medication(userId = user.id, name = name, illness = illness, intakeMethod = method, scheduledTime = time, foodContext = foodContext, initialPillCount = pillCount, pillCount = pillCount, dosagePerIntake = dosage))
            scheduleMedication(name, time)
            triggerAutoBackup()
        }
    }

    fun toggleMedicationTaken(medication: Medication) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val isTaking = !medication.isTakenToday
            var updatedMed = medication.copy(isTakenToday = isTaking)
            
            if (medication.initialPillCount > 0) {
                if (isTaking) {
                    if (medication.pillCount > 0) {
                        updatedMed = updatedMed.copy(pillCount = (medication.pillCount - medication.dosagePerIntake).coerceAtLeast(0))
                    }
                } else {
                    updatedMed = updatedMed.copy(pillCount = medication.pillCount + medication.dosagePerIntake)
                }
            }
            
            repository.updateMedication(updatedMed)
            
            if (updatedMed.pillCount <= 0 && medication.initialPillCount > 0 || isTaking) {
                cancelAlarm(updatedMed)
                cancelOverdueAlarm(updatedMed)
            }
            
            if (!isTaking && (updatedMed.pillCount > 0 || medication.initialPillCount <= 0)) {
                scheduleMedication(updatedMed.name, updatedMed.scheduledTime)
            }
            
            val startDay = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            
            if (isTaking) {
                repository.insertMedicationLog(MedicationLog(userId = user.id, medicationId = medication.id, medName = medication.name))
            } else {
                repository.deleteTodayMedicationLog(user.id, medication.id, startDay)
            }
            triggerAutoBackup()
        }
    }

    fun refillMedication(medication: Medication) {
        viewModelScope.launch {
            val updatedMed = medication.copy(pillCount = medication.initialPillCount)
            repository.updateMedication(updatedMed)
            scheduleMedication(updatedMed.name, updatedMed.scheduledTime)
            triggerAutoBackup()
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            repository.updateMedication(medication)
            scheduleMedication(medication.name, medication.scheduledTime)
            triggerAutoBackup()
        }
    }

    fun createSystemAlarm(medication: Medication) {
        if (medication.pillCount <= 0) return

        val parts = medication.scheduledTime.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, medication.name)
                putExtra(AlarmClock.EXTRA_DAYS, arrayListOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY))
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            try {
                val simpleIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, medication.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                getApplication<Application>().startActivity(simpleIntent)
            } catch (ex: Exception) {
                Log.e("HealthViewModel", "Failed to open clock app: ${ex.message}")
            }
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
            cancelAlarm(medication)
            triggerAutoBackup()
        }
    }

    private fun cancelAlarm(medication: Medication) {
        val intent = Intent(getApplication(), MedicationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            medication.name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun cancelOverdueAlarm(medication: Medication) {
        val intent = Intent(getApplication(), com.besomuncu.healthcompanion.receiver.OverdueReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            medication.name.hashCode() + 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(medication.name.hashCode() + 100)
    }
    
    fun toggleAlarmManually(medication: Medication, stop: Boolean) {
        if (stop) {
            cancelAlarm(medication)
        } else {
            scheduleMedication(medication.name, medication.scheduledTime)
        }
    }

    private fun scheduleMedication(medName: String, time: String) {
        try {
            val parts = time.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toIntOrNull() ?: return
            val minute = parts[1].toIntOrNull() ?: return

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            val user = _currentUser.value ?: return
            val intent = Intent(getApplication(), MedicationReceiver::class.java).apply {
                putExtra("med_name", medName)
                putExtra("user_id", user.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                getApplication(),
                medName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e("HealthViewModel", "Cannot schedule exact alarms. Permission missing.")
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    return
                }
            }
            
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e("HealthViewModel", "Failed to schedule medication: ${e.message}")
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val bloodPressureHistory: StateFlow<List<BloodPressure>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getBloodPressureHistory(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBloodPressure(systolic: Int, diastolic: Int, pulse: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertBloodPressure(BloodPressure(userId = user.id, systolic = systolic, diastolic = diastolic, pulse = pulse))
            triggerAutoBackup()
        }
    }

    fun updateBloodPressure(reading: BloodPressure) {
        viewModelScope.launch {
            repository.updateBloodPressure(reading)
            triggerAutoBackup()
        }
    }

    fun deleteBloodPressure(reading: BloodPressure) {
        viewModelScope.launch {
            repository.deleteBloodPressure(reading)
            triggerAutoBackup()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dailySummaries: StateFlow<List<DailySummary>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getDailySummaries(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDailySummary(summary: DailySummary) {
        viewModelScope.launch {
            repository.deleteDailySummary(summary)
            triggerAutoBackup()
        }
    }

    fun deleteAllDailySummaries() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.getDailySummaries(user.id).first().forEach {
                repository.deleteDailySummary(it)
            }
            triggerAutoBackup()
        }
    }

    fun getIntakesForDay(userId: Long, timestamp: Long): StateFlow<List<LiquidIntake>> {
        val start = Calendar.getInstance().apply { timeInMillis = timestamp; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val end = start + 86400000
        val flow = MutableStateFlow<List<LiquidIntake>>(emptyList())
        viewModelScope.launch {
            flow.value = repository.getIntakesInRange(userId, start, end)
        }
        return flow.asStateFlow()
    }

    suspend fun getIntakesForSummary(summary: DailySummary): List<LiquidIntake> {
        val start = Calendar.getInstance().apply { timeInMillis = summary.date; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val end = start + 86400000
        return repository.getIntakesInRange(summary.userId, start, end)
    }

    suspend fun getMedicationLogsForSummary(summary: DailySummary): List<MedicationLog> {
        val start = Calendar.getInstance().apply { timeInMillis = summary.date; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val end = start + 86400000
        return repository.getMedicationLogsInRange(summary.userId, start, end)
    }

    private fun getLocalizedContext(): Context {
        val context = getApplication<Application>()
        val locale = if (_settings.value.language == "Turkish") Locale("tr") else Locale("en")
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun simulateBpReminder() {
        val user = _currentUser.value ?: return
        val intent = Intent(getApplication(), com.besomuncu.healthcompanion.MainActivity::class.java).apply {
            putExtra("navigate_to", "blood_pressure")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            getApplication(),
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val laterIntent = Intent(getApplication(), com.besomuncu.healthcompanion.receiver.NotificationActionReceiver::class.java).apply {
            action = "ACTION_REMIND_LATER"
            putExtra("type", "bp")
            putExtra("user_id", user.id)
            putExtra("notification_id", 2001)
        }
        val laterPendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            20012,
            laterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val localizedContext = getLocalizedContext()
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "bp_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = localizedContext.getString(R.string.notif_bp_title)
            val channel = android.app.NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = androidx.core.app.NotificationCompat.Builder(getApplication(), channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(localizedContext.getString(R.string.notif_bp_title))
            .setContentText(localizedContext.getString(R.string.notif_bp_text))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_view, localizedContext.getString(R.string.notif_action_open), pendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, localizedContext.getString(R.string.notif_action_remind_later), laterPendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(2001, notification)
    }

    fun simulateWaterReminder() {
        val user = _currentUser.value ?: return
        val intent = Intent(getApplication(), com.besomuncu.healthcompanion.MainActivity::class.java).apply {
            putExtra("navigate_to", "liquids")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            getApplication(),
            2002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val laterIntent = Intent(getApplication(), com.besomuncu.healthcompanion.receiver.NotificationActionReceiver::class.java).apply {
            action = "ACTION_REMIND_LATER"
            putExtra("type", "water")
            putExtra("user_id", user.id)
            putExtra("notification_id", 2002)
        }
        val laterPendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            20022,
            laterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val localizedContext = getLocalizedContext()
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "water_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = localizedContext.getString(R.string.notif_hydration_title)
            val channel = android.app.NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = androidx.core.app.NotificationCompat.Builder(getApplication(), channelId)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(localizedContext.getString(R.string.notif_hydration_title))
            .setContentText(localizedContext.getString(R.string.notif_hydration_text))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_view, localizedContext.getString(R.string.notif_action_open), pendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, localizedContext.getString(R.string.notif_action_remind_later), laterPendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(2002, notification)
    }

    fun simulateMedicationReminder() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val meds = repository.getMedicationsForUserOnce(user.id)
            val med = meds.firstOrNull() ?: Medication(userId = user.id, name = "Sample Med", illness = "Health", intakeMethod = "Pill", scheduledTime = "12:00")
            
            val intent = Intent(getApplication(), com.besomuncu.healthcompanion.MainActivity::class.java).apply {
                putExtra("med_name", med.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                getApplication(),
                med.name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val takenIntent = Intent(getApplication(), com.besomuncu.healthcompanion.receiver.NotificationActionReceiver::class.java).apply {
                action = "ACTION_TAKEN"
                putExtra("med_name", med.name)
                putExtra("user_id", user.id)
                putExtra("notification_id", med.name.hashCode())
            }
            val takenPendingIntent = PendingIntent.getBroadcast(
                getApplication(),
                med.name.hashCode() + 200,
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val laterIntent = Intent(getApplication(), com.besomuncu.healthcompanion.receiver.NotificationActionReceiver::class.java).apply {
                action = "ACTION_REMIND_LATER"
                putExtra("med_name", med.name)
                putExtra("user_id", user.id)
                putExtra("notification_id", med.name.hashCode())
            }
            val laterPendingIntent = PendingIntent.getBroadcast(
                getApplication(),
                med.name.hashCode() + 300,
                laterIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val localizedContext = getLocalizedContext()
            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "medication_reminders"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(channelId, "Medication Reminders", android.app.NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }
            
            val notification = androidx.core.app.NotificationCompat.Builder(getApplication(), channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(localizedContext.getString(R.string.notif_med_title))
                .setContentText(localizedContext.getString(R.string.notif_med_text, med.name))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_edit, localizedContext.getString(R.string.notif_action_taken), takenPendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history, localizedContext.getString(R.string.notif_action_remind_later), laterPendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(med.name.hashCode(), notification)
        }
    }

    fun isBatteryOptimizing(): Boolean {
        val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !pm.isIgnoringBatteryOptimizations(getApplication<Application>().packageName)
        } else false
    }

    private suspend fun archiveAndResetDay(userId: Long?, dateTimestamp: Long) {
        val uid = userId ?: return
        val startDay = dateTimestamp
        val endDay = dateTimestamp + 86400000
        
        val intakes = repository.getIntakesInRange(uid, startDay, endDay)
        val totalWater = intakes.filter { it.liquidTypeId == null }.sumOf { it.amountMl }
        val totalOther = intakes.filter { it.liquidTypeId != null }.sumOf { it.amountMl }
        
        val meds = repository.getMedsForUserOnce(uid)
        val takenCount = meds.count { it.isTakenToday }
        val untakenNames = meds.filter { !it.isTakenToday && (it.pillCount > 0 || it.initialPillCount == 0) }.joinToString(", ") { it.name }
        
        val bpHistory = repository.getBloodPressureHistory(uid).first()
        val avgSys = if (bpHistory.isNotEmpty()) bpHistory.map { it.systolic }.average().toInt() else 0
        val avgDia = if (bpHistory.isNotEmpty()) bpHistory.map { it.diastolic }.average().toInt() else 0

        repository.insertDailySummary(
            DailySummary(
                userId = uid,
                date = startDay,
                totalWaterMl = totalWater,
                totalOtherMl = totalOther,
                medicationsTakenCount = takenCount,
                medicationsUntakenNames = untakenNames,
                averageSystolic = avgSys,
                averageDiastolic = avgDia
            )
        )
        
        repository.resetMedicationsForDay(uid)
        
        meds.forEach { cancelOverdueAlarm(it) }
    }

    fun simulateDayPassed() {
        val user = _currentUser.value ?: return
        val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        viewModelScope.launch {
            archiveAndResetDay(user.id, todayStart)
            repository.simulateDayPassed(user.id)
            repository.simulateDayPassedLogs(user.id)
            repository.simulateDayPassedSummaries(user.id)
            triggerAutoBackup()
        }
    }

    fun exportData(context: Context, userId: Long?) {
        viewModelScope.launch {
            com.besomuncu.healthcompanion.util.BackupUtils.exportDataInternal(context, userId, isAuto = false)
        }
    }

    private fun triggerAutoBackup() {
        viewModelScope.launch {
            com.besomuncu.healthcompanion.util.BackupUtils.triggerAutoBackup(getApplication())
        }
    }

    private fun findLatestBackupUri(context: Context): Uri? {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Files.getContentUri("external")
        }

        // Try to find files created by ANY app with our naming pattern (Manual or Auto)
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE 'Health_Backup_%.json' OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE 'Health_AutoBackup_%.json'"
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val id = cursor.getLong(idColumn)
                    return android.content.ContentUris.withAppendedId(collection, id)
                }
            }
        } catch (e: Exception) {
            Log.e("HealthViewModel", "Autofind failed", e)
        }
        return null
    }

    fun shareLatestData(context: Context) {
        val uri = getLastBackupUri() ?: findLatestBackupUri(context)
        if (uri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_backup))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } else {
            Toast.makeText(context, context.getString(R.string.error_no_backup_found), Toast.LENGTH_SHORT).show()
        }
    }

    fun importLatestOrPicker(context: Context, onPickerRequired: () -> Unit) {
        val uri = findLatestBackupUri(context)
        if (uri != null) {
            importData(context, uri)
        } else {
            Toast.makeText(context, context.getString(R.string.error_autofind_failed), Toast.LENGTH_LONG).show()
            onPickerRequired()
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val resolver = context.contentResolver
                val json = resolver.openInputStream(uri)?.use { 
                    it.bufferedReader().readText()
                }
                
                if (json != null) {
                    val backupData = Gson().fromJson(json, BackupData::class.java)
                    
                    saveLastBackupUri(uri.toString())
                    
                    // Smart Import Logic
                    val existingUsers = repository.getUsersOnce()
                    val existingUserNames = existingUsers.map { it.name }.toMutableSet()
                    val existingLiquidTypes = repository.getAllLiquidTypes().first()
                    val existingLiquidTypeNames = existingLiquidTypes.map { it.name }

                    // Map Old IDs to New IDs
                    val userIdMap = mutableMapOf<Long, Long>()
                    val liquidTypeIdMap = mutableMapOf<Long, Long>()

                    // 1. Restore Users and Create ID Map
                    backupData.users.forEach { backupUser ->
                        var newName = backupUser.name
                        var counter = 1
                        while (existingUserNames.contains(newName)) {
                            newName = "${backupUser.name} ($counter)"
                            counter++
                        }
                        existingUserNames.add(newName)
                        
                        val newUser = backupUser.copy(id = 0, name = newName)
                        val newId = repository.insertUser(newUser)
                        userIdMap[backupUser.id] = newId
                    }

                    // 2. Restore Liquid Types and Create ID Map
                    backupData.liquidTypes.forEach { type ->
                        if (!existingLiquidTypeNames.contains(type.name)) {
                            val newId = repository.insertLiquidType(type.copy(id = 0))
                            liquidTypeIdMap[type.id] = newId
                        } else {
                            val existingId = existingLiquidTypes.find { it.name == type.name }?.id
                            if (existingId != null) liquidTypeIdMap[type.id] = existingId
                        }
                    }

                    // 3. Restore Related Data with New IDs
                    val importedSettings = backupData.appSettings
                        .filter { userIdMap.containsKey(it.userId) }
                        .map { it.copy(userId = userIdMap[it.userId]!!) }
                    if (importedSettings.isNotEmpty()) repository.saveSettingsBatch(importedSettings)

                    val importedMeds = backupData.medications
                        .filter { userIdMap.containsKey(it.userId) }
                        .map { it.copy(id = 0, userId = userIdMap[it.userId]!!) }
                    if (importedMeds.isNotEmpty()) repository.insertMedications(importedMeds)

                    val importedBp = backupData.bloodPressures
                        .filter { userIdMap.containsKey(it.userId) }
                        .map { it.copy(id = 0, userId = userIdMap[it.userId]!!) }
                    if (importedBp.isNotEmpty()) repository.insertBloodPressures(importedBp)

                    val importedIntakes = backupData.liquidIntakes
                        .filter { userIdMap.containsKey(it.userId) }
                        .map { it.copy(
                            id = 0, 
                            userId = userIdMap[it.userId]!!,
                            liquidTypeId = if (it.liquidTypeId != null) liquidTypeIdMap[it.liquidTypeId] else null
                        ) }
                    if (importedIntakes.isNotEmpty()) repository.insertLiquidIntakes(importedIntakes)

                    val importedFavorites = backupData.favoriteIntakes
                        .filter { userIdMap.containsKey(it.userId) }
                        .map { it.copy(
                            id = 0, 
                            userId = userIdMap[it.userId]!!,
                            liquidTypeId = if (it.liquidTypeId != null) liquidTypeIdMap[it.liquidTypeId] else null
                        ) }
                    if (importedFavorites.isNotEmpty()) repository.insertFavoriteIntakes(importedFavorites)

                    val importedSummaries = backupData.dailySummaries
                        .filter { userIdMap.containsKey(it.userId) }
                        .map { it.copy(id = 0, userId = userIdMap[it.userId]!!) }
                    if (importedSummaries.isNotEmpty()) repository.insertDailySummaries(importedSummaries)

                    val importedLogs = backupData.medicationLogs
                        .filter { userIdMap.containsKey(it.userId) }
                        .map { it.copy(id = 0, userId = userIdMap[it.userId]!!) }
                    if (importedLogs.isNotEmpty()) repository.insertMedicationLogs(importedLogs)

                    Toast.makeText(context, context.getString(R.string.backup_imported), Toast.LENGTH_LONG).show()
                    
                    // Reload current user
                    val users = repository.getUsersOnce()
                    if (users.isNotEmpty()) {
                        val activeId = getActiveUserId()
                        val currentUser = users.find { it.id == activeId } ?: users.first()
                        selectUser(currentUser)
                    }
                    triggerAutoBackup()
                }
            } catch (e: Exception) {
                Log.e("HealthViewModel", "Import failed", e)
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateUserName(user: User, newName: String) {
        viewModelScope.launch {
            val updatedUser = user.copy(name = newName)
            repository.insertUser(updatedUser) // Upsert handles update
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = updatedUser
            }
            triggerAutoBackup()
        }
    }
}
