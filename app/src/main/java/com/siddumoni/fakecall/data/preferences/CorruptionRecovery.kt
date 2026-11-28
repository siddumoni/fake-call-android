package com.siddumoni.fakecall.data.preferences

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences

/**
 * An unreadable preferences file is replaced with an empty one. Without this every later write
 * would fail until the user cleared the app's storage.
 */
internal fun recoverCorruptPreferences(): ReplaceFileCorruptionHandler<Preferences> = ReplaceFileCorruptionHandler { emptyPreferences() }
