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
import com.siddumoni.fakecall.call.CallActivity
import com.siddumoni.fakecall.callers.CallerPresetsViewModel
import com.siddumoni.fakecall.notifications.ScheduledCallNotifications
import com.siddumoni.fakecall.onboarding.OnboardingViewModel
import com.siddumoni.fakecall.scheduling.ScheduledCallsViewModel
import com.siddumoni.fakecall.settings.CallSettingsViewModel
import com.siddumoni.fakecall.settings.ThemeSettingsViewModel
import com.siddumoni.fakecall.trigger.VolumeTriggerService
import com.siddumoni.fakecall.ui.AlibiCallApp
import com.siddumoni.fakecall.ui.AppScreen
import com.siddumoni.fakecall.ui.CallAccess
import com.siddumoni.fakecall.ui.CallSettingsActions

class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeSettingsViewModel by viewModels()
    private val callerPresetsViewModel: CallerPresetsViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val scheduledCallsViewModel: ScheduledCallsViewModel by viewModels()
    private val callSettingsViewModel: CallSettingsViewModel by viewModels()
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
    private var callAccess by mutableStateOf(CallAccess())

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
        if (savedInstanceState == null) scheduledCallsViewModel.endOrphanedCalls()
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
            val callSettings by callSettingsViewModel.settings.collectAsState()
            val callSettingsSaveFailed by callSettingsViewModel.saveFailed.collectAsState()

            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted && currentScreen == AppScreen.ONBOARDING) {
                    currentScreen = AppScreen.HOME
                }
            }

            BackHandler(
                enabled = currentScreen != AppScreen.HOME && currentScreen != AppScreen.ONBOARDING,
            ) {
                currentScreen = AppScreen.HOME
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
                onBack = { currentScreen = AppScreen.HOME },
                callerPresets = callerPresets,
                callerPresetSaveError = callerPresetSaveError,
                onSaveCallerPreset = callerPresetsViewModel::savePreset,
                onDeleteCallerPreset = callerPresetsViewModel::deletePreset,
                onPreviewCaller = ::previewCall,
                callSettings = callSettings,
                callAccess = callAccess,
                callSettingsSaveFailed = callSettingsSaveFailed,
                callSettingsActions =
                    CallSettingsActions(
                        onStyleChanged = callSettingsViewModel::setStyle,
                        onFullScreenChanged = callSettingsViewModel::setFullScreen,
                        onTriggerCallerChanged = callSettingsViewModel::setTriggerCaller,
                        onTriggerDelayChanged = callSettingsViewModel::setTriggerDelaySeconds,
                        onOpenFullScreenSettings = ::openFullScreenSettings,
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                    ),
                onOpenCallSettings = { currentScreen = AppScreen.CALL_SETTINGS },
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
        callAccess =
            CallAccess(
                notifications = notificationsAllowed,
                fullScreen = ScheduledCallNotifications.canUseFullScreen(this),
                volumeShortcut = VolumeTriggerService.isEnabled(this),
            )
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

    private fun previewCall(callerId: String) {
        startActivity(CallActivity.previewIntent(this, callerId))
    }

    private fun openFullScreenSettings() {
        openSettings(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
    }

    private fun openAccessibilitySettings() {
        openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /** Some devices omit a specific settings page; the app's own settings page always exists. */
    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CURRENT_SCREEN, currentScreen.name)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val STATE_CURRENT_SCREEN = "current_screen"
    }
}
