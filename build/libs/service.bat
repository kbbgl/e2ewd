
# https://www.javabullets.com/procrun-java-programs-as-windows-services/

@echo off
setlocal

set SERVICE_NAME=E2EWD

set PR_INSTALL=%~dp0%prunsrv.exe
set PR_DESCRIPTION="E2EWD"

REM Service log configuration
set PR_LOGPREFIX=%SERVICE_NAME%
set PR_LOGPATH=%~dp0%\
set PR_STDOUTPUT=%~dp0%\stdout.txt
set PR_STDERROR=%~dp0%\stderr.txt
set PR_LOGLEVEL=Debug

REM Path to java installation
set PR_JVM=%ProgramFiles%\Sisense\Infra\jre\bin\java.dll
set PR_CLASSPATH=e2ewd.jar

REM Startup configuration
set PR_STARTUP=auto
set PR_STARTMODE=jvm
set PR_STARTCLASS=


