set INITIAL_DIRECTORY=%~p0
CALL windowsVariables.bat
@echo off
echo ==============================
echo Building Interprolog

@echo off
title %build_title% - compiling .P files
rem - Compile .P files
@echo on
call compile_Ps.bat
rem - terminates in above directory

set build_title=InterProlog Build
title InterProlog Build 
rem - Simple building with JDK, to use only if you do not use an IDE
rem - Note: Needs to specify all of the below files to compile otherwise won't run because .class files won't be included inside the jar file

title %build_title% - creating dir TempCompiled
@echo on
rmdir /S /Q tempCompiled
mkdir tempCompiled

@echo off
title %build_title% - compiling Java files
rem - Compile Java files
@echo on
%JAVA_BIN%\javac -d tempCompiled -classpath .;junit.jar;tempCompiled com\declarativa\interprolog\*.java com\declarativa\interprolog\util\*.java com\declarativa\interprolog\gui\*.java com\declarativa\interprolog\examples\*.java com\xsb\interprolog\*.java


mkdir tempCompiled\com\declarativa\interprolog\xsb
mkdir tempCompiled\com\declarativa\interprolog\swi
mkdir tempCompiled\com\declarativa\interprolog\yap
mkdir tempCompiled\com\declarativa\interprolog\gui\images

copy com\declarativa\interprolog\yap\*.yap tempCompiled\com\declarativa\interprolog\yap
copy com\declarativa\interprolog\xsb\*.xwam tempCompiled\com\declarativa\interprolog\xsb
copy com\declarativa\interprolog\swi\*.pl tempCompiled\com\declarativa\interprolog\swi
copy com\declarativa\interprolog\gui\*.xwam tempCompiled\com\declarativa\interprolog\gui
copy com\declarativa\interprolog\gui\*.P tempCompiled\com\declarativa\interprolog\gui
copy com\declarativa\interprolog\gui\images\* tempCompiled\com\declarativa\interprolog\gui\images
copy com\declarativa\interprolog\*.P tempCompiled\com\declarativa\interprolog
copy com\declarativa\interprolog\examples\*.P tempCompiled\com\declarativa\interprolog\examples

cd tempCompiled 

@echo off
title %build_title% - deleting interprolog.jar Jar file
rem - delete interprolog.jar Jar file
@echo on

del ..\interprolog.jar

@echo off
title %build_title% - creating interprolog.jar Jar file
rem - Create interprolog.jar file
@echo on
%JAVA_BIN%\jar cf ..\interprolog.jar *

cd ..
rmdir /S /Q tempCompiled

@echo off
title %build_title% - Finished
cd %INITIAL_DIRECTORY%


