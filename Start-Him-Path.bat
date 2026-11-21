@echo off
setlocal
cd /d "%~dp0"

rem Reuse the running server when Start is clicked again.
powershell -NoProfile -Command "try { $r=Invoke-WebRequest -UseBasicParsing http://localhost:8080 -TimeoutSec 1; if($r.StatusCode -eq 200){exit 0} } catch {}; exit 1"
if not errorlevel 1 (
  start "" "http://localhost:8080"
  exit /b 0
)

set "JAVA_HOME="
if defined JDK_HOME if exist "%JDK_HOME%\bin\javac.exe" set "JAVA_HOME=%JDK_HOME%"
if not defined JAVA_HOME for /d %%J in ("%~dp0.runtime\jdk-17*") do if exist "%%~fJ\bin\javac.exe" set "JAVA_HOME=%%~fJ"
if not defined JAVA_HOME for /d %%J in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*") do if exist "%%~fJ\bin\javac.exe" set "JAVA_HOME=%%~fJ"
if not defined JAVA_HOME (
  echo Him-Path needs Java 17. The portable runtime should be in the .runtime folder.
  pause
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

set "MAVEN_CMD=%~dp0tools\apache-maven-3.9.16\bin\mvn.cmd"
if not exist "%MAVEN_CMD%" (
  echo The bundled Maven launcher was not found at:
  echo %MAVEN_CMD%
  pause
  exit /b 1
)

echo Starting Him-Path with its local Java 17 runtime and MariaDB database...
start "Him-Path Server" /D "%~dp0" cmd /k ""%~dp0Run-Him-Path-Server.bat""
echo Waiting for the web app...
powershell -NoProfile -Command "$ready=$false; for($i=0;$i -lt 120;$i++){try{$r=Invoke-WebRequest -UseBasicParsing http://localhost:8080 -TimeoutSec 1;if($r.StatusCode -eq 200){$ready=$true;break}}catch{};Start-Sleep -Seconds 1}; if($ready){exit 0}; exit 1"
if errorlevel 1 (
  echo Him-Path did not start. Check the Him-Path Server window.
  pause
  exit /b 1
)
start "" "http://localhost:8080"
