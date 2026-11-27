package com.siddumoni.fakecall.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertExists
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.scheduling.ScheduledCall
import com.siddumoni.fakecall.scheduling.ScheduledCallDeliveryMode
import com.siddumoni.fakecall.scheduling.ScheduledCallOperationState
import com.siddumoni.fakecall.ui.theme.AlibiCallTheme
import com.siddumoni.fakecall.settings.ThemeSettings
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
        composeRule.onNodeWithText("Approximate").assertExists()
        composeRule.onNodeWithText("Exact").performClick()
        composeRule.onNodeWithTag("schedule-save-button").performClick()

        composeRule.runOnIdle {
            assertEquals(ScheduledCallDeliveryMode.EXACT, saved?.deliveryMode)
            assertEquals(caller.id, saved?.callerPresetId)
        }
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
}
