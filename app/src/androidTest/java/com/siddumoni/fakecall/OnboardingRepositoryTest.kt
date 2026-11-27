package com.siddumoni.fakecall

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.siddumoni.fakecall.data.preferences.OnboardingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingRepositoryTest {
    @Test
    fun onboardingAcknowledgementIsPersistedLocally() =
        runBlocking {
            val repository =
                OnboardingRepository(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                )

            try {
                repository.setCompleted(false)
                assertFalse(repository.completed.first())

                repository.setCompleted(true)
                assertTrue(repository.completed.first())
            } finally {
                repository.setCompleted(false)
            }
        }
}
