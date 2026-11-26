package com.siddumoni.fakecall

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.data.preferences.ThemePreferencesRepository
import com.siddumoni.fakecall.settings.ThemeAppearance
import com.siddumoni.fakecall.settings.ThemeSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemePreferencesPersistenceTest {
    @Test
    fun appearanceAndIndependentOptionsPersistInDataStore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = ThemePreferencesRepository(context)
        val previousSettings = repository.settings.first()
        val expected = ThemeSettings(
            appearance = ThemeAppearance.DARK,
            pureBlack = true,
            dynamicColor = false,
        )

        try {
            repository.save(expected)
            assertEquals(expected, repository.settings.first())
        } finally {
            repository.save(previousSettings)
        }
    }
}
