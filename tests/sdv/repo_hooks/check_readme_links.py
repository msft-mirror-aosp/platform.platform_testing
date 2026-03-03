#!/usr/bin/env python3
# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Check if local file paths referenced in README.md files exist."""

import argparse
import os
import re
import sys
from typing import List
from urllib.parse import unquote

# Safely locate the AOSP workspace root.
fallback_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../../.."))
workspace_root = os.environ.get("REPO_ROOT", fallback_root)

# Add the tools/repohooks path so we can import 'rh'.
repohooks_path = os.path.join(workspace_root, "tools", "repohooks")
if repohooks_path not in sys.path:
    sys.path.insert(0, repohooks_path)

try:
    import rh.git
except ImportError:
    print(f"\nFATAL: Could not import 'rh.git'.\nLooked in: {repohooks_path}\n", file=sys.stderr)
    sys.exit(1)

LINK_RE = re.compile(r'\[.*?\]\((.*?)\)')

def get_parser() -> argparse.ArgumentParser:
    """Returns a command line parser."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "files",
        nargs="+",
        help="The file paths to check.",
    )
    parser.add_argument(
        "--commit-hash",
        "-c",
        help="The commit hash to check.",
        default="HEAD",
    )
    return parser

def main(argv: List[str]) -> int:
    """The main entry."""
    parser = get_parser()
    opts = parser.parse_args(argv)

    all_passed = True

    for file_path in opts.files:
        # We only care about README.md files
        if not file_path.endswith("README.md"):
            continue

        contents = rh.git.get_file_content(opts.commit_hash, file_path)
        links = LINK_RE.findall(contents)

        for link in links:
            # Ignore standard web URLs and email addresses
            if link.startswith(("http://", "https://", "mailto:")):
                continue

            # Strip fragment identifiers (e.g., file.md#section-name)
            link_path = link.split('#')[0]
            if not link_path:
                continue

            # Decode URL-encoded characters (like %20 to space)
            link_path = unquote(link_path)

            # Resolve the path to check it on the local filesystem
            if link_path.startswith('/'):
                # Treat as absolute path starting from the AOSP workspace root
                target_path = os.path.join(workspace_root, link_path.lstrip('/'))
            else:
                # Treat as relative to the current README.md's directory.
                # os.getcwd() during repohooks is the root of the current Git project (platform_testing/)
                file_dir = os.path.dirname(file_path)
                target_path = os.path.normpath(os.path.join(os.getcwd(), file_dir, link_path))

            # Verify the file actually exists in the source tree
            if not os.path.exists(target_path):
                print(f"{file_path}: Broken link found -> '{link}' (resolved to {target_path})", file=sys.stderr)
                all_passed = False

    return 0 if all_passed else 1

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
