@echo off
setlocal

set "JAVAFX_LIB=javafx-sdk-17.0.19\lib"

javac --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.media,javafx.swing -d out *.java
if errorlevel 1 (
    echo.
    echo Compile failed.
    pause
    exit /b 1
)

java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.media,javafx.swing -cp out MainFile
if errorlevel 1 (
    echo.
    echo Launch failed.
    pause
    exit /b 1
)