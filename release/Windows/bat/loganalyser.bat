@echo off
set "BUNDLE_HOME=%~dp0.."
"%BUNDLE_HOME%\runtime\bin\java.exe" ^
    --module-path "%BUNDLE_HOME%\app" ^
    --add-modules ALL-MODULE-PATH,javafx.controls,javafx.fxml,javafx.swing,javafx.web,jdk.jsobject ^
    -Xss256m -Xmx8g -Duser.language=en -Dfile.encoding=UTF-8 ^
    -m beast.pkgmgmt/beast.pkgmgmt.launcher.AppLauncherLauncher beastfx.app.tools.LogAnalyser %*