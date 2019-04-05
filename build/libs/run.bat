@echo off
set configlocation=%~dp0logback.xml%*
set jarlocation=%~dp0e2ewd.jar%*
set cmd=%SISENSE_JAVA_HOME%bin\java.exe
echo "%cmd%" -Dlogback.configurationFile="%configlocation%" -jar "%jarlocation%"
"%cmd%" -Dlogback.configurationFile="%configlocation%" -jar "%jarlocation%"
EXIT /b 0