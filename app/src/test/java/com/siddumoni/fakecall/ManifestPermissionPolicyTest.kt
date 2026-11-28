package com.siddumoni.fakecall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ManifestPermissionPolicyTest {
    @Test
    fun sourceManifestDeclaresOnlyScheduleDeliveryPermissions() {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
        val document =
            factory
                .newDocumentBuilder()
                .parse(File("src/main/AndroidManifest.xml"))
        val root = document.documentElement
        val permissionTags =
            setOf(
                "uses-permission",
                "uses-permission-sdk-23",
                "uses-permission-sdk-m",
                "permission",
            )
        val activePermissions =
            buildList {
                val nodes = root.childNodes
                for (index in 0 until nodes.length) {
                    val element = nodes.item(index) as? Element ?: continue
                    val tag = element.localName ?: element.tagName.substringAfter(':')
                    if (tag !in permissionTags) continue
                    if (
                        element.getAttributeNS(
                            "http://schemas.android.com/tools",
                            "node",
                        ) == "remove"
                    ) {
                        continue
                    }
                    add(
                        element.getAttributeNS(
                            "http://schemas.android.com/apk/res/android",
                            "name",
                        ),
                    )
                }
            }

        assertEquals(
            setOf(
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.SCHEDULE_EXACT_ALARM",
                "android.permission.USE_FULL_SCREEN_INTENT",
                "android.permission.VIBRATE",
                "android.permission.WAKE_LOCK",
            ),
            activePermissions.filter { it.isNotBlank() }.toSet(),
        )
    }

    @Test
    fun sourceManifestDoesNotDeclareAnAccessibilityService() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("android.permission.BIND_ACCESSIBILITY_SERVICE"))
        assertFalse(manifest.contains("android.accessibilityservice.AccessibilityService"))
    }

    @Test
    fun manifestDisablesBackupAndCleartextTraffic() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("""android:allowBackup="false""""))
        assertTrue(manifest.contains("""android:usesCleartextTraffic="false""""))
        assertFalse(manifest.contains("""android:name="android.permission.INTERNET""""))
    }
}
