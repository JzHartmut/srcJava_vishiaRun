#export DSTDIR=D:/vishia/Java/
export DSTDIR=$TMP/_Javadoc/
mkdir $DSTDIR

if ! test -d $DSTDIR; then export DSTDIR=../../; fi
echo %DSTDIR%
export DST=docuSrcJava_vishiaRun
export DST_priv=docuSrcJavaPriv_vishiaRun

export SRC="-subpackages org.vishia"
export SRCPATH=..
export CLASSPATH=xxxxx
#export LINKPATH=
#export CLASSPATH=xxxxx
export LINKPATH="-link ../docuSrcJava_vishiaBase"

if test -d ../../srcJava_vishiaBase; then export vishiaBase="../../srcJava_vishiaBase"
else export vishiaBase="../../../../../../cmpnJava_vishiaBase/src/main/java/srcJava_vishiaBase"
fi

$vishiaBase/_make/-genjavadocbase.sh


