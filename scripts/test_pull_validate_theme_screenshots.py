import struct
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from pull_validate_theme_screenshots import (
    MIN_BYTES,
    PNG_SIGNATURE,
    png_dimensions,
    screenshot_pull_command,
    pull_screenshot,
    validate_png,
)


def fake_png(width: int, height: int, payload_size: int = MIN_BYTES) -> bytes:
    header = PNG_SIGNATURE + struct.pack(">I", 13) + b"IHDR"
    header += struct.pack(">II", width, height) + bytes([8, 2, 0, 0, 0])
    header += bytes(max(0, payload_size - len(header)))
    return header


class ThemeScreenshotValidationTests(unittest.TestCase):
    def test_pulls_from_public_media_store_path(self):
        self.assertEqual(
            ["adb", "pull", "/sdcard/Pictures/AlibiCall/home-light.png", "/tmp/home-light.png"],
            screenshot_pull_command("home-light.png", Path("/tmp/home-light.png")),
        )

    def test_reads_dimensions_from_png_header(self):
        self.assertEqual((1080, 1920), png_dimensions(fake_png(1080, 1920)))

    def test_rejects_non_png_content(self):
        with self.assertRaisesRegex(ValueError, "valid PNG"):
            png_dimensions(b"not a png")

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
