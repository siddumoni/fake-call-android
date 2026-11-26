package com.siddumoni.fakecall

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.data.preferences.CallerPresetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallerPresetRepositoryTest {
    @Test
    fun callerProfilesCanBeCreatedUpdatedSortedAndDeleted() =
        runBlocking {
            val repository =
                CallerPresetRepository(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                )
            val firstId = "00000000-0000-4000-8000-000000000001"
            val secondId = "00000000-0000-4000-8000-000000000002"
            val initial = repository.presets.first().filterNot { it.id == firstId || it.id == secondId }

            try {
                val first = repository.save(firstId, "CallerTest-Zeta", "555-0102")
                val second = repository.save(secondId, "CallerTest-Alpha", "")
                assertEquals(
                    listOf("CallerTest-Alpha", "CallerTest-Zeta"),
                    repository.presets.first().map { it.displayName }.filter { it.startsWith("CallerTest-") },
                )

                repository.save(first.id, "CallerTest-Updated-Zeta", "+1 555 0102")
                val updated = repository.presets.first().first { it.id == firstId }
                assertEquals("CallerTest-Updated-Zeta", updated.displayName)
                assertEquals("+1 555 0102", updated.numberLabel)

                repository.delete(second.id)
                assertTrue(repository.presets.first().none { it.id == secondId })
            } finally {
                repository.delete(firstId)
                repository.delete(secondId)
                assertTrue(repository.presets.first().none { it.id == firstId || it.id == secondId })
                assertTrue(initial.all { original -> repository.presets.first().any { it.id == original.id } })
            }
        }
}
