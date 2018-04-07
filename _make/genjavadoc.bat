
set DSTDIR=..\..\
set DST=docuSrcJava_vishiaRun
set DST_priv=docuSrcJavaPriv_vishiaRun

set SRC=-subpackages org.vishia 

set SRCPATH=..;..\..\..\ZBNF\srcJava_vishiaBase

set CLASSPATH=xxxxx

echo set linkpath
set LINKPATH=-link ../../docuSrcJava_vishiaBase

..\..\..\ZBNF\srcJava_vishiaBase\_make\+genjavadocbase.bat

