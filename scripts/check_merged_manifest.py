#!/usr/bin/env python3
"""Compare the debug merged manifest permissions with the reviewed allowlist."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from xml.etree import ElementTree

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
PERMISSION_TAGS = {
    "uses-permission",
    "uses-permission-sdk-23",
    "uses-permission-sdk-m",
}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path)
    parser.add_argument(
        "--allowlist",
        type=Path,
        default=Path("config/manifest-permissions.json"),
    )
    args = parser.parse_args()

    manifest_path = args.manifest
    if manifest_path is None:
        candidates = [
            path
            for path in Path("app/build/intermediates").rglob("AndroidManifest.xml")
            if "merged" in str(path).lower() and "debug" in path.parts
        ]
        if len(candidates) != 1:
            print(
                f"Expected one merged debug manifest, found {len(candidates)}: "
                f"{candidates}",
                file=sys.stderr,
            )
            return 2
        manifest_path = candidates[0]

    policy = json.loads(args.allowlist.read_text(encoding="utf-8"))
    allowed = set(policy.get("permissions", []))
    root = ElementTree.parse(manifest_path).getroot()
    actual = {
        element.attrib.get(f"{ANDROID_NS}name", "")
        for element in root
        if element.tag.rsplit("}", 1)[-1] in PERMISSION_TAGS
    }
    actual.discard("")
    unexpected = actual - allowed
    missing = allowed - actual

    if unexpected or missing:
        print(f"Merged manifest: {manifest_path}", file=sys.stderr)
        print(f"Unexpected permissions: {sorted(unexpected)}", file=sys.stderr)
        print(f"Expected but missing permissions: {sorted(missing)}", file=sys.stderr)
        return 1

    print(f"Manifest permission policy passed: {sorted(actual)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
