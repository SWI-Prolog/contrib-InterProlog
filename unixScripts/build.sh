. unixVariables.sh
./compile_Ps.sh ${XSB_BIN_DIRECTORY}
cd ..
rm -r -f tempCompiled
mkdir tempCompiled
${JAVA_BIN}/javac -d tempCompiled -classpath .:tempCompiled:junit.jar com/xsb/interprolog/*.java com/declarativa/interprolog/*.java com/declarativa/interprolog/util/*.java com/declarativa/interprolog/gui/*.java com/declarativa/interprolog/examples/*.java
mkdir -p tempCompiled/com/declarativa/interprolog/gui/images
mkdir -p tempCompiled/com/declarativa/interprolog/swi
mkdir -p tempCompiled/com/declarativa/interprolog/xsb
cp com/declarativa/interprolog/*.P tempCompiled/com/declarativa/interprolog
cp com/declarativa/interprolog/xsb/*.xwam tempCompiled/com/declarativa/interprolog/xsb
cp com/declarativa/interprolog/swi/*.pl tempCompiled/com/declarativa/interprolog/swi
cp com/declarativa/interprolog/gui/*.xwam tempCompiled/com/declarativa/interprolog/gui
cp com/declarativa/interprolog/gui/*.P tempCompiled/com/declarativa/interprolog/gui
cp com/declarativa/interprolog/examples/*.P tempCompiled/com/declarativa/interprolog/examples
cp com/declarativa/interprolog/gui/images/* tempCompiled/com/declarativa/interprolog/gui/images
cd tempCompiled
rm ../interprolog.jar
${JAVA_BIN}/jar cf ../interprolog.jar *
cd ..
rm -r -f tempCompiled
