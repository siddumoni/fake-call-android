#!/usr/bin/env python3
"""Pull the app's Android emulator screenshots from public MediaStore storage and validate."""

from __future__ import annotations

import argparse
import struct
import subprocess
import sys
from pathlib import Path

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
MIN_WIDTH = 320
MIN_HEIGHT = 480
MIN_BYTES = 512
SCREENSHOT_NAMES = (
    "home-light.png",
    "appearance-light.png",
    "home-dark.png",
    "appearance-dark.png",
    "home-amoled.png",
    "appearance-amoled.png",
    "call-google-incoming.png",
    "call-google-active.png",
    "call-oneplus-incoming.png",
    "call-oneplus-active.png",
)


def screenshot_pull_command(name: str, destination: Path) -> list[str]:
    return ["adb", "pull", f"/sdcard/Pictures/AlibiCall/{name}", str(destination)]


def pull_screenshot(name: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(screenshot_pull_command(name, destination), check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)


def png_dimensions(data: bytes) -> tuple[int, int]:
    if len(data) < 24 or data[:8] != PNG_SIGNATURE or data[12:16] != b"IHDR":
        raise ValueError("file does not have a valid PNG signature and IHDR")
    width, height = struct.unpack(">II", data[16:24])
    if width < MIN_WIDTH or height < MIN_HEIGHT:
        raise ValueError(f"image is too small ({width}x{height})")
    return width, height


def validate_png(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if len(data) < MIN_BYTES:
        raise ValueError(f"file is unexpectedly small ({len(data)} bytes)")
    return png_dimensions(data)


def pull_and_validate(output: Path) -> list[dict[str, object]]:
    output.mkdir(parents=True, exist_ok=True)
    report: list[dict[str, object]] = []
    for name in SCREENSHOT_NAMES:
        destination = output / name
        pull_screenshot(name, destination)
        width, height = validate_png(destination)
        report.append({"file": name, "width": width, "height": height, "bytes": destination.stat().st_size})
    return report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path("screenshots"))
    args = parser.parse_args(argv)
    try:
        report = pull_and_validate(args.output)
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print(f"Screenshot export failed: {error}", file=sys.stderr)
        return 1
    for item in report:
        print(f"{item['file']}: {item['width']}x{item['height']} ({item['bytes']} bytes)")
    print(f"Validated all {len(report)} screenshots.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
