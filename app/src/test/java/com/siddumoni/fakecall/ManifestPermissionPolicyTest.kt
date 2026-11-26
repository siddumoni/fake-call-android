package com.siddumoni.fakecall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestPermissionPolicyTest {
    @Test
    fun foundationManifestDeclaresNoAndroidPermissions() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val permissions = Regex("""<uses-permission(?:-sdk-23|-sdk-m)?\b""")
            .findAll(manifest)
            .map { it.value }
            .toList()
        assertEquals(emptyList<String>(), permissions)
    }

    @Test
    fun manifestDisablesBackupAndCleartextTraffic() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("""android:allowBackup="false""""))
        assertTrue(manifest.contains("""android:usesCleartextTraffic="false""""))
        assertFalse(manifest.contains("""android:name="android.permission.INTERNET""""))
    }
}
