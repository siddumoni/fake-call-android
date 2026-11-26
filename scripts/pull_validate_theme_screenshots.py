#!/usr/bin/env python3
"""Pull the six Android emulator theme screenshots and reject invalid artifacts."""

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
)


def run_adb(*args: str) -> str:
    result = subprocess.run(
        ["adb", *args],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    return result.stdout


def installed_app_packages() -> list[str]:
    output = run_adb("shell", "pm", "list", "packages", "-3")
    return sorted(
        {
            line.removeprefix("package:").strip()
            for line in output.splitlines()
            if line.startswith("package:") and "com.siddumoni.fakecall" in line
        }
    )


def find_screenshot_package() -> str:
    candidates = [
        package
        for package in installed_app_packages()
        if not package.endswith(".test")
        and ".test." not in package
    ]
    for package in candidates:
        remote_dir = f"/sdcard/Android/data/{package}/files/screenshots"
        try:
            names = set(run_adb("shell", "ls", remote_dir).split())
        except subprocess.CalledProcessError:
            continue
        if set(SCREENSHOT_NAMES).issubset(names):
            return package

    raise RuntimeError(
        "No installed AlibiCall app package contains all six screenshots. "
        f"Candidate packages: {candidates}"
    )


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
    package = find_screenshot_package()
    remote_dir = f"/sdcard/Android/data/{package}/files/screenshots"
    output.mkdir(parents=True, exist_ok=True)
    report: list[dict[str, object]] = []

    for name in SCREENSHOT_NAMES:
        destination = output / name
        run_adb("pull", f"{remote_dir}/{name}", str(destination))
        width, height = validate_png(destination)
        report.append(
            {
                "file": name,
                "width": width,
                "height": height,
                "bytes": destination.stat().st_size,
                "package": package,
            }
        )
    return report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path("screenshots"))
    args = parser.parse_args(argv)

    try:
        report = pull_and_validate(args.output)
    except (OSError, ValueError, RuntimeError, subprocess.CalledProcessError) as error:
        print(f"Theme screenshot export failed: {error}", file=sys.stderr)
        return 1

    for item in report:
        print(
            f"{item['file']}: {item['width']}x{item['height']} "
            f"({item['bytes']} bytes) from {item['package']}"
        )
    print(f"Validated all {len(report)} theme screenshots.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
