@echo off
REM ─────────────────────────────────────────────────────────────
REM build.bat — One-command compile script (Windows)
REM Usage: build.bat
REM ─────────────────────────────────────────────────────────────

echo ================================
echo   Building Real-Time Chat App
echo ================================

if exist out rmdir /s /q out
mkdir out

for /R src %%f in (*.java) do set SOURCES=!SOURCES! "%%f"

javac -d out --release 17 ^
  src\util\Constants.java ^
  src\util\DateUtil.java ^
  src\util\LoggerUtil.java ^
  src\model\User.java ^
  src\model\Message.java ^
  src\model\ChatRoom.java ^
  src\service\AuthenticationService.java ^
  src\service\HistoryService.java ^
  src\service\ChatService.java ^
  src\service\RoomService.java ^
  src\server\ServerDashboard.java ^
  src\server\ClientHandler.java ^
  src\server\ChatServer.java ^
  src\client\ClientReader.java ^
  src\client\ClientWriter.java ^
  src\client\ChatClient.java

if %errorlevel% == 0 (
  echo   Build successful -^> out\
  echo ================================
  echo.
  echo   Run server:  java -cp out server.ChatServer
  echo   Run client:  java -cp out client.ChatClient
) else (
  echo   BUILD FAILED
)
