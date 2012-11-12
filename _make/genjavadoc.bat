
set DSTDIR=..\..\
set DST=docuSrcJava_vishiaRun
set DST_priv=docuSrcJava_vishiaRun_priv

echo set SRC
set SRC=-subpackages org.vishia.inspector 
set SRC=%SRC% org.vishia.communication
set SRC=%SRC% org.vishia.inspectorAccessor
set SRC=%SRC% org.vishia.reflect
set SRC=%SRC% ../org/vishia/msgDispatch/*.java

set SRCPATH=..;..\..\srcJava_vishiaBase

echo set linkpath
set LINKPATH=-link ../docuSrcJava_vishiaBase

..\..\srcJava_vishiaBase\_make\+genjavadocbase.bat
