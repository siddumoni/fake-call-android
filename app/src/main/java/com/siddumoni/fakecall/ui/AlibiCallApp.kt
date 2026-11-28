package com.siddumoni.fakecall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.siddumoni.fakecall.R
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.callers.CallerPresetValidationError
import com.siddumoni.fakecall.settings.CallScreenStyle
import com.siddumoni.fakecall.settings.CallSettings
import com.siddumoni.fakecall.settings.ThemeAppearance
import com.siddumoni.fakecall.settings.ThemeSettings
import com.siddumoni.fakecall.ui.theme.AlibiCallTheme

enum class AppScreen {
    ONBOARDING,
    HOME,
    THEME_SETTINGS,
    CALLER_PRESETS,
    SCHEDULES,
    CALL_SETTINGS,
}

data class CallSettingsActions(
    val onStyleChanged: (CallScreenStyle) -> Unit = {},
    val onFullScreenChanged: (Boolean) -> Unit = {},
    val onTriggerCallerChanged: (String) -> Unit = {},
    val onTriggerDelayChanged: (Int) -> Unit = {},
    val onOpenFullScreenSettings: () -> Unit = {},
    val onStartQuickCall: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlibiCallApp(
    settings: ThemeSettings,
    onAppearanceChanged: (ThemeAppearance) -> Unit,
    onPureBlackChanged: (Boolean) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    saveFailed: Boolean,
    onboardingSaveFailed: Boolean = false,
    onCompleteOnboarding: () -> Unit = {},
    screen: AppScreen,
    onOpenThemeSettings: () -> Unit,
    onOpenCallerPresets: () -> Unit = {},
    onBack: () -> Unit,
    callerPresets: List<CallerPreset> = emptyList(),
    callerPresetSaveError: CallerPresetValidationError? = null,
    onSaveCallerPreset: (String?, String, String) -> Boolean = { _, _, _ -> false },
    onDeleteCallerPreset: (String) -> Unit = {},
    onPreviewCaller: (String) -> Unit = {},
    callSettings: CallSettings = CallSettings(),
    callAccess: CallAccess = CallAccess(),
    callSettingsSaveFailed: Boolean = false,
    callSettingsActions: CallSettingsActions = CallSettingsActions(),
    onOpenCallSettings: () -> Unit = {},
    scheduledCalls: List<com.siddumoni.fakecall.scheduling.ScheduledCall> = emptyList(),
    scheduleOperationState: com.siddumoni.fakecall.scheduling.ScheduledCallOperationState =
        com.siddumoni.fakecall.scheduling.ScheduledCallOperationState.IDLE,
    notificationsAllowed: Boolean = true,
    exactAlarmAllowed: Boolean = false,
    onOpenSchedules: () -> Unit = {},
    onOpenScheduleNotificationSettings: () -> Unit = {},
    onSaveSchedule: (com.siddumoni.fakecall.scheduling.ScheduledCall) -> Unit = {},
    onCancelSchedule: (String) -> Unit = {},
    onDeleteSchedule: (String) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onRequestExactAlarmPermission: () -> Unit = {},
    onClearScheduleOperation: () -> Unit = {},
) {
    AlibiCallTheme(settings = settings) {
        Scaffold(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("app-root"),
            topBar = {
                if (screen == AppScreen.THEME_SETTINGS ||
                    screen == AppScreen.CALLER_PRESETS ||
                    screen == AppScreen.SCHEDULES ||
                    screen == AppScreen.CALL_SETTINGS
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(
                                    when (screen) {
                                        AppScreen.THEME_SETTINGS -> R.string.theme_settings_title
                                        AppScreen.CALLER_PRESETS -> R.string.caller_presets_title
                                        AppScreen.CALL_SETTINGS -> R.string.call_settings_title
                                        else -> R.string.schedules_title
                                    },
                                ),
                            )
                        },
                        navigationIcon = {
                            TextButton(onClick = onBack) {
                                Text(stringResource(R.string.back))
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            when (screen) {
                AppScreen.ONBOARDING ->
                    OnboardingScreen(
                        modifier = Modifier.padding(innerPadding),
                        saveFailed = onboardingSaveFailed,
                        onContinue = onCompleteOnboarding,
                    )
                AppScreen.HOME ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        onOpenThemeSettings = onOpenThemeSettings,
                        onOpenCallerPresets = onOpenCallerPresets,
                        onOpenSchedules = onOpenSchedules,
                        onOpenCallSettings = onOpenCallSettings,
                    )
                AppScreen.SCHEDULES ->
                    ScheduledCallsScreen(
                        modifier = Modifier.padding(innerPadding),
                        calls = scheduledCalls,
                        callerPresets = callerPresets,
                        operationState = scheduleOperationState,
                        notificationsAllowed = notificationsAllowed,
                        exactAlarmAllowed = exactAlarmAllowed,
                        onSave = onSaveSchedule,
                        onCancel = onCancelSchedule,
                        onDelete = onDeleteSchedule,
                        onRequestNotifications = onRequestNotificationPermission,
                        onRequestExactAlarms = onRequestExactAlarmPermission,
                        onOpenNotificationSettings = onOpenScheduleNotificationSettings,
                        onClearOperationState = onClearScheduleOperation,
                    )
                AppScreen.THEME_SETTINGS ->
                    ThemeSettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        settings = settings,
                        onAppearanceChanged = onAppearanceChanged,
                        onPureBlackChanged = onPureBlackChanged,
                        onDynamicColorChanged = onDynamicColorChanged,
                        saveFailed = saveFailed,
                    )
                AppScreen.CALLER_PRESETS ->
                    CallerPresetsScreen(
                        modifier = Modifier.padding(innerPadding),
                        presets = callerPresets,
                        saveError = callerPresetSaveError,
                        onSave = onSaveCallerPreset,
                        onDelete = onDeleteCallerPreset,
                        onPreview = { caller -> onPreviewCaller(caller.id) },
                    )
                AppScreen.CALL_SETTINGS ->
                    CallSettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        settings = callSettings,
                        callers = callerPresets,
                        access = callAccess,
                        saveFailed = callSettingsSaveFailed,
                        onStyleChanged = callSettingsActions.onStyleChanged,
                        onFullScreenChanged = callSettingsActions.onFullScreenChanged,
                        onTriggerCallerChanged = callSettingsActions.onTriggerCallerChanged,
                        onTriggerDelayChanged = callSettingsActions.onTriggerDelayChanged,
                        onPreview = onPreviewCaller,
                        onRequestNotifications = onRequestNotificationPermission,
                        onOpenFullScreenSettings = callSettingsActions.onOpenFullScreenSettings,
                        onStartQuickCall = callSettingsActions.onStartQuickCall,
                    )
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    modifier: Modifier = Modifier,
    saveFailed: Boolean,
    onContinue: () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .testTag("onboarding-screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.onboarding_summary),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_privacy_heading),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.onboarding_privacy_offline),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.onboarding_privacy_simulation),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.onboarding_privacy_no_telephony),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (saveFailed) {
            Text(
                text = stringResource(R.string.onboarding_save_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(
            onClick = onContinue,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("onboarding-continue-button"),
        ) {
            Text(stringResource(R.string.onboarding_continue))
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenThemeSettings: () -> Unit,
    onOpenCallerPresets: () -> Unit,
    onOpenSchedules: () -> Unit,
    onOpenCallSettings: () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .testTag("home-screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = stringResource(R.string.caller_presets_title), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(R.string.caller_presets_home_description), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onOpenCallerPresets) {
                    Text(stringResource(R.string.caller_presets_open))
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.schedules_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.schedules_home_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onOpenSchedules) {
                    Text(stringResource(R.string.schedules_open))
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.call_settings_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.call_settings_home_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onOpenCallSettings) {
                    Text(stringResource(R.string.call_settings_open))
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_theme_card_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.home_theme_card_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onOpenThemeSettings) {
                    Text(stringResource(R.string.open_theme_settings))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_simulation_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemeSettingsScreen(
    modifier: Modifier = Modifier,
    settings: ThemeSettings,
    onAppearanceChanged: (ThemeAppearance) -> Unit,
    onPureBlackChanged: (Boolean) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    saveFailed: Boolean,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("theme-settings-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.theme_appearance_heading),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        AppearanceOption(
            title = stringResource(R.string.theme_follow_system),
            selected = settings.appearance == ThemeAppearance.FOLLOW_SYSTEM,
            onClick = { onAppearanceChanged(ThemeAppearance.FOLLOW_SYSTEM) },
        )
        AppearanceOption(
            title = stringResource(R.string.theme_light),
            selected = settings.appearance == ThemeAppearance.LIGHT,
            onClick = { onAppearanceChanged(ThemeAppearance.LIGHT) },
        )
        AppearanceOption(
            title = stringResource(R.string.theme_dark),
            selected = settings.appearance == ThemeAppearance.DARK,
            onClick = { onAppearanceChanged(ThemeAppearance.DARK) },
        )
        HorizontalDivider()
        PreferenceSwitch(
            title = stringResource(R.string.theme_pure_black),
            description = stringResource(R.string.theme_pure_black_description),
            checked = settings.pureBlack,
            onCheckedChange = onPureBlackChanged,
        )
        PreferenceSwitch(
            title = stringResource(R.string.theme_dynamic_color),
            description = stringResource(R.string.theme_dynamic_color_description),
            checked = settings.dynamicColor,
            onCheckedChange = onDynamicColorChanged,
        )
        if (saveFailed) {
            Text(
                text = stringResource(R.string.theme_save_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AppearanceOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onClick,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = title,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun PreferenceSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}
