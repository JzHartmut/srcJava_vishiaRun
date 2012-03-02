##--------------------------------------------------------------------------------------------
## Environment variables set from zbnfjax:
## JAVA_JDK: Directory where bin/javac is found. This java version will taken for compilation
## The java-copiler may be located at a user-specified position.
## Set the environment variable JAVA_HOME, where bin/javac will be found.

export DST="../../docuSrcJava_vishiaRun"
export DST_priv="../../docuSrcJava_vishiaRun_priv"

export SRC="-subpackages org.vishia.inspector"
#export SRC="$SRC  org.vishia.msgDispatch"
export SRC="$SRC  org.vishia.communication"
export SRC="$SRC  org.vishia.inspectorAccessor"
export SRC="$SRC  org.vishia.reflect"
export SRC="$SRC  ../org/vishia/msgDispatch/*.java"

export SRCPATH="..:../../srcJava_vishiaBase"
export LINKPATH="-link ../docuSrcJava_vishiaBase"

../../srcJava_vishiaBase/_make/+genjavadocbase.sh
