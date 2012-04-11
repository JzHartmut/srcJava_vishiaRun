#!/bin/bash

## The TMP_JAVAC is a directory, which contains only this compiling results. It will be clean in the batch processing.
export TMP_JAVAC="../../../tmp_javac"

## Output jar-file with path and filename relative from current dir:
export OUTDIR_JAVAC="../../exe"
export JAR_JAVAC="vishiaRun.jar"

## Manifest-file for jar building relativ path from current dir:
export MANIFEST_JAVAC="vishiaRun.manifest"

## Input for javac, only choice of primary sources, relativ path from current (make)-directory:
INPUT_JAVAC=""
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/communication/*.java"
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/inspector/*.java"
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/inspector/example/*.java"
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/inspectorAccessor/*.java"
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/msgDispatch/*.java"
INPUT_JAVAC="$INPUT_JAVAC ../org/vishia/reflect/*.java"
export INPUT_JAVAC

## Sets the CLASSPATH variable for compilation (used jar-libraries). Do not leaf empty also it aren't needed because it is used in command line.
##This component depends on the following component:
export CLASSPATH_JAVAC="../../exe/vishiaBase.jar"

## Sets the src-path for further necessary sources. It is always .. because the make directory is parallel to the java sources of this component.
export SRCPATH_JAVAC=".."

##call of javac and jar with preparing directories etc.
../../srcJava_vishiaBase/_make/+javacjarbase.sh
