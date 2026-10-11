#!/usr/bin/env python3
"""Actual installed APK native Android WebView smoke, using page-level CDP.
Android WebView does not support Playwright's Browser.setDownloadBehavior.
CDP Runtime.evaluate works through the WebView's per-page debugger socket.
No game DB mutation, no artificial results, no physical device certification.
"""
import json, os, re, subprocess, time, urllib.request, uuid
from pathlib import Path

from websocket import create_connection, WebSocketTimeoutException

APP = "com.braseiro.forbiddenlands"
EXPECTED_SOURCE = "b4e48fb77b7b7fdc5f85d1c998518196a0aaac32"
ROOT = Path.cwd()
OUT = ROOT / "fl-u23-android-smoke.json"
SCREENSHOT = ROOT / "fl-u23-android-smoke.png"

def adb(*args, timeout=30, binary=False):
    result = subprocess.run(["adb", *args], capture_output=True, timeout=timeout,
        check=True, text=not binary)
    return result.stdout if binary else result.stdout.strip()

def json_get(path):
    with urllib.request.urlopen("http://127.0.0.1:9222" + path, timeout=6) as response:
        return json.load(response)

def discover_socket(pid):
    sockets = adb("shell", "cat", "/proc/net/unix")
    target = "webview_devtools_remote_" + str(pid)
    matches = re.findall(r"@(webview_devtools_remote[^\s]*)", sockets)
    return next((socket for socket in matches if socket == target), None)

class CdpPage:
    def __init__(self, websocket_url):
        self.url = websocket_url
        # Android WebView page debugger is reachable only through the local
        # adb-forwarded app socket. Its CDP server accepts the standard
        # Playwright-style no-Origin handshake, not a browser Origin header.
        self.conn = create_connection(websocket_url, timeout=12,
            suppress_origin=True, enable_multithread=True)
        self.id = 0

    def close(self):
        self.conn.close()

    def command(self, method, params=None):
        self.id += 1
        mid = self.id
        self.conn.send(json.dumps({"id": mid, "method": method,
            "params": params or {}}, separators=(",", ":")))
        end = time.monotonic() + 15
        while time.monotonic() < end:
            try:
                msg = json.loads(self.conn.recv())
            except WebSocketTimeoutException:
                continue
            if msg.get("id") != mid:
                continue
            if "error" in msg:
                raise RuntimeError("CDP " + method + ": " + str(msg["error"]))
            return msg.get("result", {})
        raise TimeoutError("No CDP response for " + method)

    def evaluate(self, expression, await_promise=True):
        response = self.command("Runtime.evaluate", {
            "expression": expression, "awaitPromise": await_promise,
            "returnByValue": True})
        if response.get("exceptionDetails"):
            raise AssertionError("JS exception: " +
                str(response["exceptionDetails"].get("text")))
        remote = response.get("result", {})
        if remote.get("subtype") == "error":
            raise AssertionError("JS runtime error: " + str(remote))
        return remote.get("value")

def connect_page():
    for _ in range(40):
        try:
            pages = json_get("/json/list")
            candidates = [entry for entry in pages
                if entry.get("type") == "page" and entry.get("webSocketDebuggerUrl")
                and entry.get("url", "").startswith(
                    ("http://localhost", "https://localhost", "capacitor://localhost"))]
            if candidates:
                entry = candidates[0]
                return CdpPage(entry["webSocketDebuggerUrl"]), entry["url"]
        except (OSError, ValueError, KeyError):
            pass
        time.sleep(1)
    raise AssertionError("No debuggable installed local WebView page")

def verify_one(index):
    adb("shell", "am", "force-stop", APP)
    launch = adb("shell", "monkey", "-p", APP,
        "-c", "android.intent.category.LAUNCHER", "1", timeout=25)
    assert "Events injected: 1" in launch, "The installed app could not launch"
    pid = None
    socket = None
    for _ in range(90):
        try:
            pid_text = adb("shell", "pidof", APP)
            if pid_text:
                pid = int(pid_text.split()[0])
                socket = discover_socket(pid)
                if socket:
                    break
        except (ValueError, subprocess.CalledProcessError):
            pass
        time.sleep(1)
    assert socket and pid, "Installed app did not create a native WebView process"
    subprocess.run(["adb", "forward", "--remove", "tcp:9222"], capture_output=True)
    adb("forward", "tcp:9222", "localabstract:" + socket)
    page, url = connect_page()
    try:
        page.command("Runtime.enable")
        state = None
        for _ in range(90):
            state = page.evaluate("""(() => ({
              ready: document.readyState,
              rootChildren: document.querySelector('#root')?.children.length || 0,
              textLength: (document.body?.innerText || '').trim().length,
              moduleLoading: (document.body?.innerText || '').includes('Abrindo módulo'),
              excerpt: (document.body?.innerText || '').slice(0,300),
              native: !!window.Capacitor?.isNativePlatform?.()
            }))()""")
            if state and state.get("ready") == "complete" and                     state.get("rootChildren", 0) > 0 and                     state.get("textLength", 0) > 80 and not state.get("moduleLoading"):
                break
            time.sleep(1)
        assert state and state["ready"] == "complete", state
        assert state["rootChildren"] > 0 and state["textLength"] > 80, state
        assert not state.get("moduleLoading"), "Module lazy-loader remained visible after 90 seconds"
        assert state["native"], "JS did not detect Capacitor native platform"
        assert re.search(r"Braseiro|Forbidden|Aventura|Campanha|Configura|Personage|MESTRE IA",
            state["excerpt"], re.I), state["excerpt"]
        provenance = page.evaluate("""(async()=>{
          const r=await fetch('/fl-build-source.json',{cache:'no-store'});
          if(!r.ok)throw Error('source HTTP '+r.status);
          return await r.json();
        })()""")
        assert isinstance(provenance, dict), "Build-source marker missing"
        assert provenance.get("sourceCommit") == EXPECTED_SOURCE, provenance
        result = {
            "launch": index, "status": "PASS", "pid": pid,
            "webviewUrl": url, "native": state["native"],
            "textLength": state["textLength"],
            "sourceCommit": provenance["sourceCommit"]}
        if index == 3:
            SCREENSHOT.write_bytes(adb("exec-out", "screencap", "-p",
                timeout=25, binary=True))
            assert SCREENSHOT.stat().st_size > 4000, "Screen capture missing"
        return result
    finally:
        page.close()
        subprocess.run(["adb", "forward", "--remove", "tcp:9222"], capture_output=True)

def main():
    results = []
    report = {"status": "FAIL", "apk": "BRASEIRO_FORBIDDEN_LANDS_3.3.24_U23_INTEGRATED.apk",
        "sourceCommit": EXPECTED_SOURCE, "launches": results,
        "scope": "Actual published U23 APK installed in Android API35 emulator; three native WebView force-stop/reopen sessions; no physical Galaxy or external TTS verified."}
    try:
        for iteration in (1, 2, 3):
            item = verify_one(iteration)
            results.append(item)
            print("ANDROID_WEBVIEW_REOPEN_PASS " + json.dumps(item), flush=True)
        log = adb("logcat", "-d", "-t", "5000", timeout=35)
        assert not re.search(r"FATAL EXCEPTION[\s\S]{0,1500}Process: " +
            re.escape(APP), log), "App fatal exception in logcat"
        report["status"] = "PASS"
        report["androidApi"] = adb("shell", "getprop", "ro.build.version.sdk")
        assert report["androidApi"] == "35", report["androidApi"]
    except Exception as error:
        report["failure"] = type(error).__name__ + ": " + str(error)
        raise
    finally:
        OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n",
            encoding="utf8")
        print("ANDROID_WEBVIEW_SMOKE " + json.dumps(report, ensure_ascii=False),
            flush=True)
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
