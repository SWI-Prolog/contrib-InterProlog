. unixVariables.sh
cd ..
rm -r -f htmldocs
mkdir htmldocs
${JAVA_BIN}/javadoc -classpath junit.jar:. -link http://java.sun.com/j2se/1.4.2/docs/api -link http://www.junit.org/junit/javadoc/3.8.1 -public -d htmldocs com.declarativa.interprolog com.declarativa.interprolog.gui com.declarativa.interprolog.util com.xsb.interprolog
