@echo off
echo ==================================================
echo   BUILDING GMAIL CLIENT AND SERVER (JAVA)
echo ==================================================

if not exist bin mkdir bin

set CP=lib\javax.mail-1.6.2.jar;lib\javax.activation-1.2.0.jar;lib\sqlite-jdbc-3.36.0.3.jar;lib\flatlaf-3.4.1.jar

echo Compiling Java source files...
powershell -NoProfile -Command "(Get-ChildItem -Recurse -Filter *.java src).FullName | Out-File -Encoding ASCII sources.txt"
javac -encoding UTF-8 -cp "%CP%" -d bin @sources.txt
if exist sources.txt del sources.txt

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation completed successfully into bin folder!
) else (
    echo [ERROR] Compilation failed. Please check JDK installation.
)
