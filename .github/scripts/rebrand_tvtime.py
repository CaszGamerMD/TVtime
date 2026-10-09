#!/usr/bin/env python3
"""One-time code, resource, command, and documentation rebrand."""
from pathlib import Path
import json
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
REPO_URL = "https://github.com/CaszGamerMD/TVtime"
PROTECTED_URL = "__TVTIME_REPOSITORY_URL__"
SKIP = {".github/scripts/rebrand_tvtime.py", ".github/workflows/rebrand.yml", "LICENSE"}


def new_path(old):
    return (old
        .replace("/com/caszgamermd/tvtime/", "/com/caszgamermd/caszualtvtime/")
        .replace("src/main/resources/assets/tvtime/", "src/main/resources/assets/caszual_tv_time/")
        .replace("src/main/resources/data/tvtime/", "src/main/resources/data/caszual_tv_time/")
        .replace("src/client/resources/tvtime.client.mixins.json",
                 "src/client/resources/caszual_tv_time.client.mixins.json")
        .replace("TVtime", "CaszualTvTime"))


def new_contents(path, text):
    text = text.replace(REPO_URL, PROTECTED_URL)
    text = text.replace("tvtime-capture", "caszual-tv-time-capture")
    text = text.replace("TVTIME", "CASZUAL_TV_TIME")
    if path.endswith(".java"):
        text = text.replace("com.caszgamermd.tvtime", "com.caszgamermd.caszualtvtime")
        text = text.replace("TVtime", "CaszualTvTime")
        text = re.sub(r'"(?:\\.|[^"\\])*"',
                      lambda match: match.group(0).replace("CaszualTvTime", "Caszual TV Time"),
                      text)
        text = text.replace("tvtime", "caszual_tv_time")
    else:
        text = text.replace("TVtime", "Caszual TV Time")
        text = text.replace("tvtime", "caszual_tv_time")

    text = text.replace(PROTECTED_URL, REPO_URL)
    text = text.replace("0.1.0-alpha.1", "0.1.0-alpha.2")
    text = text.replace("Caszual TV Time-0.1.0-alpha.2.jar",
                        "caszual-tv-time-0.1.0-alpha.2.jar")

    if path == "src/main/resources/fabric.mod.json":
        meta = json.loads(text)
        meta["id"] = "caszual_tv_time"
        meta["name"] = "Caszual TV Time"
        meta["description"] = ("In-world TVs, desktop broadcasting, cameras "
                               "and positional audio for Fabric.")
        meta["entrypoints"]["main"] = ["com.caszgamermd.caszualtvtime.CaszualTvTime"]
        meta["entrypoints"]["client"] = ["com.caszgamermd.caszualtvtime.client.CaszualTvTimeClient"]
        meta["mixins"][0]["config"] = "caszual_tv_time.client.mixins.json"
        text = json.dumps(meta, indent=2) + "\n"
    if path == "gradle.properties":
        text = re.sub(r"^archives_base_name=.*$", "archives_base_name=caszual-tv-time",
                      text, flags=re.M)
        text = re.sub(r"^maven_group=.*$", "maven_group=com.caszgamermd.caszualtvtime",
                      text, flags=re.M)
    if path == "settings.gradle":
        text = re.sub(r'rootProject.name = "[^"]+"', 'rootProject.name = "Caszual TV Time"', text)
    if path == "README.md":
        text += ("\n## Breaking rebrand\n\nPreviously called TVtime. The new mod ID "
                 "is caszual_tv_time. Old tvtime block/item IDs are not migrated, "
                 "as requested; remove the prior JAR before installing the new one. "
                 "The GitHub repository URL remains unchanged.\n")
    return text


def repair_rebranded_tree():
    tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).split(b"\0")
    count = 0
    for entry in tracked:
        if not entry:
            continue
        name = entry.decode("utf-8")
        if name in SKIP or name.startswith(".github/workflows/"):
            continue
        path = ROOT / name
        if not path.is_file():
            continue
        try:
            before = path.read_text(encoding="utf-8")
        except UnicodeError:
            continue
        after = (before
            .replace("com.caszgamermd.caszualcaszual_tv_time",
                     "com.caszgamermd.caszualtvtime")
            .replace("com.caszgamermd.caszual_tv_time",
                     "com.caszgamermd.caszualtvtime")
            .replace("__CASZUAL_TV_TIME_REPOSITORY_URL__", REPO_URL))
        if after != before:
            path.write_text(after, encoding="utf-8")
            count += 1
    assert "com.caszgamermd.caszualtvtime" in (
        ROOT / "src/main/java/com/caszgamermd/caszualtvtime/CaszualTvTime.java").read_text()
    assert REPO_URL in (ROOT / "src/main/resources/fabric.mod.json").read_text()
    print("Rebrand integrity fixes applied to", count, "files")


def main():
    descriptor = ROOT / 'src/main/resources/fabric.mod.json'
    if descriptor.is_file() and json.loads(descriptor.read_text()).get('id') == 'caszual_tv_time':
        repair_rebranded_tree()
        return
    tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).split(b"\0")
    modified = moved = 0
    for entry in tracked:
        if not entry:
            continue
        name = entry.decode("utf-8")
        if name in SKIP:
            continue
        current = ROOT / name
        if name == "dist/TVtime-0.1.0-alpha.1.jar":
            current.unlink(missing_ok=True)
            modified += 1
            continue
        if not current.is_file():
            continue
        try:
            previous = current.read_text(encoding="utf-8")
        except UnicodeError:
            continue
        destination = new_path(name)
        replacement = new_contents(name, previous)
        if destination == name and previous == replacement:
            continue
        target = ROOT / destination
        if target != current and target.exists():
            raise RuntimeError("Destination already exists: " + destination)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(replacement, encoding="utf-8")
        if target != current:
            current.unlink()
            moved += 1
        modified += 1

    descriptor = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text())
    assert descriptor["id"] == "caszual_tv_time"
    assert (ROOT / "src/client/resources/caszual_tv_time.client.mixins.json").is_file()
    assert (ROOT / "src/main/java/com/caszgamermd/caszualtvtime/CaszualTvTime.java").is_file()
    assert (ROOT / "src/client/java/com/caszgamermd/caszualtvtime/client/CaszualTvTimeClient.java").is_file()
    assert "caszual-tv-time-capture.exe" in (ROOT / ".github/workflows/windows-test-bundle.yml").read_text()
    assert "caszual_tv_time/native/windows-x64" in (ROOT / ".github/workflows/windows-test-bundle.yml").read_text()
    print(f"Rebranded {modified} files and moved {moved} paths")


if __name__ == "__main__":
    main()
