package com.siddumoni.fakecall

import android.Manifest
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.callers.CallerPresetsViewModel
import com.siddumoni.fakecall.data.preferences.CallerPresetRepository
import com.siddumoni.fakecall.data.preferences.ScheduledCallRepository
import com.siddumoni.fakecall.notifications.ScheduledCallLink
import com.siddumoni.fakecall.notifications.ScheduledCallNotifications
import com.siddumoni.fakecall.onboarding.OnboardingViewModel
import com.siddumoni.fakecall.scheduling.ScheduledCallAction
import com.siddumoni.fakecall.scheduling.ScheduledCallAlarmScheduler
import com.siddumoni.fakecall.scheduling.ScheduledCallState
import com.siddumoni.fakecall.scheduling.ScheduledCallsViewModel
import com.siddumoni.fakecall.settings.ThemeSettingsViewModel
import com.siddumoni.fakecall.ui.AlibiCallApp
import com.siddumoni.fakecall.ui.AppScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeSettingsViewModel by viewModels()
    private val callerPresetsViewModel: CallerPresetsViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val scheduledCallsViewModel: ScheduledCallsViewModel by viewModels()
    private var activeScheduledCallId by mutableStateOf<String?>(null)
    private var notificationsAllowed by mutableStateOf(false)
    private var exactAlarmAllowed by mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshSchedulePermissions()
            scheduledCallsViewModel.reconcilePendingAlarms(notificationsAllowed)
        }
    private val exactAlarmSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshSchedulePermissions()
            scheduledCallsViewModel.reconcilePendingAlarms(notificationsAllowed)
        }
    private var currentScreen by mutableStateOf(AppScreen.HOME)
    private var selectedCaller by mutableStateOf<CallerPreset?>(null)
    private var orphanCleanup: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialOnboardingComplete = (application as AlibiCallApplication).initialOnboardingCompleted
        val restoredScreen =
            savedInstanceState
                ?.getString(STATE_CURRENT_SCREEN)
                ?.let { restored -> AppScreen.entries.firstOrNull { it.name == restored } }
        currentScreen =
            if (initialOnboardingComplete) {
                restoredScreen
                    ?.takeUnless { restored -> restored == AppScreen.ONBOARDING }
                    ?: AppScreen.HOME
            } else {
                AppScreen.ONBOARDING
            }
        activeScheduledCallId = savedInstanceState?.getString(STATE_SCHEDULED_CALL_ID)
        selectedCaller =
            savedInstanceState?.let {
                val id = it.getString(STATE_CALLER_ID)
                val name = it.getString(STATE_CALLER_NAME)
                val number = it.getString(STATE_CALLER_NUMBER)
                if (id != null && name != null && number != null) CallerPreset(id, name, number) else null
            }
        // Must finish before a launch intent is handled, so cleanup cannot end a just-answered call.
        orphanCleanup =
            if (savedInstanceState == null) scheduledCallsViewModel.endOrphanedCalls() else null
        enableEdgeToEdge()
        ScheduledCallNotifications.ensureChannel(this)
        refreshSchedulePermissions()
        setContent {
            val settings by themeViewModel.settings.collectAsState()
            val saveFailed by themeViewModel.saveFailed.collectAsState()
            val callerPresets by callerPresetsViewModel.presets.collectAsState()
            val callerPresetSaveError by callerPresetsViewModel.saveError.collectAsState()
            val onboardingCompleted by onboardingViewModel.completed.collectAsState()
            val onboardingSaveFailed by onboardingViewModel.saveFailed.collectAsState()
            val scheduledCalls by scheduledCallsViewModel.calls.collectAsState()
            val scheduleOperationState by scheduledCallsViewModel.operationState.collectAsState()

            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted && currentScreen == AppScreen.ONBOARDING) {
                    currentScreen = AppScreen.HOME
                }
            }

            BackHandler(
                enabled = currentScreen != AppScreen.HOME && currentScreen != AppScreen.ONBOARDING,
            ) {
                finishScheduledCallIfNeeded()
                currentScreen = AppScreen.HOME
                selectedCaller = null
            }

            AlibiCallApp(
                settings = settings,
                onAppearanceChanged = themeViewModel::setAppearance,
                onPureBlackChanged = themeViewModel::setPureBlack,
                onDynamicColorChanged = themeViewModel::setDynamicColor,
                saveFailed = saveFailed,
                onboardingSaveFailed = onboardingSaveFailed,
                onCompleteOnboarding = onboardingViewModel::complete,
                screen = currentScreen,
                onOpenThemeSettings = { currentScreen = AppScreen.THEME_SETTINGS },
                onOpenCallerPresets = { currentScreen = AppScreen.CALLER_PRESETS },
                onOpenSchedules = { currentScreen = AppScreen.SCHEDULES },
                onOpenScheduleNotificationSettings = ::openScheduleNotificationSettings,
                onBack = {
                    finishScheduledCallIfNeeded()
                    currentScreen = AppScreen.HOME
                },
                callerPresets = callerPresets,
                callerPresetSaveError = callerPresetSaveError,
                onSaveCallerPreset = callerPresetsViewModel::savePreset,
                onDeleteCallerPreset = callerPresetsViewModel::deletePreset,
                selectedCaller = selectedCaller,
                onPreviewCaller = { caller ->
                    selectedCaller = caller
                    currentScreen = AppScreen.SIMULATED_CALL
                },
                onAnswerSimulatedCall = { answerScheduledCall() },
                onEndSimulatedCall = {
                    finishScheduledCallIfNeeded()
                    selectedCaller = null
                    currentScreen = AppScreen.HOME
                },
                scheduledCalls = scheduledCalls,
                scheduleOperationState = scheduleOperationState,
                notificationsAllowed = notificationsAllowed,
                exactAlarmAllowed = exactAlarmAllowed,
                onSaveSchedule = scheduledCallsViewModel::save,
                onCancelSchedule = scheduledCallsViewModel::cancel,
                onDeleteSchedule = scheduledCallsViewModel::delete,
                onRequestNotificationPermission = ::requestNotificationPermission,
                onRequestExactAlarmPermission = ::requestExactAlarmPermission,
                onClearScheduleOperation = scheduledCallsViewModel::clearOperationState,
            )
        }
        handleScheduledCallIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleScheduledCallIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshSchedulePermissions()
        scheduledCallsViewModel.reconcilePendingAlarms(notificationsAllowed)
    }

    private fun refreshSchedulePermissions() {
        notificationsAllowed = ScheduledCallNotifications.canPost(this)
        val manager = getSystemService(ALARM_SERVICE) as AlarmManager
        exactAlarmAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
        }
    }

    private fun requestExactAlarmPermission() {
        val request =
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:$packageName"),
            )
        exactAlarmSettingsLauncher.launch(request)
    }

    private fun openScheduleNotificationSettings() {
        val channelSettings =
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, ScheduledCallNotifications.CHANNEL_ID)
        try {
            startActivity(channelSettings)
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
        }
    }

    private fun handleScheduledCallIntent(intent: Intent?) {
        val data = intent?.data ?: return
        // Only our explicit notification URIs are accepted. Do not trust caller-supplied extras
        // or arbitrary schemes/authorities on this exported launcher Activity.
        if (intent.action != Intent.ACTION_VIEW) return
        val link = ScheduledCallLink.parse(data.scheme, data.authority, data.pathSegments) ?: return
        val id = link.scheduleId
        lifecycleScope.launch {
            orphanCleanup?.join()
            val repository = ScheduledCallRepository(this@MainActivity)
            val call =
                repository
                    .calls
                    .first()
                    .firstOrNull { it.id == id }
                    ?: return@launch
            if (call.state != ScheduledCallState.RINGING && call.state != ScheduledCallState.IN_CALL) {
                return@launch
            }
            val caller =
                CallerPresetRepository(this@MainActivity)
                    .presets
                    .first()
                    .firstOrNull { it.id == call.callerPresetId }
                    ?: return@launch
            var inCall = call.state == ScheduledCallState.IN_CALL
            if (link.answer && !inCall) {
                // False means the call timed out or was declined in the meantime.
                if (!repository.transition(id, ScheduledCallAction.ANSWER)) return@launch
                ScheduledCallAlarmScheduler(this@MainActivity).cancelTimeout(id)
                ScheduledCallNotifications.cancel(this@MainActivity, id)
                inCall = true
            }
            // Switching to another simulation must not leave the previous one answered forever.
            // END is rejected for a call that is only ringing, which keeps its own timeout.
            activeScheduledCallId
                ?.takeIf { previous -> previous != id }
                ?.let { previous -> repository.transition(previous, ScheduledCallAction.END) }
            selectedCaller = caller
            activeScheduledCallId = id
            currentScreen = if (inCall) AppScreen.SIMULATED_IN_CALL else AppScreen.SIMULATED_CALL
        }
    }

    private fun answerScheduledCall() {
        val id = activeScheduledCallId
        if (id == null) {
            currentScreen = AppScreen.SIMULATED_IN_CALL
            return
        }
        lifecycleScope.launch {
            if (
                ScheduledCallRepository(this@MainActivity)
                    .transition(id, ScheduledCallAction.ANSWER)
            ) {
                ScheduledCallAlarmScheduler(this@MainActivity).cancelTimeout(id)
                ScheduledCallNotifications.cancel(this@MainActivity, id)
                currentScreen = AppScreen.SIMULATED_IN_CALL
            } else {
                // The call already timed out or was dismissed: do not leave a dead Answer button.
                activeScheduledCallId = null
                selectedCaller = null
                currentScreen = AppScreen.HOME
            }
        }
    }

    private fun finishScheduledCallIfNeeded() {
        val id = activeScheduledCallId ?: return
        val action =
            if (currentScreen == AppScreen.SIMULATED_IN_CALL) {
                ScheduledCallAction.END
            } else {
                ScheduledCallAction.DECLINE
            }
        lifecycleScope.launch {
            ScheduledCallRepository(this@MainActivity).transition(id, action)
            ScheduledCallAlarmScheduler(this@MainActivity).cancelTimeout(id)
            ScheduledCallNotifications.cancel(this@MainActivity, id)
            activeScheduledCallId = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CURRENT_SCREEN, currentScreen.name)
        activeScheduledCallId?.let { id -> outState.putString(STATE_SCHEDULED_CALL_ID, id) }
        selectedCaller?.let { caller ->
            outState.putString(STATE_CALLER_ID, caller.id)
            outState.putString(STATE_CALLER_NAME, caller.displayName)
            outState.putString(STATE_CALLER_NUMBER, caller.numberLabel)
        }
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val STATE_CURRENT_SCREEN = "current_screen"
        const val STATE_SCHEDULED_CALL_ID = "active_scheduled_call_id"
        const val STATE_CALLER_ID = "selected_caller_id"
        const val STATE_CALLER_NAME = "selected_caller_name"
        const val STATE_CALLER_NUMBER = "selected_caller_number"
    }
}
