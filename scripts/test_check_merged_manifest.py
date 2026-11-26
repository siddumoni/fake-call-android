import json
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from io import StringIO
from pathlib import Path

from check_merged_manifest import main, read_permission_sets


class ManifestPermissionPolicyTests(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        self.permission = (
            "com.siddumoni.fakecall.debug."
            "DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        )
        self.manifest = self.root / "AndroidManifest.xml"
        self.allowlist = self.root / "manifest-permissions.json"

    def tearDown(self):
        self.temp_dir.cleanup()

    def write_manifest(self, protection_level: str = "signature", extra: str = ""):
        self.manifest.write_text(
            f"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <permission
        android:name="{self.permission}"
        android:protectionLevel="{protection_level}" />
    <uses-permission android:name="{self.permission}" />
    {extra}
</manifest>
""",
            encoding="utf-8",
        )

    def write_policy(self):
        policy = {
            "variants": {
                "debug": {
                    "permissions": [self.permission],
                    "defined_permissions": {self.permission: "signature"},
                }
            },
            "rationale": {self.permission: "AndroidX Core signature permission."},
        }
        self.allowlist.write_text(json.dumps(policy), encoding="utf-8")

    def test_reads_requested_permission_and_protection_level(self):
        self.write_manifest()
        requested, defined = read_permission_sets(self.manifest)

        self.assertEqual({self.permission}, requested)
        self.assertEqual({self.permission: "signature"}, defined)

    def test_accepts_only_the_exact_variant_permission_and_signature_level(self):
        self.write_manifest()
        self.write_policy()

        with redirect_stdout(StringIO()):
            result = main(
                [
                    "--manifest",
                    str(self.manifest),
                    "--variant",
                    "debug",
                    "--allowlist",
                    str(self.allowlist),
                ]
            )

        self.assertEqual(0, result)

    def test_rejects_an_unlisted_permission(self):
        self.write_manifest(
            extra='<uses-permission android:name="android.permission.INTERNET" />'
        )
        self.write_policy()

        with redirect_stdout(StringIO()), redirect_stderr(StringIO()):
            result = main(
                [
                    "--manifest",
                    str(self.manifest),
                    "--variant",
                    "debug",
                    "--allowlist",
                    str(self.allowlist),
                ]
            )

        self.assertEqual(1, result)

    def test_rejects_a_weakened_permission_protection_level(self):
        self.write_manifest(protection_level="normal")
        self.write_policy()

        with redirect_stdout(StringIO()), redirect_stderr(StringIO()):
            result = main(
                [
                    "--manifest",
                    str(self.manifest),
                    "--variant",
                    "debug",
                    "--allowlist",
                    str(self.allowlist),
                ]
            )

        self.assertEqual(1, result)

    def test_rejects_allowlisted_permission_without_rationale(self):
        self.write_manifest()
        self.write_policy()
        policy = json.loads(self.allowlist.read_text(encoding="utf-8"))
        policy["rationale"] = {}
        self.allowlist.write_text(json.dumps(policy), encoding="utf-8")

        with redirect_stdout(StringIO()), redirect_stderr(StringIO()):
            result = main(
                [
                    "--manifest",
                    str(self.manifest),
                    "--variant",
                    "debug",
                    "--allowlist",
                    str(self.allowlist),
                ]
            )

        self.assertEqual(1, result)


if __name__ == "__main__":
    unittest.main()
