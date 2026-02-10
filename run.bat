@echo off
echo BNM Tender System - Windows Launcher
echo ====================================

echo [1/3] Cleaning and Building Project...
call mvn clean package
if %errorlevel% neq 0 (
    echo Build failed!
    pause
    exit /b %errorlevel%
)

echo [2/3] Copying Dependencies...
call mvn dependency:copy-dependencies
if %errorlevel% neq 0 (
    echo Dependency copy failed!
    pause
    exit /b %errorlevel%
)

echo [3/3] Running Application...
echo.
java -cp "target/bnm-tender-system-1.0-SNAPSHOT.jar;target/dependency/*" com.bnm.tender.Main

pause
