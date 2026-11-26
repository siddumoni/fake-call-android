#!/usr/bin/env python3
"""Compare final debug and release merged-manifest permissions with the allowlist."""

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


def read_permission_sets(manifest_path: Path) -> tuple[set[str], set[str]]:
    root = ElementTree.parse(manifest_path).getroot()
    requested: set[str] = set()
    defined: set[str] = set()

    for element in root:
        tag = element.tag.rsplit("}", 1)[-1]
        name = element.attrib.get(f"{ANDROID_NS}name", "").strip()
        if not name:
            continue
        if tag in REQUESTED_PERMISSION_TAGS:
            requested.add(name)
        elif tag == "permission":
            defined.add(name)

    return requested, defined


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--manifest",
        type=Path,
        help="Check one explicit merged manifest instead of both variants.",
    )
    parser.add_argument(
        "--allowlist",
        type=Path,
        default=Path("config/manifest-permissions.json"),
    )
    args = parser.parse_args()

    if args.manifest is not None:
        manifests = [("explicit", args.manifest)]
    else:
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

    policy = json.loads(args.allowlist.read_text(encoding="utf-8"))
    allowed_requested = set(policy.get("permissions", []))
    allowed_defined = set(policy.get("defined_permissions", []))
    rationale = policy.get("rationale", {})

    missing_rationale = sorted(
        permission
        for permission in allowed_requested | allowed_defined
        if not str(rationale.get(permission, "")).strip()
    )
    if missing_rationale:
        print(
            f"Allowlisted permission(s) without a rationale: {missing_rationale}",
            file=sys.stderr,
        )
        return 2

    failed = False
    for variant, manifest_path in manifests:
        actual_requested, actual_defined = read_permission_sets(manifest_path)
        unexpected_requested = actual_requested - allowed_requested
        missing_requested = allowed_requested - actual_requested
        unexpected_defined = actual_defined - allowed_defined
        missing_defined = allowed_defined - actual_defined

        print(f"{variant} merged manifest: {manifest_path}")
        if (
            unexpected_requested
            or missing_requested
            or unexpected_defined
            or missing_defined
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
        else:
            print(
                "  Permission policy passed: no unreviewed requested or "
                "defined permissions."
            )

    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
