@echo off
echo ==============================
echo Compiling Interprolog Prolog files.
CALL windowsVariables.bat
cd ..
echo ================= =============
echo Compiling interprolog.P
%XSB_BIN_DIRECTORY%\xsb.exe --noprompt --quiteload --nobanner -e "compile('com/declarativa/interprolog/xsb/interprolog.P'),halt."
if errorlevel 1 goto interprologerror

echo ==============================
echo Compiling visualization.P
%XSB_BIN_DIRECTORY%\xsb.exe --noprompt --quiteload --nobanner -e "compile('com/declarativa/interprolog/gui/visualization.P'),halt."
if errorlevel 1 goto visualerror

echo ==============================
echo Compiling tests.P
%XSB_BIN_DIRECTORY%\xsb.exe --noprompt --quiteload --nobanner -e "compile('com/declarativa/interprolog/tests.P'),halt."
if errorlevel 1 goto testerror
goto end

:interprologerror
echo ERROR: Unable to compile interprolog.P
goto end

:visualerror
echo ERROR: Unable to compile visualization.P
goto end

:testerror
echo ERROR: Unable to compile tests.P
goto end

:end
echo Done