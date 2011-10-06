echo off
REM Sample script to launch a Java class using Interprolog, 
REM assuming Prolog engine paths remain defined only in environment variables

CALL windowsVariables.bat
%JAVA_BIN%\java -Djava.library.path=%XSB_BIN_DIRECTORY% -DXSB_BIN_DIRECTORY=%XSB_BIN_DIRECTORY% -DSWI_BIN_DIRECTORY=%SWI_BIN_DIRECTORY% -DYAP_BIN_DIRECTORY=%YAP_BIN_DIRECTORY% -classpath ..\junit.jar;..\interprolog.jar %1
echo on