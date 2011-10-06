echo off
CALL windowsVariables.bat
%JAVA_BIN%\java -classpath ..\interprolog.jar com.declarativa.interprolog.gui.XSBSubprocessEngineWindow %1 %XSB_BIN_DIRECTORY%\xsb
rem pause
echo on
