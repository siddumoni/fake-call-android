#!/usr/bin/env python3
"""Install the Android SDK platform and a requested emulator image.

Compilation uses Android 17 API 37.1 because the stable Compose toolchain needs
that minor platform. Emulator validation can target Android 15 (API 35), 16
(API 36), or 17 (API 37) independently of the compile SDK.
"""

from __future__ import annotations

import argparse
import os
import re
import shutil
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


def emulator_image_candidates(
    available: set[str],
    requested_api_level: str | None = None,
) -> list[tuple[str, str]]:
    """Return compatible Google APIs images in descending preference.

    A requested runtime API is strict: do not silently select a different
    Android version. Without a request, prefer API 37 preview images and fall
    back to stable API 36 or 35 if the preview SDK archive is unavailable.
    """
    if requested_api_level == "37":
        versions = ["37.1", "37.0"]
    elif requested_api_level in ("35", "36"):
        versions = [requested_api_level]
    elif requested_api_level is None:
        versions = ["37.1", "37.0", "36", "35"]
    else:
        raise ValueError(f"Unsupported emulator API level: {requested_api_level}")

    candidates: list[tuple[str, str]] = []
    for api_version in versions:
        variants = (
            ("google_apis", "google_apis_ps16k")
            if api_version.startswith("37.")
            else ("google_apis", "google_apis_ps16k")
        )
        runtime_api = "37" if api_version.startswith("37.") else api_version
        for variant in variants:
            package = f"system-images;android-{api_version};{variant};x86_64"
            if package in available:
                candidates.append((package, runtime_api))
    return candidates


def install_emulator_image(
    candidates: list[tuple[str, str]],
    *,
    sdk_root: Path,
) -> tuple[str, str]:
    """Try each eligible image; a broken SDK archive must not abort fallback."""
    failures: list[str] = []
    for package, runtime_api in candidates:
        channel = 3 if runtime_api == "37" else 0
        print(f"Attempting Android emulator image: {package}")
        try:
            install([package], channel=channel)
            return package, runtime_api
        except subprocess.CalledProcessError as error:
            failures.append(f"{package} (sdkmanager exit {error.returncode})")
            package_path = sdk_root.joinpath(*package.split(";"))
            shutil.rmtree(package_path, ignore_errors=True)
            print(
                f"Unable to install {package}; trying the next available image.",
                file=sys.stderr,
            )

    raise RuntimeError(
        "Unable to install any supported emulator image. Tried: "
        + ", ".join(failures or ["no compatible images found"])
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--kind", choices=("compile", "emulator"), required=True)
    parser.add_argument(
        "--api-level",
        choices=("35", "36", "37"),
        help="Strict emulator runtime API; compile SDK remains Android 17.",
    )
    args = parser.parse_args()

    available = package_ids()
    platform = next(
        (
            item
            for item in (
                "platforms;android-37.1",
                "platforms;android-37.0",
                "platforms;android-37",
            )
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
        if args.api_level:
            parser.error("--api-level is only valid with --kind=emulator")
        print(f"Installed Android 17 platform: {platform}")
        print(f"Installed Android Build Tools: {selected_build_tools}")
        return 0

    sdk_root = Path(
        os.environ.get("ANDROID_SDK_ROOT")
        or os.environ.get("ANDROID_HOME", "")
    )
    try:
        image, emulator_api = install_emulator_image(
            emulator_image_candidates(available, args.api_level),
            sdk_root=sdk_root,
        )
    except (RuntimeError, ValueError) as error:
        print(str(error), file=sys.stderr)
        return 2

    image_api = image.split(";")[1].removeprefix("android-")
    write_github_env("ANDROID_EMULATOR_IMAGE", image)
    write_github_env("ANDROID_EMULATOR_API_LEVEL", emulator_api)
    write_github_env("ANDROID_EMULATOR_SYSTEM_IMAGE_API_LEVEL", image_api)
    write_github_env("ANDROID_EMULATOR_TARGET", image.split(";")[2])
    write_github_env("ANDROID_EMULATOR_ARCH", image.split(";")[3])
    print(
        f"Selected emulator image: {image} "
        f"(runtime API {emulator_api}, system image API {image_api})"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
