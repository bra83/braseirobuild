#!/usr/bin/env python3
"""Galaxy A57 (SM-A576B, Android 16) / S23 Ultra mobile UI safeguards.

Applied only to a verified, disposable R36 (version 9.0.37) project.
Preserves source R35, SQLite, character state, TTS engines and all game logic.
Anchors bind to the real MainActivity; fail closed if the shell has changed.
"""
from pathlib import Path
import sys

MAIN = Path("android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java")
PATCHES = (
    (
        "            applySystemBarInsets(shellRoot);",
        """            // The insets listener belongs to the outer shellHost, not shellRoot.
            // Re-registering on shellRoot after rotation double-pads content.
            android.view.ViewParent parent=shellRoot.getParent();
            if(parent instanceof View)applySystemBarInsets((View)parent);"""
    ),
    (
        'TextView title=text("PATHFINDER 2E • BRASEIRO",19,GOLD,true);brand.addView(title);',
        'TextView title=text("PATHFINDER 2E • BRASEIRO",19,GOLD,true);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);brand.addView(title);'
    ),
    (
        "era.setLetterSpacing(.12f);brand.addView(era);",
        "era.setLetterSpacing(.12f);era.setSingleLine(true);era.setEllipsize(TextUtils.TruncateAt.END);brand.addView(era);"
    ),
    (
        "header.addView(quickVolume,new LinearLayout.LayoutParams(dp(48),dp(40)));",
        "header.addView(quickVolume,new LinearLayout.LayoutParams(dp(48),dp(48)));"
    ),
    (
        "Button stop=smallButton(\"PARAR\",RED);stop.setTextSize(10);",
        "Button stop=smallButton(\"PARAR\",RED);stop.setTextSize(12);"
    ),
    (
        "header.addView(stop,new LinearLayout.LayoutParams(dp(86),dp(40)));",
        "header.addView(stop,new LinearLayout.LayoutParams(dp(76),dp(48)));"
    ),
    (
        "root.addView(header,new LinearLayout.LayoutParams(-1,dp(54)));",
        "root.addView(header,new LinearLayout.LayoutParams(-1,dp(60)));"
    ),
    (
        "root.addView(navScroll,new LinearLayout.LayoutParams(-1,dp(50)));",
        "root.addView(navScroll,new LinearLayout.LayoutParams(-1,dp(56)));"
    ),
    (
        "root.addView(statusText,new LinearLayout.LayoutParams(-1,dp(44)));",
        "root.addView(statusText,new LinearLayout.LayoutParams(-1,dp(48)));"
    ),
    (
        "LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(46));",
        "LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(48));"
    ),
    (
        "                left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;",
        """                left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;
                // Android 15/16 edge-to-edge + Samsung gesture navigation:
                // keep chat/input and bottom controls above the on-screen keyboard.
                if(Build.VERSION.SDK_INT>=35){
                    android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());
                    bottom=Math.max(bottom,ime.bottom);
                }"""
    ),
)


def transform(source):
    for old, new in PATCHES:
        count = source.count(old)
        if count != 1:
            raise RuntimeError("Galaxy A57 UI source anchor changed (count=%d): %s" % (count, old[:85]))
        source = source.replace(old, new, 1)
    return source


def apply(project):
    root = Path(project).resolve()
    version = root / "version.properties"
    if not version.is_file() or "VERSION_NAME=9.0.37" not in version.read_text(encoding="utf-8").splitlines():
        raise RuntimeError("Galaxy A57 patch requires disposable R36 9.0.37; never modify R35.")
    path = root / MAIN
    if not path.is_file():
        raise RuntimeError("MainActivity production source not found.")
    old = path.read_text(encoding="utf-8")
    new = transform(old)
    if 'setContentView(shellHost);' not in new or 'applySystemBarInsets(shellHost);' not in new:
        raise RuntimeError("Production safe-area installation disappeared.")
    path.write_text(new, encoding="utf-8")
    print("R36_A57_ANDROID16_UI_PATCHED=true touch_min_dp=48 gesture_insets=true ime_insets=true")
    print("R36_A57_PHYSICAL_DEVICE_CERTIFIED=false")
    return new


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: patch_galaxy_a57_compat.py <disposable_R36_project>")
    apply(sys.argv[1])
