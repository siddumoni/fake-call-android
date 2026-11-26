#!/usr/bin/env python3
"""Install Android 17 SDK packages from the preview channel and select an emulator.

The latest stable Compose release requires the Android 17 API 37.1 minor
platform for compilation. The app still targets Android API 37 at runtime.
Discover package IDs from sdkmanager rather than assuming platforms;android-37.
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path


def run(command: list[str], *, capture: bool = False) -> str:
    result = subprocess.run(
        command,
        check=True,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.STDOUT if capture else None,
    )
    return result.stdout or ""


def package_ids() -> set[str]:
    output = run(["sdkmanager", "--list", "--channel=3"], capture=True)
    packages: set[str] = set()
    for line in output.splitlines():
        candidate = line.split("|", 1)[0].strip()
        if re.match(r"^(platforms|build-tools|system-images);", candidate):
            packages.add(candidate)
    return packages


def version_key(package: str) -> tuple[int, ...]:
    version = package.split(";", 1)[1]
    return tuple(int(piece) for piece in version.split("."))


def install(packages: list[str], channel: int) -> None:
    run(["sdkmanager", f"--channel={channel}", *packages])


def write_github_env(name: str, value: str) -> None:
    github_env = os.environ.get("GITHUB_ENV")
    if github_env:
        with Path(github_env).open("a", encoding="utf-8") as stream:
            stream.write(f"{name}={value}\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--kind", choices=("compile", "emulator"), required=True)
    args = parser.parse_args()

    available = package_ids()
    platform = next(
        (
            item
            for item in ("platforms;android-37.1", "platforms;android-37.0", "platforms;android-37")
            if item in available
        ),
        None,
    )
    build_tools = sorted(
        (
            item
            for item in available
            if re.fullmatch(r"build-tools;37\.\d+\.\d+", item)
        ),
        key=version_key,
    )
    if not platform:
        print(
            "Android 17 API 37.1 compile platform is not available in SDK Manager "
            "even with preview channel 3 enabled.",
            file=sys.stderr,
        )
        return 2
    if not build_tools:
        print(
            "Android 17 Build Tools 37.x are not available in SDK Manager.",
            file=sys.stderr,
        )
        return 2

    selected_build_tools = build_tools[-1]
    compile_packages = [platform, selected_build_tools, "platform-tools"]
    if args.kind == "emulator":
        compile_packages.append("emulator")
    install(compile_packages, channel=3)
    write_github_env("ANDROID_COMPILE_PLATFORM", platform)
    write_github_env("ANDROID_BUILD_TOOLS", selected_build_tools)

    if args.kind == "compile":
        print(f"Installed Android 17 platform: {platform}")
        print(f"Installed Android Build Tools: {selected_build_tools}")
        return 0

    image_candidates = [
        item
        for item in available
        if (
            item.startswith("system-images;android-37.1;")
            or item.startswith("system-images;android-37.0;")
        )
        and item.endswith(";x86_64")
    ]
    image = None
    for api_level in ("37.1", "37.0"):
        for preferred in (
            "google_apis_ps16k",
            "google_apis_playstore_ps16k",
            "google_apis",
            "google_apis_playstore",
        ):
            image = next(
                (
                    item
                    for item in image_candidates
                    if item.split(";")[1] == f"android-{api_level}"
                    and item.split(";")[2] == preferred
                ),
                None,
            )
            if image:
                break
        if image:
            break
    emulator_api = "37"
    if image is None:
        # API 37 system images may be unavailable for the x86_64 hosted runner.
        # Keep compileSdk/targetSdk at 37, but test runtime behaviour on the
        # highest stable x86_64 Google APIs image available (API 36).
        fallback = "system-images;android-36;google_apis;x86_64"
        if fallback not in available:
            print(
                "No Android 17 x86_64 image and no API 36 Google APIs fallback "
                "are available.",
                file=sys.stderr,
            )
            return 2
        image = fallback
        emulator_api = "36"

    channel = 3 if emulator_api == "37" else 0
    install([image], channel=channel)
    write_github_env("ANDROID_EMULATOR_IMAGE", image)
    write_github_env("ANDROID_EMULATOR_API_LEVEL", emulator_api)
    write_github_env("ANDROID_EMULATOR_SYSTEM_IMAGE_API_LEVEL", image.split(";")[1].removeprefix("android-"))
    write_github_env("ANDROID_EMULATOR_TARGET", image.split(";")[2])
    write_github_env("ANDROID_EMULATOR_ARCH", image.split(";")[3])
    print(f"Selected emulator system image: {image} (API {emulator_api})")
    if emulator_api != "37":
        print(
            "API 37 x86_64 system image unavailable; emulator test uses API 36. "
            "Compilation and targetSdk remain API 37."
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
