#!/usr/bin/env python3
"""Check ordered patches in temporary indexes without changing source checkouts."""
import json
import os
from pathlib import Path
import subprocess
import tempfile

catalog = Path(__file__).resolve().parent
root = catalog.parents[3]
series = json.loads((catalog / "series.json").read_text())
for entry in series["repositories"]:
    with tempfile.TemporaryDirectory(prefix="annibale-patches-") as temporary:
        env = dict(os.environ, GIT_INDEX_FILE=str(Path(temporary) / "index"))

        def git(*args):
            return subprocess.check_output(
                ["git", "-C", str(root / entry["repo"]), *args], env=env, text=True
            ).strip()

        git("read-tree", entry["base_commit"])
        for patch in entry["patches"]:
            git("apply", "--cached", str(catalog / patch["file"]))
        actual = git("write-tree")
        if actual != entry["result_tree"]:
            raise SystemExit(f"FAIL {entry['repo']}: unexpected tree {actual}")
    print(f"PASS {entry['repo']}")
print(f"Verified {len(series['repositories'])} repositories")
