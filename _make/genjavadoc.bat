
set DSTDIR=..\..\
set DST=docuSrcJava_vishiaRun
set DST_priv=docuSrcJava_vishiaRun_priv

set SRC=-subpackages org.vishia 

set SRCPATH=..;..\..\srcJava_vishiaBase

echo set linkpath
set LINKPATH=-link ../docuSrcJava_vishiaBase

..\..\srcJava_vishiaBase\_make\+genjavadocbase.bat

