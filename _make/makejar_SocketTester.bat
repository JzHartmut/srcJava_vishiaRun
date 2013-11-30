echo off
call setJAVA_JDK6.bat



REM The TMP_JAVAC is a directory, which contains only this compiling results. It will be clean in the batch processing.
set TMP_JAVAC=..\..\tmp_javac

REM Output jar-file with path and filename relative from current dir:
set OUTDIR_JAVAC=..\..\exe
set JAR_JAVAC=SocketTester.jar

REM Manifest-file for jar building relativ path from current dir:
set MANIFEST_JAVAC=SocketTester.manifest

REM Input for javac, only choice of primary sources, relativ path from current (make)-directory:
set INPUT_JAVAC=../org/vishia/communication/SocketTester.java

set CLASSPATH_JAVAC=x

REM Sets the src-path for this component, maybe for further necessary sources:
::set SRCLIB=../../../AAXDWP
set SRCLIB=d:\vishia\Java
set SRCPATH_JAVAC=..;%SRCLIB%/srcJava_vishiaBase;%SRCLIB%/srcJava_Zbnf;%SRCLIB%/srcJava_vishiaGUI;%SRCLIB%/srcJava_vishiaRun;


%SRCLIB%\srcJava_vishiaBase\_make\+javacjarbase.bat


