. unixVariables.sh
${JAVA_BIN}/java  -Djava.library.path=${XSB_BIN_DIRECTORY} -classpath ${CLASSPATH}:../junit.jar:../interprolog.jar -DXSB_BIN_DIRECTORY=${XSB_BIN_DIRECTORY} -DSWI_BIN_DIRECTORY=${SWI_BIN_DIRECTORY} -DYAP_BIN_DIRECTORY=${YAP_BIN_DIRECTORY} com.declarativa.interprolog.AllTests
