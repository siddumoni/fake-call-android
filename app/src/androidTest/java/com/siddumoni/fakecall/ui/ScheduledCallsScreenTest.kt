package com.siddumoni.fakecall.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallDeliveryMode
import com.siddumoni.fakecall.scheduling.ScheduledCallOperationState
import com.siddumoni.fakecall.settings.ThemeSettings
import com.siddumoni.fakecall.ui.theme.AlibiCallTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ScheduledCallsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scheduleEditorUsesApproximateByDefaultAndCanSelectExactExplicitly() {
        val caller = CallerPreset(UUID.randomUUID().toString(), "Fictional caller", "")
        var saved: ScheduledCall? = null

        composeRule.setContent {
            AlibiCallTheme(settings = ThemeSettings.Default) {
                ScheduledCallsScreen(
                    calls = emptyList(),
                    callerPresets = listOf(caller),
                    operationState = ScheduledCallOperationState.IDLE,
                    notificationsAllowed = true,
                    exactAlarmAllowed = true,
                    onSave = { saved = it },
                    onCancel = {},
                    onDelete = {},
                    onRequestNotifications = {},
                    onRequestExactAlarms = {},
                    onClearOperationState = {},
                )
            }
        }

        composeRule.onNodeWithTag("schedule-add-button").performClick()
        composeRule.onNodeWithText("Approximate").assertIsDisplayed()
        composeRule.onNodeWithText("Exact").performClick()
        composeRule.onNodeWithTag("schedule-save-button").performClick()

        composeRule.runOnIdle {
            assertEquals(ScheduledCallDeliveryMode.EXACT, saved?.deliveryMode)
            assertEquals(caller.id, saved?.callerPresetId)
        }
    }

    @Test
    fun editRetainsScheduleIdentityAndDeleteFencesThePendingRecord() {
        val caller = CallerPreset(UUID.randomUUID().toString(), "Fictional caller", "")
        val schedule =
            ScheduledCall(
                id = UUID.randomUUID().toString(),
                callerPresetId = caller.id,
                triggerAtEpochMillis = System.currentTimeMillis() + 60_000,
                deliveryMode = ScheduledCallDeliveryMode.APPROXIMATE,
            )
        var edited: ScheduledCall? = null
        var cancelled: String? = null
        var deleted: String? = null
        val operationState = mutableStateOf(ScheduledCallOperationState.IDLE)

        composeRule.setContent {
            AlibiCallTheme(settings = ThemeSettings.Default) {
                ScheduledCallsScreen(
                    calls = listOf(schedule),
                    callerPresets = listOf(caller),
                    operationState = operationState.value,
                    notificationsAllowed = true,
                    exactAlarmAllowed = true,
                    onSave = {
                        edited = it
                        operationState.value = ScheduledCallOperationState.SAVED
                    },
                    onCancel = { cancelled = it },
                    onDelete = { deleted = it },
                    onRequestNotifications = {},
                    onRequestExactAlarms = {},
                    onClearOperationState = { operationState.value = ScheduledCallOperationState.IDLE },
                )
            }
        }

        composeRule.onNodeWithTag("schedule-edit-${schedule.id}").performClick()
        composeRule.onNodeWithText("Exact").performClick()
        composeRule.onNodeWithTag("schedule-save-button").performClick()
        composeRule.runOnIdle {
            assertEquals(schedule.id, edited?.id)
            assertEquals(ScheduledCallDeliveryMode.EXACT, edited?.deliveryMode)
        }

        composeRule.onNodeWithTag("schedule-cancel-${schedule.id}").performClick()
        composeRule.runOnIdle { assertEquals(schedule.id, cancelled) }

        composeRule.onNodeWithTag("schedule-delete-${schedule.id}").performClick()
        composeRule.onNodeWithTag("schedule-delete-confirm").performClick()
        composeRule.runOnIdle { assertEquals(schedule.id, deleted) }
    }

    @Test
    fun exactModeExplainsSpecialAccessAndNotificationRequirement() {
        val caller = CallerPreset(UUID.randomUUID().toString(), "Fictional caller", "")

        composeRule.setContent {
            AlibiCallTheme(settings = ThemeSettings.Default) {
                ScheduledCallsScreen(
                    calls = emptyList(),
                    callerPresets = listOf(caller),
                    operationState = ScheduledCallOperationState.IDLE,
                    notificationsAllowed = false,
                    exactAlarmAllowed = false,
                    onSave = {},
                    onCancel = {},
                    onDelete = {},
                    onRequestNotifications = {},
                    onRequestExactAlarms = {},
                    onClearOperationState = {},
                )
            }
        }

        composeRule.onNodeWithTag("schedule-add-button").performClick()
        composeRule.onNodeWithText("Exact").performClick()
        composeRule.onNodeWithText("Open Alarms & reminders settings").assertExists()
        composeRule.onNodeWithText("Enable notifications").assertExists()
    }

    @Test
    fun notificationSettingsButtonDelegatesToSystemSettings() {
        var opened = false
        composeRule.setContent {
            AlibiCallTheme(settings = ThemeSettings.Default) {
                ScheduledCallsScreen(
                    calls = emptyList(),
                    callerPresets = emptyList(),
                    operationState = ScheduledCallOperationState.IDLE,
                    notificationsAllowed = true,
                    exactAlarmAllowed = true,
                    onSave = {},
                    onCancel = {},
                    onDelete = {},
                    onRequestNotifications = {},
                    onRequestExactAlarms = {},
                    onOpenNotificationSettings = { opened = true },
                    onClearOperationState = {},
                )
            }
        }
        composeRule.onNodeWithTag("schedule-notification-settings").performClick()
        composeRule.runOnIdle { assertEquals(true, opened) }
    }
}
