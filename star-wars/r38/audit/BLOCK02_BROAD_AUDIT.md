# STAR WARS — EDGE OF THE GALAXY — BLOCK02 BROAD AUDIT

- Files scanned: **261**
- Critical: **1**
- High: **14**
- Medium: **1**
- R46 JS syntax files checked: **4**
- Local refs checked: **18**; missing: **12**
- Data actions: **2**; unbound: **0**
- Native bridge calls checked: **0**

## Findings

- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: location.href (`app/src/main/assets/vtt/r46/final-runtime.js`)
- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: blob (`app/src/main/assets/vtt/r46/r46-shell.bundle.js`)
- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: url (`app/src/main/assets/vtt/r46/r46-shell.bundle.js`)
- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: value (`app/src/main/assets/vtt/r46/r46-shell.bundle.js`)
- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: String(value  (`app/src/main/assets/vtt/r46/r46-shell.bundle.js`)
- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: asset.remoteUrl (`app/src/main/assets/vtt/r46/r46-shell.bundle.js`)
- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: asset (`app/src/main/assets/vtt/r46/r46-shell.bundle.js`)
- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: asset (`app/src/main/assets/vtt/r46/r46-shell.bundle.js`)
- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: asset.remoteUrl (`app/src/main/assets/vtt/r46/r46-shell.bundle.js`)
- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: ${source} (`app/src/main/assets/vtt/r46/r46-shell.bundle.js`)
- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: window.location.href (`app/src/main/assets/vtt/r46/r46-shell.bundle.js`)
- **HIGH · MISSING_LOCAL_ASSET** — Local UI asset not found: window.location.href (`app/src/main/assets/vtt/r46/r46-shell.bundle.js`)
- **MEDIUM · VIEWPORT_FIT** — viewport-fit=cover not found in R46 shell
- **HIGH · SAFE_AREA_BINDING** — native bottom inset CSS variable not found in R46
- **HIGH · ANDROID_INSETS** — No modern WindowInsets handling detected in MainActivity
- **CRITICAL · R46_BOOT** — Could not load R46 shell in browser runtime

## Runtime smoke

- URL: `None`
- Page errors: **0**
- Console errors: **2**
- Failed requests: **0**
- Broken rendered images: **0**
- Navigation controls exercised: **0**
