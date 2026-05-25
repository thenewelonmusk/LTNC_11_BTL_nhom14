@echo off
REM Test Connection Pool Fix - Auction System

echo ========================================
echo Auction System - Connection Pool Test
echo ========================================
echo.

REM Set paths
set PROJECT_PATH=c:\Users\ADMIN\Downloads\LTNC_11_BTL_nhom141
set JAVA_HOME=C:\Users\ADMIN\.jdks\openjdk-25.0.2
set PATH=%JAVA_HOME%\bin;%PATH%

REM Change to project directory
cd /d %PROJECT_PATH%

REM Check if build exists
if not exist "target\classes" (
    echo Build not found. Building project...
    mvn clean package -DskipTests
    if %ERRORLEVEL% NEQ 0 (
        echo Build failed!
        pause
        exit /b 1
    )
)

echo.
echo ========================================
echo Starting AuctionServer...
echo ========================================
echo.
echo Expected behavior:
echo - Status monitor prints every 1 second
echo - No "Connection timeout" errors
echo - Pool shows: total=10, active=[variable], idle=[variable]
echo.

REM Run server
java -cp "target\classes;target\dependency\*" com.auction.server.AuctionServer

pause
