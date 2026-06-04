#!/usr/bin/env python3
"""
Patch a jpackage-generated BEAST.cfg from classpath mode to module-path mode.

Usage:
    python3 release/patch-beast-cfg.py <path/to/BEAST.cfg>

jpackage writes classpath-mode config when --main-jar is used:
    app.classpath = <all jars>
    app.mainclass = beast.pkgmgmt.launcher.BeastLauncher

This script converts it to module mode so that module descriptors
(provides/requires) are visible at runtime and external BEAST packages
loaded via ModuleLayer resolve correctly:
    app.mainmodule = beast.pkgmgmt/beast.pkgmgmt.launcher.BeastLauncher
    [JavaOptions]
    java-options=--module-path=$APPDIR
    java-options=--add-modules=ALL-MODULE-PATH

$APPDIR is substituted by the jpackage native launcher with the absolute
path to the app/ directory at runtime.

Platform-specific BEAST.cfg paths:
    Linux   — <dist>/BEAST/lib/app/BEAST.cfg
    Windows — <dist>/BEAST/app/BEAST.cfg
    macOS   — <dist>/BEAST.app/Contents/app/BEAST.cfg
"""
import sys
import re

cfg = sys.argv[1]

with open(cfg, encoding='utf-8') as f:
    txt = f.read()

txt = re.sub(r'(?m)^app\.classpath[^\n]*\n?', '', txt)
txt = re.sub(r'(?m)^app\.mainclass=.*',
             'app.mainmodule=beast.pkgmgmt/beast.pkgmgmt.launcher.BeastLauncher', txt)

ins = ('java-options=--module-path=$APPDIR\n'
       'java-options=--add-modules=ALL-MODULE-PATH\n')

if '[JavaOptions]' in txt:
    txt = re.sub(r'(?m)^\[JavaOptions\]\n', '[JavaOptions]\n' + ins, txt, count=1)
else:
    txt += '\n[JavaOptions]\n' + ins

with open(cfg, 'w', encoding='utf-8') as f:
    f.write(txt)

print(f'Patched {cfg}')
