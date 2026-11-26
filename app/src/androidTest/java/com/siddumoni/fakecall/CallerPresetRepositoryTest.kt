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
            val repository = CallerPresetRepository(
                InstrumentationRegistry.getInstrumentation().targetContext,
            )
            val idPrefix = "repo-test-"
            val initial = repository.presets.first().filterNot { it.id.startsWith(idPrefix) }
            val firstId = idPrefix + "first"
            val secondId = idPrefix + "second"

            try {
                val first = repository.save(firstId, "Zeta", "555-0102")
                val second = repository.save(secondId, "Alpha", "")
                assertEquals(listOf("Alpha", "Zeta"), repository.presets.first().map { it.displayName }.filter { it == "Alpha" || it == "Zeta" })

                repository.save(first.id, "Updated Zeta", "+1 555 0102")
                val updated = repository.presets.first().first { it.id == firstId }
                assertEquals("Updated Zeta", updated.displayName)
                assertEquals("+1 555 0102", updated.numberLabel)

                repository.delete(second.id)
                assertTrue(repository.presets.first().none { it.id == secondId })
            } finally {
                repository.delete(firstId)
                repository.delete(secondId)
                assertTrue(repository.presets.first().none { it.id.startsWith(idPrefix) })
                assertTrue(initial.all { original -> repository.presets.first().any { it.id == original.id } })
            }
        }
}
