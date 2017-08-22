REM jzcmd should be a batch file able to found in the system's path.
REM for jzcmd.bat see .../zbnfjax/batch_template/jzcmd.bat.
REM to use this tool the download www.vishia.org/ZBNF/download/.../zbnfjax.zip is necessary.
call jzcmd.bat genXMI_vishiaRun.bat --log=D:/tmp/tmpXml/vishiaRun2Xmi.log --loglevel=333
pause
exit /B

==JZcmd==

##!checkJZcmd=<:><&$TMP>/tmpJZcmd_CHECK_<&scriptfile>.xml<.>;

currdir=<:><&scriptdir><.>;
include $ZBNFJAX_HOME/zmake/Java2Xmi.jzcmd;  ##contains the program to create XMI

Filepath xmldir = D:/tmp/tmpXml;


Fileset src =
( org/vishia/inspectorTarget/*.java        
, org/vishia/inspcPC/*.java          
, org/vishia/inspcPC/accTarget/*.java          
, org/vishia/inspcPC/mng/*.java          
, org/vishia/inspcPC/*.java          
, org/vishia/curves/*.java          
, org/vishia/msgDispatch/*.java          
, org/vishia/reflect/*.java          
, org/vishia/communication/*.java
, ../srcJava_vishiaBase:org/vishia/byteData/*.java          
, ../srcJava_vishiaBase:org/vishia/byteData/reflection_Jc/*.java          
, ../srcJava_vishiaBase:org/vishia/util/*.java          
);



main(){
  
  //zmake $xmldir/*.xml := parseJava2Xml(..&src);

  zmake ../rpy/vishiaRun.xmi := genXMI(..:&src, tmpxml=xmldir);

}

