@echo off
setlocal
rem ---- Builds the mod on Windows. Needs JDK 25 installed (https://adoptium.net) ----
set GRADLE_VERSION=9.2.0

java -version >nul 2>&1 || (echo Java not found. Install JDK 25 from https://adoptium.net and try again. & pause & exit /b 1)

if not exist ".gradle-dist\gradle-%GRADLE_VERSION%\bin\gradle.bat" (
  echo Downloading Gradle %GRADLE_VERSION%...
  mkdir .gradle-dist 2>nul
  powershell -NoProfile -Command "Invoke-WebRequest https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip -OutFile .gradle-dist\gradle.zip; Expand-Archive .gradle-dist\gradle.zip .gradle-dist -Force"
)

call ".gradle-dist\gradle-%GRADLE_VERSION%\bin\gradle.bat" build
echo.
echo Done. Your jar is in build\libs (use the one WITHOUT "-sources" in the name).
pause
