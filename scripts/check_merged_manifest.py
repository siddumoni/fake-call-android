#!/usr/bin/env python3
"""Compare final debug and release merged-manifest permissions with a strict allowlist."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from xml.etree import ElementTree

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
REQUESTED_PERMISSION_TAGS = {
    "uses-permission",
    "uses-permission-sdk-23",
    "uses-permission-sdk-m",
}


def read_permission_sets(manifest_path: Path) -> tuple[set[str], dict[str, str]]:
    """Return requested permissions and declared permission protection levels."""
    root = ElementTree.parse(manifest_path).getroot()
    requested: set[str] = set()
    defined: dict[str, str] = {}

    for element in root:
        tag = element.tag.rsplit("}", 1)[-1]
        name = element.attrib.get(f"{ANDROID_NS}name", "").strip()
        if not name:
            continue
        if tag in REQUESTED_PERMISSION_TAGS:
            requested.add(name)
        elif tag == "permission":
            defined[name] = element.attrib.get(
                f"{ANDROID_NS}protectionLevel", ""
            ).strip()

    return requested, defined


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--manifest",
        type=Path,
        help="Check one explicit merged manifest instead of both variants.",
    )
    parser.add_argument(
        "--variant",
        choices=("debug", "release"),
        help="Required with --manifest so the correct variant policy is applied.",
    )
    parser.add_argument(
        "--allowlist",
        type=Path,
        default=Path("config/manifest-permissions.json"),
    )
    args = parser.parse_args(argv)

    if args.manifest is not None:
        if args.variant is None:
            parser.error("--variant is required when --manifest is supplied")
        manifests = [(args.variant, args.manifest)]
    else:
        if args.variant is not None:
            parser.error("--variant may only be used with --manifest")
        intermediates = Path("app/build/intermediates/merged_manifests")
        manifests = [
            (
                variant,
                intermediates
                / variant
                / f"process{variant.capitalize()}Manifest"
                / "AndroidManifest.xml",
            )
            for variant in ("debug", "release")
        ]

    missing_files = [str(path) for _, path in manifests if not path.is_file()]
    if missing_files:
        print(
            f"Required final merged manifest(s) not found: {missing_files}",
            file=sys.stderr,
        )
        return 2

    try:
        policy = json.loads(args.allowlist.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        print(f"Unable to read manifest permission policy: {error}", file=sys.stderr)
        return 2

    variants = policy.get("variants", {})
    rationale = policy.get("rationale", {})
    failed = False

    for variant, manifest_path in manifests:
        variant_policy = variants.get(variant)
        if not isinstance(variant_policy, dict):
            print(
                f"No manifest permission policy is defined for {variant}.",
                file=sys.stderr,
            )
            failed = True
            continue

        allowed_requested = set(variant_policy.get("permissions", []))
        allowed_defined = dict(variant_policy.get("defined_permissions", {}))
        allowlisted_names = allowed_requested | set(allowed_defined)
        missing_rationale = sorted(
            permission
            for permission in allowlisted_names
            if not str(rationale.get(permission, "")).strip()
        )
        if missing_rationale:
            print(
                f"{variant} allowlisted permission(s) without a rationale: "
                f"{missing_rationale}",
                file=sys.stderr,
            )
            failed = True
            continue

        actual_requested, actual_defined = read_permission_sets(manifest_path)
        unexpected_requested = actual_requested - allowed_requested
        missing_requested = allowed_requested - actual_requested
        unexpected_defined = set(actual_defined) - set(allowed_defined)
        missing_defined = set(allowed_defined) - set(actual_defined)
        incorrect_protection = {
            name: {
                "expected": allowed_defined[name],
                "actual": actual_defined[name],
            }
            for name in set(actual_defined) & set(allowed_defined)
            if actual_defined[name] != allowed_defined[name]
        }

        print(f"{variant} merged manifest: {manifest_path}")
        if (
            unexpected_requested
            or missing_requested
            or unexpected_defined
            or missing_defined
            or incorrect_protection
        ):
            failed = True
            print(
                f"  Unexpected requested permissions: {sorted(unexpected_requested)}",
                file=sys.stderr,
            )
            print(
                f"  Expected but missing requested permissions: "
                f"{sorted(missing_requested)}",
                file=sys.stderr,
            )
            print(
                f"  Unexpected permission declarations: {sorted(unexpected_defined)}",
                file=sys.stderr,
            )
            print(
                f"  Expected but missing permission declarations: "
                f"{sorted(missing_defined)}",
                file=sys.stderr,
            )
            print(
                f"  Incorrect permission protection levels: {incorrect_protection}",
                file=sys.stderr,
            )
        else:
            print(
                "  Permission policy passed: exact requested permissions, "
                "declared permissions and protection levels match."
            )

    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
