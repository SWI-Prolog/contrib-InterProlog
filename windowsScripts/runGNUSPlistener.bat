echo off
CALL windowsVariables.bat
%JAVA_BIN%\java -classpath ..\interprolog.jar com.declarativa.interprolog.gui.GNUSubprocessEngineWindow %1 %GNU_BIN_DIRECTORY%\gprolog
echo on

