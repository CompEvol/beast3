@echo off

REM Check whether the JRE is included
IF EXIST %~dp0\..\jre (

REM for BEAST version that includes JRE
    "%~dp0\..\jre\bin\java" -Xmx4g -Duser.language=en -Dfile.encoding=UTF-8 -jar "%~dp0\..\DensiTree.jar" %*

) ELSE (
REM for version that does not include JRE
    java -Xmx4g -Duser.language=en -Dfile.encoding=UTF-8 -jar "%~dp0\..\DensiTree.jar" %*
)
