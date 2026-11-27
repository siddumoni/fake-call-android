import struct
import tempfile
import unittest
from pathlib import Path

from pull_validate_theme_screenshots import (
    MIN_BYTES,
    PNG_SIGNATURE,
    parse_app_packages,
    png_dimensions,
    validate_png,
    screenshot_read_command,
)


def fake_png(width: int, height: int, payload_size: int = MIN_BYTES) -> bytes:
    header = PNG_SIGNATURE + struct.pack(">I", 13) + b"IHDR"
    header += struct.pack(">II", width, height) + bytes([8, 2, 0, 0, 0])
    header += bytes(max(0, payload_size - len(header)))
    return header


class ThemeScreenshotValidationTests(unittest.TestCase):
    def test_reads_screenshot_from_app_private_storage_with_run_as(self):
        self.assertEqual(
            ["adb", "exec-out", "run-as", "com.siddumoni.fakecall.debug", "cat", "files/screenshots/home-light.png"],
            screenshot_read_command("com.siddumoni.fakecall.debug", "home-light.png"),
        )

    def test_detects_debug_and_release_packages_and_excludes_test_package(self):
        packages = parse_app_packages(
            "\n".join(
                (
                    "package:com.siddumoni.fakecall",
                    "package:com.siddumoni.fakecall.debug",
                    "package:com.siddumoni.fakecall.debug.test",
                    "package:com.other.app",
                ),
            ),
        )
        self.assertEqual(
            ["com.siddumoni.fakecall", "com.siddumoni.fakecall.debug"],
            packages,
        )

    def test_reads_dimensions_from_png_header(self):
        self.assertEqual((1080, 1920), png_dimensions(fake_png(1080, 1920)))

    def test_rejects_non_png_content(self):
        with self.assertRaisesRegex(ValueError, "valid PNG"):
            png_dimensions(b"run-as: unknown package")

    def test_rejects_too_small_image_dimensions(self):
        with self.assertRaisesRegex(ValueError, "too small"):
            png_dimensions(fake_png(1, 1))

    def test_validates_file_size_and_dimensions(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "home-light.png"
            path.write_bytes(fake_png(1080, 1920))
            self.assertEqual((1080, 1920), validate_png(path))

    def test_rejects_tiny_file_even_if_png_signature_present(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "home-light.png"
            path.write_bytes(fake_png(1080, 1920, payload_size=40))
            with self.assertRaisesRegex(ValueError, "unexpectedly small"):
                validate_png(path)


if __name__ == "__main__":
    unittest.main()
