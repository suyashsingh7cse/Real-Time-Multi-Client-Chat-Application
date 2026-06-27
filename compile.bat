@echo off
REM ============================================================
REM  Build Script (Windows) — Real-Time Chat Application
REM  Developer: Suyash
REM ============================================================

echo ============================================
echo   Building Real-Time Chat Application...
echo ============================================

if exist out rmdir /s /q out
mkdir out

for /r src %%f in (*.java) do (
    javac -d out -sourcepath src "%%f"
)

if %ERRORLEVEL% == 0 (
    echo.
    echo [SUCCESS] Build complete. Classes in: out\
    echo.
    echo   Run Server : java -cp out server.ChatServer
    echo   Run Client : java -cp out client.ChatClient
) else (
    echo [FAILED] Build failed.
)
