@echo off
REM ===============================================================
REM  JavScraper ? Full Build Pipeline
REM  Packages Python Worker ? Bundles into MSI ? Creates Installer
REM ===============================================================
setlocal enabledelayedexpansion

echo ========================================
echo  JavScraper Build Pipeline v1.0.0
echo ========================================
echo.

REM ---- Config ----
set JAVASCRAPER_ROOT=%~dp0..
set WORKER_DIR=%JAVASCRAPER_ROOT%\scraper-worker
set APP_DIR=%JAVASCRAPER_ROOT%\app
set INSTALLER_DIR=%JAVASCRAPER_ROOT%\installer
set VENV_PYTHON=%WORKER_DIR%\venv\Scripts\python.exe
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot

REM ---- Step 1: Install PyInstaller ----
echo [1/5] Installing PyInstaller...
call "%VENV_PYTHON%" -m pip install pyinstaller
if %ERRORLEVEL% neq 0 (
    echo ERROR: PyInstaller installation failed
    exit /b 1
)
echo OK
echo.

REM ---- Step 2: Build Python Worker EXE ----
echo [2/5] Building Python Worker...
cd /d "%WORKER_DIR%"
call "%VENV_PYTHON%" -m PyInstaller scraper-worker.spec --clean
if %ERRORLEVEL% neq 0 (
    echo ERROR: PyInstaller build failed
    exit /b 1
)
echo OK - Output: %WORKER_DIR%\dist\scraper-worker.exe
echo.

REM ---- Step 3: Copy Worker into App Resources ----
echo [3/5] Copying worker to app resources...
mkdir "%APP_DIR%\src\main\resources\worker" 2>nul
copy /Y "%WORKER_DIR%\dist\scraper-worker.exe" "%APP_DIR%\src\main\resources\worker\"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Copy failed
    exit /b 1
)
echo OK
echo.

REM ---- Step 4: Build Kotlin MSI ----
echo [4/5] Building Kotlin Desktop MSI...
cd /d "%APP_DIR%"
set JAVA_HOME=%JAVA_HOME%
call gradlew packageMsi --no-daemon
if %ERRORLEVEL% neq 0 (
    echo ERROR: MSI build failed
    exit /b 1
)
echo OK - Output: %APP_DIR%\build\compose\binaries\main\msi\JavScraper-1.0.0.msi
echo.

REM ---- Step 5: Create WiX Bundle (optional) ----
echo [5/5] Creating WiX Bundle (if WiX Toolset is available)...
cd /d "%INSTALLER_DIR%"
where candle >nul 2>nul
if %ERRORLEVEL% equ 0 (
    candle bundle.wxs
    if !ERRORLEVEL! equ 0 (
        light bundle.wixobj
        echo OK - Output: %INSTALLER_DIR%\JavScraper-1.0.0.exe
    ) else (
        echo WARNING: candle compilation failed
    )
) else (
    echo SKIP: WiX Toolset not found. Install with:
    echo   winget install WiXToolset.WiX
)
echo.

echo ========================================
echo  Build Pipeline Complete!
echo ========================================
echo  Worker EXE: %WORKER_DIR%\dist\scraper-worker.exe
echo  JavScraper MSI: %APP_DIR%\build\compose\binaries\main\msi\JavScraper-1.0.0.msi
echo ========================================

endlocal
