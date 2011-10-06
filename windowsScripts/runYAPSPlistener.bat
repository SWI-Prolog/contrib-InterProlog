echo off
CALL windowsVariables.bat
%JAVA_BIN%\java -classpath ..\interprolog.jar com.declarativa.interprolog.gui.YAPSubprocessEngineWindow %1 %YAP_BIN_DIRECTORY%\yap
rem pause
echo on
