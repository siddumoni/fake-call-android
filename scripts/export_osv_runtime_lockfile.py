#!/usr/bin/env python3
"""Create a Gradle lockfile view containing only release runtime dependencies.

OSV-Scanner uses this generated file to check dependencies shipped in the
release application's runtime classpath, without treating lint, test-runner,
Ktlint, or other build-tool dependencies as APK runtime dependencies.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

RUNTIME_CONFIGURATION = "releaseRuntimeClasspath"


def runtime_lockfile_content(source: str) -> str:
    selected: list[str] = []
    seen: set[str] = set()

    for line_number, raw_line in enumerate(source.splitlines(), start=1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue

        coordinate, separator, configuration_text = line.partition("=")
        if not separator:
            raise ValueError(f"Line {line_number}: missing '=' separator")

        # Gradle writes this special marker for configurations with no modules.
        if coordinate == "empty":
            continue

        parts = coordinate.split(":")
        if len(parts) != 3 or not all(parts):
            raise ValueError(
                f"Line {line_number}: invalid Gradle module coordinate "
                f"{coordinate!r}"
            )

        configurations = {
            item.strip()
            for item in configuration_text.split(",")
            if item.strip()
        }
        if RUNTIME_CONFIGURATION not in configurations:
            continue

        if coordinate in seen:
            raise ValueError(
                f"Line {line_number}: duplicate release runtime coordinate "
                f"{coordinate!r}"
            )
        seen.add(coordinate)
        selected.append(f"{coordinate}={RUNTIME_CONFIGURATION}")

    if not selected:
        raise ValueError(
            f"No dependencies were found in {RUNTIME_CONFIGURATION}; "
            "refusing to produce an empty security scan input"
        )

    header = [
        "# Generated from app/gradle.lockfile; do not edit.",
        f"# Scope: {RUNTIME_CONFIGURATION} (dependencies shipped in the release APK).",
        "",
    ]
    return "\n".join(header + sorted(selected)) + "\n"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--input",
        type=Path,
        default=Path("app/gradle.lockfile"),
        help="Source Gradle dependency lockfile",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("build/osv/gradle.lockfile"),
        help="Generated runtime-only Gradle lockfile",
    )
    args = parser.parse_args(argv)

    try:
        source = args.input.read_text(encoding="utf-8")
        output = runtime_lockfile_content(source)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(output, encoding="utf-8", newline="\n")
    except (OSError, ValueError) as error:
        print(f"Release runtime lockfile generation failed: {error}", file=sys.stderr)
        return 2

    dependency_count = sum(
        1
        for line in output.splitlines()
        if line and not line.startswith("#")
    )
    print(
        f"Generated {args.output} with {dependency_count} "
        f"{RUNTIME_CONFIGURATION} dependencies."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
