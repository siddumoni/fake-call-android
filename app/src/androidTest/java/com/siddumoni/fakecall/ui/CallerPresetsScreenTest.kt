package com.siddumoni.fakecall.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.siddumoni.fakecall.callers.CallerPreset
import com.siddumoni.fakecall.callers.CallerPresetValidator
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class CallerPresetsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun createCallerAndLaunchLocalPreview() {
        val presets = mutableStateOf(emptyList<CallerPreset>())
        val previewed = mutableStateOf<CallerPreset?>(null)

        composeRule.setContent {
            CallerPresetsScreen(
                presets = presets.value,
                saveError = null,
                onSave = { id, name, number ->
                    if (CallerPresetValidator.validate(name, number) != null) {
                        false
                    } else {
                        val item = CallerPreset(id ?: UUID.randomUUID().toString(), name.trim(), number.trim())
                        presets.value = presets.value.filterNot { it.id == item.id } + item
                        true
                    }
                },
                onDelete = { id -> presets.value = presets.value.filterNot { it.id == id } },
                onPreview = { previewed.value = it },
            )
        }

        composeRule.onNodeWithTag("caller-preset-add-button").performClick()
        composeRule.onNodeWithTag("caller-preset-name").performTextInput("Fictional Caller")
        composeRule.onNodeWithTag("caller-preset-number").performTextInput("+1 (555) 010-1234")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.onNodeWithText("Fictional Caller").assertExists()
        composeRule.onNodeWithText("Preview").performClick()
        composeRule.runOnIdle {
            assertEquals("Fictional Caller", previewed.value?.displayName)
            assertEquals("+1 (555) 010-1234", previewed.value?.numberLabel)
        }
    }

    @Test
    fun emptyStateExplainsHowToBegin() {
        composeRule.setContent {
            CallerPresetsScreen(
                presets = emptyList(),
                saveError = null,
                onSave = { _, _, _ -> true },
                onDelete = {},
                onPreview = {},
            )
        }
        composeRule.onNodeWithText("No caller profiles yet. Add one to preview your first simulated call.").assertExists()
    }
}
