# JavScraper Phase 4 — Packaging

**Status:** ⬜ PENDING

**Goal:** Package Python Worker as a standalone exe, build Windows MSI installer for Kotlin Desktop app, and merge both into a single installer.

## Task 1: PyInstaller — Package Python Worker

### 1.1 Install PyInstaller
```powershell
cd scraper-worker
venv\Scripts\pip install pyinstaller
```

### 1.2 Build single-file exe
```powershell
venv\Scripts\pyinstaller --onefile --name scraper-worker main.py
```

### 1.3 Verify
```powershell
dist\scraper-worker.exe
echo {"jsonrpc":"2.0","id":"1","method":"list_sites","params":{}} | dist\scraper-worker.exe
```

**Output:** `scraper-worker/dist/scraper-worker.exe` (~30-50MB)

**Note:** PyInstaller bundles Python interpreter + all dependencies. For curl-cffi with c extensions, may need `--hidden-import` flags.

### 1.4 Spec file customizations
Create `scraper-worker/scraper-worker.spec`:
```python
# -*- mode: python ; coding: utf-8 -*-
a = Analysis(
    ['main.py'],
    pathex=[],
    binaries=[],
    datas=[],
    hiddenimports=['curl_cffi', 'lxml', 'bs4', 'requests'],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    noarchive=False,
)
pyz = PYZ(a.pure)
exe = EXE(pyz, a.scripts, a.binaries, a.datas, [], name='scraper-worker', debug=False, bootloader_ignore_signals=False, strip=False, upx=True, upx_exclude=[], runtime_tmpdir=None, console=True, disable_windowed_traceback=False, argv_emulation=False, target_arch=None, codesign_identity=None, entitlements_file=None)
```

## Task 2: Kotlin Desktop — MSI Installer

### 2.1 Prerequisites
- WiX Toolset v3.x or v4.x installed
- Added to PATH

```powershell
winget install WiXToolset.WiX
```

### 2.2 Configure build.gradle.kts
Already configured in Phase 2:
```kotlin
compose.desktop {
    application {
        mainClass = "javscraper.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "JavScraper"
            packageVersion = "1.0.0"
            vendor = "JavScraper"
            windows {
                menuGroup = "JavScraper"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            }
        }
    }
}
```

### 2.3 Build MSI
```powershell
cd app
gradle packageMsi
```

**Output:** `app/build/compose/binaries/main/msi/JavScraper-1.0.0.msi`

## Task 3: Merge into Single Installer

### 3.1 Installed Layout
```
%LOCALAPPDATA%\JavScraper\
├── JavScraper.exe          ~25MB (Kotlin Compose)
├── worker\
│   └── scraper-worker.exe  ~50MB (PyInstaller)
├── config.json              (created on first run)
└── logs\
    └── javscraper.log       (created on first run)
```

### 3.2 WiX Custom Action (Bundle)
Create `installer/bundle.wxs`:
```xml
<Wix xmlns="http://schemas.microsoft.com/wix/2006/wi">
  <Bundle Name="JavScraper" Version="1.0.0" Manufacturer="JavScraper" UpgradeCode="a1b2c3d4-e5f6-7890-abcd-ef1234567890">
    <BootstrapperApplicationRef Id="WixStandardBootstrapperApplication.RtfLicense" />
    <Chain>
      <MsiPackage SourceFile="path\to\JavScraper-1.0.0.msi" />
      <ExePackage SourceFile="path\to\scraper-worker.exe" InstallCommand="" />
    </Chain>
  </Bundle>
</Wix>
```

### 3.3 Build pipeline
```powershell
# Step 1: Package Python Worker
cd scraper-worker
pyinstaller --onefile --name scraper-worker main.py

# Step 2: Copy worker into app resources
copy dist\scraper-worker.exe ..\app\src\main\resources\worker\

# Step 3: Build MSI (worker bundled inside)
cd ..\app
gradle packageMsi

# Step 4: (Optional) Create merged bundle with WiX
cd ..\installer
candle bundle.wxs
light bundle.wixobj
```

## Task 4: Code Signing (Optional)

### 4.1 Sign worker exe
```powershell
signtool sign /fd SHA256 /a dist\scraper-worker.exe
```

### 4.2 Sign MSI
```powershell
signtool sign /fd SHA256 /a app\build\compose\binaries\main\msi\JavScraper-1.0.0.msi
```

## Estimated Output Sizes

| Component | Size |
|-----------|------|
| JavScraper.exe | ~25MB |
| scraper-worker.exe | ~50MB |
| Total Installer | ~75-80MB |

## Verification Checklist
- [ ] `scraper-worker.exe` runs standalone and responds to JSON-RPC
- [ ] `JavScraper.msi` installs cleanly on clean Windows
- [ ] After install, app starts and connects to worker
- [ ] Scan → Scrape → NFO + images flow works end-to-end
- [ ] Uninstall removes all files and registry entries