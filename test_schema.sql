create vertex type IndirectObject if not exists;
alter type IndirectObject custom javaClass='test.IndirectObject';
create property IndirectObject.testData if not exists STRING;


create vertex type SimpleVertex if not exists;
alter type SimpleVertex custom javaClass='test.SimpleVertex';

create property SimpleVertex.uuid if not exists STRING;
create property SimpleVertex.s  if not exists  STRING;
create property SimpleVertex.i  if not exists  INTEGER;
create property SimpleVertex.f  if not exists FLOAT;
create property SimpleVertex.b  if not exists BOOLEAN;
create property SimpleVertex.fecha  if not exists  DATETIME;
create property SimpleVertex.serial  if not exists LONG;
create property SimpleVertex.oI  if not exists INTEGER;
create property SimpleVertex.oF  if not exists FLOAT;
create property SimpleVertex.oB  if not exists  BOOLEAN;

create edge type EdgeAttrib if not exists;
alter type EdgeAttrib custom javaClass='test.EdgeAttrib';
create property EdgeAttrib.uuid if not exists STRING;
create property EdgeAttrib.nota  if not exists STRING;
create property EdgeAttrib.fecha  if not exists DATETIME;
create property EdgeAttrib.enumValue  if not exists string;
 
create vertex type IndirectObject  if not exists;
alter type IndirectObject custom javaClass='test.IndirectObject';
create property IndirectObject.testData  if not exists STRING;

create edge type IndirectObject_directLink  if not exists;

create edge type IndirectObject_alDirectLinked  if not exists;

create edge type IndirectObject_hmDirectLinked  if not exists;
 
create edge type IndirectObject_indirectLink  if not exists;
 
create edge type IndirectObject_indirectLinkedFromAL  if not exists;
 
create edge type IndirectObject_alIndirectLinked  if not exists;
 
create edge type IndirectObject_hmIndirectLinked  if not exists;
 
create vertex type UserSID if not exists;
alter type UserSID custom javaClass='net.odbogm.security.UserSID';
create property UserSID.name if not exists STRING;
create index if not exists on UserSID (name) UNIQUE;
create property UserSID.uuid  if not exists STRING;
create index if not exists on UserSID(uuid) UNIQUE;

create edge type UserSID_groups  if not exists;

create vertex type GroupSID  if not exists;
alter type GroupSID custom javaClass='net.odbogm.security.GroupSID';
create property GroupSID.name  if not exists STRING;
create index if not exists on GroupSID(name) UNIQUE;
create property GroupSID.uuid  if not exists STRING;
create index if not exists on GroupSID(uuid) UNIQUE;
 

create edge type GroupSID_participants  if not exists;
 
create edge type GroupSID_addedTo  if not exists;
 
create vertex type SObject  if not exists ;
alter type SObject custom javaClass='net.odbogm.security.SObject';

create edge type SObject___owner  if not exists;
 
create edge type SObject___acl  if not exists ;

create edge type SObject___inherit  if not exists;
 
create vertex type SSimpleVertex if not exists extends SObject;
alter type SSimpleVertex custom javaClass='test.SSimpleVertex';
create property SSimpleVertex.s if not exists STRING;

create edge type SSimpleVertex___owner if not exists extends SObject___owner;
 
create edge type SSimpleVertex___acl if not exists extends SObject___acl;

create edge type SSimpleVertex___inherit if not exists extends SObject___inherit;
 
create vertex type SimpleVertex if not exists;
alter type SimpleVertex custom javaClass='test.SimpleVertex';
create property SimpleVertex.uuid if not exists STRING;
create property SimpleVertex.s if not exists STRING;
create property SimpleVertex.i if not exists INTEGER;
create property SimpleVertex.f if not exists FLOAT;
create property SimpleVertex.b if not exists BOOLEAN;
create property SimpleVertex.fecha if not exists DATETIME;
create property SimpleVertex.serial if not exists LONG;
create property SimpleVertex.oI if not exists INTEGER;
create property SimpleVertex.oF if not exists FLOAT;
create property SimpleVertex.oB if not exists BOOLEAN;

create vertex type SimpleVertexInterfaceAttr if not exists extends SimpleVertex;
alter type SimpleVertexInterfaceAttr custom javaClass='test.SimpleVertexInterfaceAttr';

create edge type SimpleVertexInterfaceAttr_itest if not exists;

create edge type SimpleVertexInterfaceAttr_iList if not exists;
 

create vertex type SimpleVertexWithEmbedded if not exists ;
alter type SimpleVertexWithEmbedded custom javaClass='test.SimpleVertexWithEmbedded';

create edge type SimpleVertexWithEmbedded_stringlist if not exists ;

create edge type SimpleVertexWithEmbedded_simplemap if not exists ;
 
create vertex type SimpleVertexWithImplement if not exists extends SimpleVertex;
alter type SimpleVertexWithImplement custom javaClass='test.SimpleVertexWithImplement';

create vertex type FooNode if not exists ;
alter type FooNode custom javaClass='test.Foo';

create property FooNode.text if not exists STRING;
 
create edge type FooNode_lsve if not exists ;
 
create vertex type Enums if not exists ;
alter type Enums custom javaClass='test.Enums';
create property Enums.description if not exists STRING;
create property Enums.theEnum if not exists string;
create property Enums.enums if not exists list;
 
create edge type Enums_enumToString if not exists ;
 
create edge type Enums_stringToEnum if not exists ;
 
create vertex type SimpleVertexEx extends SimpleVertex;
alter type SimpleVertexEx custom javaClass='test.SimpleVertexEx';
create property SimpleVertexEx.svex if not exists STRING (mandatory true);
create property SimpleVertexEx.enumTest if not exists string;
create property SimpleVertexEx.svuuid if not exists STRING;

create edge type SimpleVertexEx_looptest if not exists ;

create edge type SimpleVertexEx_eagerTest if not exists ;

create index on SimpleVertexEx(svuuid) UNIQUE;
 
create edge type SimpleVertexEx_svinner if not exists ;

create edge type SimpleVertexEx_alString if not exists ;
 
create edge type SimpleVertexEx_hmString if not exists ;
 
create edge type SimpleVertexEx_alSV if not exists ;
 
create edge type SimpleVertexEx_lSV if not exists ;
 
create edge type SimpleVertexEx_alSVE if not exists ;
 
create edge type SimpleVertexEx_hmSV if not exists ;
 
create edge type SimpleVertexEx_hmSVE if not exists ;
 
create edge type SimpleVertexEx_ohmSVE if not exists extends EdgeAttrib;
 
create vertex type SVExChild extends SimpleVertexEx;
alter type SVExChild custom javaClass='test.SVExChild';

create edge type SVExChild_looptest if not exists extends SimpleVertexEx_looptest;
 
create edge type SVExChild_svinner if not exists extends SimpleVertexEx_svinner;

create edge type SVExChild_alString if not exists extends SimpleVertexEx_alString;

create edge type SVExChild_hmString if not exists extends SimpleVertexEx_hmString;
 
create edge type SVExChild_alSV extends SimpleVertexEx_alSV;

create edge type SVExChild_lSV extends SimpleVertexEx_lSV;

create edge type SVExChild_alSVE if not exists extends SimpleVertexEx_alSVE;

create edge type SVExChild_hmSV if not exists extends SimpleVertexEx_hmSV;
 
create edge type SVExChild_hmSVE if not exists extends SimpleVertexEx_hmSVE;
 
create edge type SVExChild_ohmSVE if not exists extends EdgeAttrib;

create vertex type Serial if not exists ;
alter type Serial custom javaClass='test.Serial';
create property Serial.s1 if not exists LONG;
create property Serial.s2 if not exists LONG;


create vertex type Secure if not exists extends SObject;
alter type Secure custom javaClass='test.Secure';
create property Secure.s if not exists STRING;

create edge type Secure___owner if not exists extends SObject___owner;

create edge type Secure_subs if not exists ;
 
create edge type Secure___inherit if not exists extends SObject___inherit;
 
 
create vertex type SubSecure if not exists ;
alter type SubSecure custom javaClass='test.SubSecure';

create edge type SubSecure_aList if not exists ;

