@echo off
set currpath=%~dp0e2ewd.jar%*
set cmd=%SISENSE_JAVA_HOME%bin\java.exe
"%cmd%" -jar "%currpath%"