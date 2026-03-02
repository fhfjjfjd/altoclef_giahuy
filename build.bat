@echo off
rem AltoClef Huy Edition - Build Script (Windows)

echo ==================================
echo   AltoClef Huy Edition - Builder
echo ==================================
echo.

rem Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install Java 17+.
    pause
    exit /b 1
)

for /f "tokens=3" %%a in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%a
echo [INFO] Java version: %JAVA_VER%

rem Build
echo [INFO] Building project...
call gradlew.bat build
if errorlevel 1 (
    echo.
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

echo.
echo ==================================
echo   Build complete!
echo ==================================
echo.

rem Show output
for %%f in (build\libs\*.jar) do (
    echo [OUTPUT] %%f
)
echo.
echo Copy the JAR to .minecraft\mods\ to use.
pause
