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
        'core.smart_search',
        'core.webpage_archive',
        'scrapers.site_check',
        'scrapers.openaver.censored.javbus',
        'scrapers.openaver.censored.javdb',
        'scrapers.openaver.censored.javdb_extra_parsing',
        'scrapers.openaver.censored.javdb_parsing',
        'scrapers.openaver.censored.jav321',
        'scrapers.openaver.uncensored.fc2',
        'scrapers.openaver.uncensored.fc2_mirror',
        'scrapers.openaver.uncensored.heyzo',
        'scrapers.openaver.uncensored.avsox',
        'scrapers.openaver.uncensored.d2pass',
        'scrapers.openaver.censored.dmm',
        'scrapers.openaver.censored.javlibrary',
        'scrapers.openaver.mixed.mmtv',
        'scrapers.openaver.domestic.madouqu',
        'scrapers.openaver.domestic.mdtv',
        'scrapers.openaver.domestic.hdouban',
        'scrapers.openaver.domestic.cnmdb',
        'scrapers.openaver.domestic.javday',
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
