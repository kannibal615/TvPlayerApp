#!/usr/bin/env python3
"""Capture reproducible Android TV audit evidence from an adb target."""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path
from typing import Iterable


KEYS = {
    "UP": "KEYCODE_DPAD_UP",
    "DPAD_UP": "KEYCODE_DPAD_UP",
    "DOWN": "KEYCODE_DPAD_DOWN",
    "DPAD_DOWN": "KEYCODE_DPAD_DOWN",
    "LEFT": "KEYCODE_DPAD_LEFT",
    "DPAD_LEFT": "KEYCODE_DPAD_LEFT",
    "RIGHT": "KEYCODE_DPAD_RIGHT",
    "DPAD_RIGHT": "KEYCODE_DPAD_RIGHT",
    "OK": "KEYCODE_DPAD_CENTER",
    "ENTER": "KEYCODE_DPAD_CENTER",
    "DPAD_CENTER": "KEYCODE_DPAD_CENTER",
    "BACK": "KEYCODE_BACK",
    "HOME": "KEYCODE_HOME",
    "MENU": "KEYCODE_MENU",
    "PLAY_PAUSE": "KEYCODE_MEDIA_PLAY_PAUSE",
}


class AdbError(RuntimeError):
    pass


def adb_program() -> str:
    candidates = [
        os.environ.get("ADB_PATH", ""),
        shutil.which("adb") or "",
    ]
    for root_name in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        root = os.environ.get(root_name, "")
        if root:
            candidates.extend(
                [
                    str(Path(root) / "platform-tools" / "adb"),
                    str(Path(root) / "platform-tools" / "adb.exe"),
                ]
            )
    local_app_data = os.environ.get("LOCALAPPDATA", "")
    if local_app_data:
        candidates.append(str(Path(local_app_data) / "Android" / "Sdk" / "platform-tools" / "adb.exe"))

    for candidate in candidates:
        if candidate and (shutil.which(candidate) or Path(candidate).is_file()):
            return candidate
    raise AdbError(
        "adb was not found; add platform-tools to PATH or set ADB_PATH to the adb/adb.exe location"
    )


def run(
    command: list[str], *, check: bool = True, binary: bool = False, timeout: int = 45
) -> str | bytes:
    try:
        result = subprocess.run(command, capture_output=True, check=False, timeout=timeout)
    except subprocess.TimeoutExpired as exc:
        raise AdbError(f"Command timed out after {timeout}s: {command[0]}") from exc
    if check and result.returncode != 0:
        stderr = result.stderr.decode("utf-8", "replace").strip()
        stdout = result.stdout.decode("utf-8", "replace").strip()
        raise AdbError(stderr or stdout or f"Command failed: {' '.join(command)}")
    if binary:
        return result.stdout
    return result.stdout.decode("utf-8", "replace").replace("\r\n", "\n")


def adb(serial: str, *args: str, check: bool = True, binary: bool = False) -> str | bytes:
    return run([adb_program(), "-s", serial, *args], check=check, binary=binary)


def safe_name(value: str) -> str:
    value = re.sub(r"[^a-zA-Z0-9._-]+", "-", value.strip()).strip("-._")
    return value[:80] or "checkpoint"


def write_text(path: Path, value: str | bytes) -> None:
    if isinstance(value, bytes):
        value = value.decode("utf-8", "replace")
    path.write_text(value, encoding="utf-8")


def ensure_device(serial: str) -> dict[str, str]:
    executable = adb_program()
    connect_output = ""
    if ":" in serial:
        connect_output = str(run([executable, "connect", serial], check=False)).strip()

    devices = str(run([executable, "devices"]))
    state = "missing"
    for line in devices.splitlines()[1:]:
        parts = line.split()
        if parts and parts[0] == serial:
            state = parts[1] if len(parts) > 1 else "unknown"
            break

    if state == "unauthorized":
        raise AdbError("Device is unauthorized; accept the adb authorization dialog on the TV")
    if state != "device":
        detail = f" ({connect_output})" if connect_output else ""
        raise AdbError(f"Device {serial} is not ready; adb state={state}{detail}")
    return {"serial": serial, "state": state, "connect_output": connect_output}


def shell_text(serial: str, command: str, *, check: bool = False) -> str:
    # Pass the complete remote command as one adb argument. Adding a second
    # `sh -c` layer makes adb split the command differently on Windows, so
    # Fire OS receives only the executable and drops its arguments.
    return str(adb(serial, "shell", command, check=check)).strip()


def infer_package(serial: str) -> str:
    activity = shell_text(serial, "dumpsys activity activities")
    patterns = [
        r"mResumedActivity:.*?\s([A-Za-z0-9._]+)/(?:[A-Za-z0-9._$]+)",
        r"topResumedActivity=.*?\s([A-Za-z0-9._]+)/(?:[A-Za-z0-9._$]+)",
    ]
    for pattern in patterns:
        match = re.search(pattern, activity)
        if match:
            return match.group(1)
    return ""


def getprop(serial: str, name: str) -> str:
    return shell_text(serial, f"getprop {name}")


def package_version(package_dump: str) -> dict[str, str]:
    output: dict[str, str] = {}
    for key in ("versionName", "versionCode"):
        match = re.search(rf"\b{key}=([^\s]+)", package_dump)
        if match:
            output[key] = match.group(1)
    return output


def command_preflight(args: argparse.Namespace) -> int:
    connection = ensure_device(args.serial)
    output = Path(args.output).expanduser().resolve()
    output.mkdir(parents=True, exist_ok=True)
    package = args.package or infer_package(args.serial)

    device = {
        **connection,
        "captured_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "manufacturer": getprop(args.serial, "ro.product.manufacturer"),
        "model": getprop(args.serial, "ro.product.model"),
        "device": getprop(args.serial, "ro.product.device"),
        "android_version": getprop(args.serial, "ro.build.version.release"),
        "sdk": getprop(args.serial, "ro.build.version.sdk"),
        "build_fingerprint": getprop(args.serial, "ro.build.fingerprint"),
        "display_size": shell_text(args.serial, "wm size"),
        "display_density": shell_text(args.serial, "wm density"),
        "package": package,
        "perfetto_available": bool(shell_text(args.serial, "command -v perfetto")),
        "simpleperf_available": bool(shell_text(args.serial, "command -v simpleperf")),
    }

    if package:
        dump = shell_text(args.serial, f"dumpsys package {package}")
        device.update(package_version(dump))
        write_text(output / "package.txt", dump)
        write_text(
            output / "launcher-activity.txt",
            shell_text(args.serial, f"cmd package resolve-activity --brief {package}"),
        )

    write_text(output / "activity-preflight.txt", shell_text(args.serial, "dumpsys activity activities"))
    write_text(output / "window-preflight.txt", shell_text(args.serial, "dumpsys window windows"))
    (output / "device.json").write_text(json.dumps(device, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(device, indent=2, ensure_ascii=False))
    if not package:
        print("Warning: no foreground package inferred; pass --package for package-scoped evidence", file=sys.stderr)
    return 0


def normalized_keys(values: Iterable[str]) -> list[str]:
    keys: list[str] = []
    for value in values:
        for item in value.split(","):
            name = item.strip().upper()
            if not name:
                continue
            key = KEYS.get(name, name if name.startswith("KEYCODE_") else "")
            if not key:
                raise AdbError(f"Unsupported key: {item}")
            keys.append(key)
    return keys


def focus_lines(window_dump: str, activity_dump: str) -> list[str]:
    lines = []
    for text in (window_dump, activity_dump):
        for line in text.splitlines():
            if any(token in line for token in ("mCurrentFocus", "mFocusedApp", "mResumedActivity", "topResumedActivity")):
                clean = line.strip()
                if clean and clean not in lines:
                    lines.append(clean)
    return lines


def command_capture(args: argparse.Namespace) -> int:
    ensure_device(args.serial)
    package = args.package or infer_package(args.serial)
    root = Path(args.output).expanduser().resolve()
    checkpoints = root / "checkpoints"
    checkpoints.mkdir(parents=True, exist_ok=True)
    stamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S-%f")
    checkpoint = checkpoints / f"{stamp}-{safe_name(args.label)}"
    checkpoint.mkdir(parents=True, exist_ok=False)

    keys = normalized_keys(args.key or [])
    started = time.monotonic()
    for key in keys:
        adb(args.serial, "shell", "input", "keyevent", key)
        time.sleep(args.settle_ms / 1000.0)

    screenshot = adb(args.serial, "exec-out", "screencap", "-p", binary=True)
    if not screenshot:
        raise AdbError("screencap returned no image data")
    (checkpoint / "screenshot.png").write_bytes(screenshot)

    ui_device_path = f"/data/local/tmp/codex-ui-{os.getpid()}.xml"
    ui_status = shell_text(args.serial, f"uiautomator dump {ui_device_path} 2>&1")
    ui_xml = adb(args.serial, "exec-out", "cat", ui_device_path, check=False)
    shell_text(args.serial, f"rm -f {ui_device_path}")
    write_text(checkpoint / "ui.xml", ui_xml)
    write_text(checkpoint / "ui-dump-status.txt", ui_status)

    window_dump = shell_text(args.serial, "dumpsys window windows")
    activity_dump = shell_text(args.serial, "dumpsys activity activities")
    write_text(checkpoint / "window.txt", window_dump)
    write_text(checkpoint / "activity.txt", activity_dump)

    pid = shell_text(args.serial, f"pidof -s {package}") if package else ""
    if pid:
        logs = str(adb(args.serial, "logcat", "-d", "--pid", pid, "-t", str(args.log_lines), check=False))
        if not logs.strip():
            logs = str(adb(args.serial, "logcat", "-d", "-t", str(args.log_lines), check=False))
    else:
        logs = str(adb(args.serial, "logcat", "-d", "-t", str(args.log_lines), check=False))
    write_text(checkpoint / "logcat.txt", logs)

    gfxinfo = shell_text(args.serial, f"dumpsys gfxinfo {package} framestats") if package else ""
    write_text(checkpoint / "gfxinfo-framestats.txt", gfxinfo)

    metadata = {
        "label": args.label,
        "captured_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "serial": args.serial,
        "package": package,
        "pid": pid,
        "keys": keys,
        "settle_ms": args.settle_ms,
        "capture_elapsed_ms": round((time.monotonic() - started) * 1000, 1),
        "focus": focus_lines(window_dump, activity_dump),
        "ui_dump_status": ui_status,
        "files": sorted(path.name for path in checkpoint.iterdir()),
    }
    (checkpoint / "metadata.json").write_text(
        json.dumps(metadata, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    print(checkpoint)
    return 0


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description=__doc__)
    sub = root.add_subparsers(dest="command", required=True)

    preflight = sub.add_parser("preflight", help="Verify adb access and record device/package metadata")
    preflight.add_argument("--serial", required=True, help="adb serial, for example 192.168.1.33:5555")
    preflight.add_argument("--output", required=True, help="Audit run directory")
    preflight.add_argument("--package", default="", help="Application package; infer foreground package if omitted")
    preflight.set_defaults(handler=command_preflight)

    capture = sub.add_parser("capture", help="Optionally send TV keys and capture an evidence checkpoint")
    capture.add_argument("--serial", required=True)
    capture.add_argument("--output", required=True)
    capture.add_argument("--package", default="", help="Application package; infer foreground package if omitted")
    capture.add_argument("--label", required=True)
    capture.add_argument("--key", action="append", help="TV key; repeat or provide a comma-separated sequence")
    capture.add_argument("--settle-ms", type=int, default=700, help="Delay after each key event")
    capture.add_argument("--log-lines", type=int, default=500, help="Recent Logcat lines to retain")
    capture.set_defaults(handler=command_capture)
    return root


def main() -> int:
    args = parser().parse_args()
    try:
        return int(args.handler(args))
    except AdbError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
