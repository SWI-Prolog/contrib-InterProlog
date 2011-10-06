. unixVariables.sh
${JAVA_BIN}/java -Djava.library.path=${XSB_BIN_DIRECTORY} -classpath ${CLASSPATH}:../interprolog.jar com.xsb.interprolog.NativeEngineWindow $1 ${XSB_BIN_DIRECTORY}
