@echo off
set "BUNDLE_HOME=%~dp0.."
"%BUNDLE_HOME%\runtime\bin\java.exe" ^
    -Xmx4g -Duser.language=en -Dfile.encoding=UTF-8 ^
    -jar "%BUNDLE_HOME%\app\DensiTree.jar" %*
