# -*- mode: python ; coding: utf-8 -*-

"""
JavScraper Python Worker - PyInstaller spec
Packages the worker as a single-file executable.
"""

import os
import sys

block_cipher = None

# In spec files, __file__ is not available.
# SPECPATH and os.getcwd() point to the spec file's directory.
spec_dir = os.path.dirname(os.path.abspath(SPECPATH)) if 'SPECPATH' in dir() else os.getcwd()

a = Analysis(
    ['main.py'],
    pathex=[spec_dir],
    binaries=[],
    datas=[],
    hiddenimports=[
        'ipc_handler',
        'scrapers.registry',
        'scrapers.base',
        'scrapers.models',
        'core.file_prober',
        'core.smart_search',
        'scrapers.site_check',
        'scrapers.openaver.javbus',
        'scrapers.openaver.javdb',
        'scrapers.openaver.jav321',
        'scrapers.openaver.fc2',
        'scrapers.openaver.heyzo',
        'scrapers.openaver.avsox',
        'scrapers.openaver.d2pass',
        'scrapers.openaver.dmm',
        'scrapers.openaver.javlibrary',
        'scrapers.openaver.mmtv',
        'curl_cffi',
        'lxml',
        'lxml._elementpath',
        'bs4',
        'bs4.builder._lxml',
        'requests',
        'httpx',
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[
        'tkinter',
        'matplotlib',
        'numpy',
        'scipy',
        'PIL',
        'PyQt5',
        'notebook',
        'ipython',
        'jupyter',
    ],
    noarchive=False,
)

pyz = PYZ(a.pure, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.datas,
    [],
    name='scraper-worker',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=True,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)
