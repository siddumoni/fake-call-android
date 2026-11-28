package com.siddumoni.fakecall.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.siddumoni.fakecall.data.preferences.recoverCorruptPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CorruptionRecoveryTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun corruptPreferencesFileIsReplacedAndCanBeWrittenAgain() =
        runBlocking {
            val file = folder.newFile("corrupt.preferences_pb")
            // A length-delimited field that claims more bytes than the file holds.
            file.writeBytes(byteArrayOf(0x0A, 0x7F))
            val scope = CoroutineScope(Dispatchers.IO + Job())
            try {
                val store =
                    PreferenceDataStoreFactory.create(
                        corruptionHandler = recoverCorruptPreferences(),
                        scope = scope,
                    ) { file }
                val key = stringPreferencesKey("name")

                assertTrue(
                    store.data
                        .first()
                        .asMap()
                        .isEmpty(),
                )
                store.edit { it[key] = "saved" }

                assertEquals("saved", store.data.first()[key])
            } finally {
                scope.cancel()
            }
        }
}
