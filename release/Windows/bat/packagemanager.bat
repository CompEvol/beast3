@echo off
set "BUNDLE_HOME=%~dp0.."
"%BUNDLE_HOME%\runtime\bin\java.exe" ^
    --module-path "%BUNDLE_HOME%\app" ^
    -cp "%BUNDLE_HOME%\app\resolver\*" --add-reads beast.pkgmgmt=ALL-UNNAMED ^
    --add-modules ALL-MODULE-PATH,java.compiler,javafx.controls,javafx.fxml,javafx.swing,javafx.web,jdk.jsobject --enable-native-access=javafx.graphics,javafx.media,javafx.web ^
    -Xss256m -Xmx8g -Duser.language=en -Dfile.encoding=UTF-8 ^
    -m beast.pkgmgmt/beast.pkgmgmt.PackageManager %*