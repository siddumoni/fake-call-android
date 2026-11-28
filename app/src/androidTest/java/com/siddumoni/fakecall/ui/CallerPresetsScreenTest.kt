package com.siddumoni.fakecall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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

        scrollTo(hasTestTag("caller-preset-add-button")).performClick()
        composeRule.onNodeWithTag("caller-preset-name").performTextInput("Fictional Caller")
        composeRule.onNodeWithTag("caller-preset-number").performTextInput("+1 (555) 010-1234")
        composeRule.onNodeWithText("Save").performClick()

        scrollTo(hasText("Fictional Caller")).assertExists()
        scrollTo(hasText("Preview")).performClick()
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
        scrollTo(hasText("No caller profiles yet. Add one to preview your first simulated call.")).assertExists()
    }

    @Test
    fun everyActionStaysReachableInShortWindowAtDoubleFontScale() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Box(Modifier.size(width = 320.dp, height = 240.dp)) {
                    CallerPresetsScreen(
                        presets = listOf(CallerPreset(UUID.randomUUID().toString(), "Fictional Caller", "555-0100")),
                        saveError = null,
                        onSave = { _, _, _ -> true },
                        onDelete = {},
                        onPreview = {},
                    )
                }
            }
        }
        composeRule
            .onNodeWithText("Caller profiles")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        listOf("Preview", "Edit", "Delete").forEach { label ->
            scrollTo(hasText(label)).assertIsDisplayed()
        }
    }

    private fun scrollTo(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        composeRule.onNodeWithTag("caller-presets-screen").performScrollToNode(matcher)
        return composeRule.onNode(matcher).performScrollTo()
    }
}
