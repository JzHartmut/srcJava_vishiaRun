echo off
REM The java-copiler may be located at a user-specified position.
REM Set the environment variable JAVA_HOME, where bin/javac will be found.
if "%JAVA_HOME%" == "" set JAVA_HOME=D:\Progs\JAVA\jdk1.6.0_21
::set PATH=%JAVA_HOME%\bin;%PATH%

REM The TMP_JAVAC is a directory, which contains only this compiling results. It will be clean in the batch processing.
set TMP_JAVAC=..\..\..\tmp_javac

REM Output jar-file with path and filename relative from current dir. It is beside the srcJava-directory:
set OUTPUTFILE_JAVAC=..\..\exe\GenReflectProAgentExplorerXml.jar
if not exist ..\..\exe mkdir ..\..\exe

REM Manifest-file for jar building relativ path from current dir:
set MANIFEST_JAVAC=GenReflectProAgentExplorerXml.manifest

REM Input for javac, only choice of primary sources, relativ path from current (make)-directory:
set INPUT_JAVAC=../org/vishia/inspector/helper/GenReflectProAgentExplorerXml.java

REM Sets the CLASSPATH variable for compilation (used jar-libraries).
REM This component based on the ZBNF and the vishiaRun.
set CLASSPATH_JAVAC=../../zbnfjax/zbnf.jar

REM Sets the src-path for this component, maybe for further necessary sources:
set SRCPATH_JAVAC=..;../../srcJava_Zbnf

REM Call java-compilation and jar with given input environment. This following commands are the same for all java-compilations.
echo on
if exist %TMP_JAVAC% rmdir /S /Q %TMP_JAVAC%
mkdir %TMP_JAVAC%
mkdir %TMP_JAVAC%\bin
%JAVA_HOME%\bin\javac.exe -deprecation -d %TMP_JAVAC%/bin -sourcepath %SRCPATH_JAVAC% -classpath %CLASSPATH_JAVAC% %INPUT_JAVAC% 1>>%TMP_JAVAC%\javac_ok.txt 2>%TMP_JAVAC%\error.txt
echo off
if errorlevel 1 goto :error
echo copiling successfull, generate jar:

set ENTRYDIR=%CD%
cd %TMP_JAVAC%\bin
echo jar -c
%JAVA_HOME%\bin\jar.exe -cvfm %ENTRYDIR%/%OUTPUTFILE_JAVAC% %ENTRYDIR%/%MANIFEST_JAVAC% *  >>../error.txt
if errorlevel 1 goto :error
cd %ENTRYDIR%

pause
goto :ende

:error
  type %TMP_JAVAC%\error.txt
  pause
  goto :ende

:ende
