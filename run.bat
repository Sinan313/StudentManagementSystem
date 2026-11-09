@echo off
rem Compile and launch the Student Management System (Windows).
rem Usage: run.bat          start the app
rem        run.bat test     compile and run the tests
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\sms\*.java test\sms\*.java
if errorlevel 1 exit /b 1
if "%1"=="test" (
    java -cp out sms.StudentRepositoryTest
) else (
    java -cp out sms.StudentManagementApp
)
