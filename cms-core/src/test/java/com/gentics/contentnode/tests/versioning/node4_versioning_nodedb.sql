delete from contentrepository;
delete from tagmap;
delete from contentfile;
delete from contentset;
delete from contenttag;
delete from folder;
delete from node;
delete from page;
delete from systemuser;
delete from usergroup;
delete from contenttag_nodeversion;
delete from ds;
delete from ds_nodeversion;
delete from ds_obj;
delete from ds_obj_nodeversion;
delete from node_contentgroup;
delete from objprop;
delete from objprop_category;
delete from objtag;
delete from page_nodeversion;
delete from template;
delete from template_folder;
delete from templategroup;
delete from templatetag;
delete from publish;
delete from systemsession;
delete from value_nodeversion;
delete from value where templatetag_id != 0 or contenttag_id != 0 or objtag_id != 0;
delete from value where templatetag_id != 0 or contenttag_id != 0 or objtag_id != 0;
update construct set creator = 1 where creator = 15;
delete from dicuser where output_id in (793,1029,1341,1397,1398,1399,1401,1407,1411,1416,1417,1418,1419,1426,1433,1434,1435,1437,1542,1543,1544,1545,1548,1551,1552,1553);
delete from tagmap;

UPDATE `value` set value_text='velocity' where id = 25899;

INSERT INTO contentrepository (id, name, dbtype, username, password, url) VALUES
 (1, 'Migrated from Configuration file', 'mysql', 'root', '', 'jdbc:mariadb://dev6.office:3305/objectdelete_cr_tests_autocreate?characterEncoding=UTF8');

INSERT INTO `tagmap` (`tagname`, `mapname`, `searchname`, `object`, `objtype`, `attributetype`, `multivalue`, `static`, `optimized`, `foreignlinkattribute`,  `foreignlinkattributerule`, `contentrepository_id`) VALUES
 ('file.description','description','',10008,0,1,0,1,0,'','',1),
 ('file.creator','creator','',10008,0,1,0,1,0,'','',1),
 ('file.createtimestamp','createtimestamp','',10008,0,3,0,1,0,'','',1),
 ('file.creator.email','creatoremail','',10008,0,1,0,1,0,'','',1),
 ('file.editor','editor','',10008,0,1,0,1,0,'','',1),
 ('file.edittimestamp','edittimestamp','',10008,0,3,0,1,0,'','',1),
 ('file.editor.email','editoremail','',10008,0,1,0,1,0,'','',1),
 ('','obj_type','',10008,0,3,0,1,0,'','',1),
 ('','contentid','',10008,0,1,0,1,0,'','',1),
 ('file.url','url','',10008,0,1,0,1,0,'','',1),
 ('folder.id','folder_id','',10008,10002,2,0,1,1,'','',1),
 ('folder.name','name','',10002,0,1,0,1,1,'','',1),
 ('folder.description','description','',10002,0,1,0,1,0,'','',1),
 ('folder.creator','creator','',10002,0,1,0,1,0,'','',1),
 ('folder.creationtimestamp','createtimestamp','',10002,0,3,0,1,0,'','',1),
 ('folder.creator.email','creatoremail','',10002,0,1,0,1,0,'','',1),
 ('folder.editor','editor','',10002,0,1,0,1,0,'','',1),
 ('folder.edittimestamp','edittimestamp','',10002,0,3,0,1,0,'','',1),
 ('folder.editor.email','editoremail','',10002,0,1,0,1,0,'','',1),
 ('folder.mother','folder_id','',10002,10002,2,0,1,1,'','',1),
 ('','obj_type','',10002,0,3,0,1,0,'','',1),
 ('','contentid','',10002,0,1,0,1,0,'','',1),
 ('page.name','name','',10007,0,1,0,1,1,'','',1),
 ('page.description','description','',10007,0,1,0,1,0,'','',1),
 ('page.creator','creator','',10007,0,1,0,1,0,'','',1),
 ('page.creationtimestamp','createtimestamp','',10007,0,3,0,1,0,'','',1),
 ('page.creator.email','creatoremail','',10007,0,1,0,1,0,'','',1),
 ('page.editor','editor','',10007,0,1,0,1,0,'','',1),
 ('page.edittimestamp','edittimestamp','',10007,0,3,0,1,0,'','',1),
 ('page.editor.email','editoremail','',10007,0,1,0,1,0,'','',1),
 ('page.publisher','publisher','',10007,0,1,0,1,1,'','',1),
 ('page.publishtimestamp','publishtimestamp','',10007,0,3,0,1,0,'','',1),
 ('page.publisher.email','publishermail','',10007,0,1,0,1,0,'','',1),
 ('folder.id','folder_id','',10007,10002,2,0,1,1,'','',1),
 ('','obj_type','',10007,0,3,0,1,0,'','',1),
 ('','contentid','',10007,0,1,0,1,0,'','',1),
 ('page.priority','priority','',10007,0,3,0,1,0,'','',1),
 ('','content','',10007,0,5,0,1,0,'','',1),
 ('page.language.id','languageid','',10007,0,1,0,1,0,'','',1),
 ('folder.pub_dir','pub_dir','',10002,0,1,0,1,1,'','',1),
 ('file.name','name','',10008,0,1,0,1,1,'','',1),
 ('file.type','mimetype','',10008,0,1,0,1,0,'','',1),
 ('page.url','url','',10007,0,1,0,1,0,'','',1),
 ('page.language.code','languagecode','',10007,0,1,0,1,0,'','',1),
 ('binarycontent','binarycontent','',10008,0,6,0,1,0,'','',1),
 ('','subfolder_id','',10002,10002,7,0,1,0,'folder_id','',1),
 ('','subpage_id','',10002,10007,7,0,1,0,'folder_id','',1),
 ('','subfile_id','',10002,10008,7,0,1,0,'folder_id','',1),
 ('node.id','node_id','',10007,0,1,0,1,1,'','',1),
 ('node.id','node_id','',10002,0,1,0,1,1,'','',1),
 ('node.id','node_id','',10008,0,1,0,1,1,'','',1),
 ('page.ml_id','ml_id','',10007,0,3,0,1,0,'','',1),
 ('file.name','filename','',10008,0,1,0,1,1,'','',1),
 ('page.filename','filename','',10007,0,1,0,1,1,'','',1),
 ('folder.path','url','',10002,0,1,0,1,0,'','',1),
 ('page.folder.pub_dir','pub_dir','',10007,0,1,0,1,1,'','',1),
 ('file.folder.pub_dir','pub_dir','',10008,0,1,0,1,1,'','',1);

INSERT INTO `part` (`id`, `construct_id`, `type_id`, `name_id`, `required`, `editable`, `partoption_id`, `partorder`, `keyword`, `hidden`, `ml_id`, `info_int`, `info_text`) VALUES (900,248,32,2982,0,1,0,1,'datasource',0,13,1,'test');
INSERT INTO `part` (`id`, `construct_id`, `type_id`, `name_id`, `required`, `editable`, `partoption_id`, `partorder`, `keyword`, `hidden`, `ml_id`, `info_int`, `info_text`) VALUES (901,248,27,2983,0,1,0,2,'dhtml',0,1,0,'');
INSERT INTO `part` (`id`, `construct_id`, `type_id`, `name_id`, `required`, `editable`, `partoption_id`, `partorder`, `keyword`, `hidden`, `ml_id`, `info_int`, `info_text`) VALUES (902,248,22,2984,0,1,0,3,'file',0,13,0,'');
INSERT INTO `part` (`id`, `construct_id`, `type_id`, `name_id`, `required`, `editable`, `partoption_id`, `partorder`, `keyword`, `hidden`, `ml_id`, `info_int`, `info_text`) VALUES (903,248,18,2985,0,1,0,4,'height',0,13,0,'');
INSERT INTO `part` (`id`, `construct_id`, `type_id`, `name_id`, `required`, `editable`, `partoption_id`, `partorder`, `keyword`, `hidden`, `ml_id`, `info_int`, `info_text`) VALUES (904,248,19,2986,0,1,0,5,'width',0,13,0,'');
INSERT INTO `part` (`id`, `construct_id`, `type_id`, `name_id`, `required`, `editable`, `partoption_id`, `partorder`, `keyword`, `hidden`, `ml_id`, `info_int`, `info_text`) VALUES (906,248,17,2988,0,1,0,7,'orderedlist',0,1,0,'mylist');
INSERT INTO `part` (`id`, `construct_id`, `type_id`, `name_id`, `required`, `editable`, `partoption_id`, `partorder`, `keyword`, `hidden`, `ml_id`, `info_int`, `info_text`) VALUES (907,248,16,2989,0,1,0,8,'unorderedlist',0,1,0,'mylist');
 
INSERT INTO `construct` (`id`, `name_id`, `ml_id`, `keyword`, `childable`, `intext`, `locked`, `locked_by`, `global`, `creator`, `cdate`, `editor`, `edate`, `description_id`, `autoenable`, `category_id`) VALUES (248,2980,1,'parttypetest',1,1,0,0,0,1,1260980296,1,1260981451,2981,1,14);

 
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (12,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (12,4);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (20,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (20,4);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (164,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (164,4);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (105,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (105,4);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (106,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (106,4);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (104,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (104,4);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (15,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (15,4);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (247,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (247,4);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (14,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (14,4);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (182,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (223,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (22,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (193,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (170,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (230,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (227,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (229,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (228,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (156,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (211,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (237,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (138,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (168,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (95,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (198,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (99,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (92,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (154,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (210,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (183,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (133,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (160,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (153,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (199,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (236,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (209,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (192,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (200,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (226,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (231,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (243,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (151,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (245,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (246,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (2,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (235,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (233,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (187,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (161,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (159,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (123,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (124,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (188,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (125,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (126,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (189,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (127,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (128,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (129,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (130,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (145,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (122,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (166,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (167,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (165,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (173,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (147,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (144,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (212,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (146,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (13,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (191,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (213,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (214,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (179,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (66,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (72,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (169,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (232,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (234,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (201,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (202,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (220,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (190,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (175,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (176,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (177,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (68,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (67,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (71,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (171,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (242,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (174,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (206,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (207,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (197,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (195,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (208,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (204,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (203,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (172,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (120,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (121,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (221,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (224,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (239,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (178,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (136,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (155,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (194,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (148,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (149,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (152,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (137,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (238,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (162,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (163,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (150,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (244,3);
INSERT INTO `construct_node` (`construct_id`, `node_id`) VALUES (248,3);

INSERT INTO `content` (`id`,`node_id`,`locked`,`locked_by`,`creator`,`cdate`,`editor`,`edate`) VALUES
 (3,3,0,0,1,1258376611,1,1258376611),
 (4,3,0,0,1,1258376636,1,1258376636),
 (5,3,0,0,1,1258376611,1,1258376611),
 (6,3,0,0,1,1258376636,1,1258376636),
 (7,4,0,0,1,1258377466,1,1258377466),
 (8,4,0,0,1,1258465345,1,1258465345),
 (9,4,0,0,1,1258465533,1,1258465533),
 (10,4,0,0,1,1258465789,1,1258465789),
 (11,4,0,0,1,1258465872,1,1258465872),
 (12,4,0,0,1,1258465943,1,1258465943),
 (13,4,0,0,1,1258465985,1,1258465985),
 (14,4,0,0,1,1258466031,1,1258466031),
 (15,4,0,0,1,1258466083,1,1258466083),
 (16,4,0,0,1,1258466157,1,1258466157),
 (17,4,0,0,1,1258466365,1,1258466365),
 (18,4,0,0,1,1258466418,1,1258466418),
 (19,4,0,0,1,1258466465,1,1258466465),
 (20,4,0,0,1,1258466502,1,1258466502),
 (21,4,0,0,1,1258466554,1,1258466554),
 (22,4,0,0,1,1259066563,1,1259066563),
 (23,4,0,0,1,1259066563,1,1259066563),
 (24,3,0,0,1,1259147447,1,1259147447);
INSERT INTO `content` (`id`, `node_id`, `locked`, `locked_by`, `creator`, `cdate`, `editor`, `edate`) VALUES (28,3,0,0,1,1260973907,1,1260973907);

INSERT INTO `contentfile` (`id`,`name`,`filetype`,`folder_id`,`filesize`,`creator`,`cdate`,`editor`,`edate`,`description`,`sizex`,`sizey`,`md5`,`dpix`,`dpiy`) VALUES
 (3,'testfile1.1.txt','text/plain',7,18,1,1258376795,1,1258376905,'',0,0,'3bdec9c4e06e01a307a63b1280041be0',0,0),
 (4,'testfile1.2.txt','text/plain',7,18,1,1258376885,1,1258376910,'',0,0,'3bdec9c4e06e01a307a63b1280041be0',0,0),
 (5,'textimage1.1.gif','image/gif',7,831,1,1258376964,1,1258376964,'',11,11,'daa8f181004ac956fcff940f5731964f',72,72),
 (6,'testimage1.2.gif','image/gif',7,831,1,1258376981,1,1258376981,'',11,11,'daa8f181004ac956fcff940f5731964f',72,72),
 (7,'testfile1.txt','text/plain',14,18,1,1258377523,1,1258377523,'',0,0,'3bdec9c4e06e01a307a63b1280041be0',0,0),
 (8,'testimage1.gif','image/gif',14,831,1,1258377495,1,1258377495,'',11,11,'daa8f181004ac956fcff940f5731964f',72,72),
 (9,'file.sh','application/x-sh',22,235,1,1259067007,1,1259067007,'',0,0,'556d9854d777d396c428cb3f26e02e10',0,0);


INSERT INTO `contentset` (`id`,`nada`) VALUES 
 (1454,0),
 (1455,0),
 (1456,0),
 (1457,0),
 (1458,0),
 (1459,0),
 (1460,0),
 (1461,0),
 (1462,0),
 (1463,0),
 (1464,0),
 (1465,0),
 (1466,0),
 (1467,0),
 (1468,0),
 (1469,0),
 (1470,0),
 (1471,0),
 (1472,0);
 
INSERT INTO `contentset` (`id`, `nada`) VALUES (1473,0);
INSERT INTO `contentset` (`id`, `nada`) VALUES (1474,0);
INSERT INTO `contentset` (`id`, `nada`) VALUES (1475,0);
INSERT INTO `contentset` (`id`, `nada`) VALUES (1476,0);

INSERT INTO `contenttag` (`id`,`content_id`,`construct_id`,`enabled`,`name`,`unused`) VALUES
 (3,3,12,1,'html',0),
 (4,4,12,1,'html',0),
 (5,5,12,1,'html',0),
 (6,6,12,1,'html',0),
 (7,7,14,1,'html',0),
 (8,8,14,1,'html',0),
 (9,8,20,1,'dsoverview1',0),
 (10,9,14,1,'html',0),
 (11,9,164,1,'vtl1',0),
 (12,10,14,1,'html',0),
 (13,10,20,1,'dsoverview1',0),
 (14,11,14,1,'html',0),
 (15,11,164,1,'vtl1',0),
 (16,12,14,1,'html',0),
 (17,12,104,1,'seitenurl1',0),
 (18,13,14,1,'html',0),
 (19,13,15,1,'seitentag1',0),
 (20,14,14,1,'html',0),
 (21,14,20,1,'dsoverview1',0),
 (22,15,14,1,'html',0),
 (23,15,164,1,'vtl1',0),
 (24,16,14,1,'html',0),
 (25,16,106,1,'dateiurl1',0),
 (26,17,14,1,'html',0),
 (27,17,20,1,'dsoverview1',0),
 (28,18,14,1,'html',0),
 (29,18,164,1,'vtl1',0),
 (30,19,14,1,'html',0),
 (31,19,105,1,'bildurl1',0),
 (32,20,14,1,'html',0),
 (33,20,247,1,'templatetag1',0),
 (34,21,14,1,'html',0),
 (35,21,164,1,'vtl1',0),
 (36,22,14,3,'html',0),
 (37,23,14,3,'html',0),
 (38,24,12,3,'html',0);
 
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (42,28,12,3,'html',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (43,28,14,1,'text',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (44,28,243,1,'blogbild1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (45,28,166,1,'bread1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (46,28,22,1,'liste1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (47,28,104,3,'seitenurl1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (48,28,99,1,'download1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (49,28,195,1,'googlemap1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (50,28,124,1,'checkbox1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (51,28,122,1,'form1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (52,28,173,1,'nav11',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (53,28,173,3,'nav12',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (54,28,239,1,'gallerie1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (55,28,15,1,'seitentag1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (56,28,2,1,'link1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (57,28,193,1,'tabext1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (58,28,193,3,'tabext1.1.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (59,28,14,3,'tabext1.A1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (60,28,193,3,'tabext1.A1.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (61,28,14,3,'tabext1.B1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (62,28,193,3,'tabext1.B1.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (63,28,14,3,'tabext1.C1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (64,28,193,3,'tabext1.C1.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (65,28,193,3,'tabext1.2.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (66,28,14,3,'tabext1.A2',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (67,28,193,3,'tabext1.A2.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (68,28,14,3,'tabext1.B2',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (69,28,193,3,'tabext1.B2.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (70,28,14,3,'tabext1.C2',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (71,28,193,3,'tabext1.C2.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (72,28,193,3,'tabext1.3.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (73,28,14,3,'tabext1.A3',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (74,28,193,3,'tabext1.A3.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (75,28,14,3,'tabext1.B3',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (76,28,193,3,'tabext1.B3.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (77,28,14,3,'tabext1.C3',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (78,28,193,3,'tabext1.C3.style',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (79,28,247,1,'templatetag1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (80,28,194,1,'lightboximage1',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (81,28,243,1,'blogbild2',0);
INSERT INTO `contenttag` (`id`, `content_id`, `construct_id`, `enabled`, `name`, `unused`) VALUES (82,28,248,1,'parttypetest1',0);

INSERT INTO outputuser (id, info) VALUES
 (2968, 6),
 (2969, 6),
 (2970, 6),
 (2971, 6),
 (2972, 6),
 (2973, 6),
 (2974, 6),
 (2975, 6),
 (2976, 6),
 (2977, 6);

INSERT INTO `outputuser` (`id`, `info`) VALUES (2978,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2979,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2980,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2981,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2982,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2983,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2984,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2985,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2986,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2987,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2988,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2989,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2992,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2993,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2994,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2995,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2996,6);
INSERT INTO `outputuser` (`id`, `info`) VALUES (2997,6);

INSERT INTO dicuser (output_id, language_id, value) VALUES
 (2968, 1, 'object.templatetest'),
 (2968, 2, 'object.templatetest'),
 (2968, 3, 'object.templatetest'),
 (2969, 1, 'description for object.templatetest'),
 (2969, 2, 'description for object.templatetest'),
 (2969, 3, 'description for object.templatetest'),
 (2970, 1, 'object.filetest'),
 (2970, 2, 'object.filetest'),
 (2970, 3, 'object.filetest'),
 (2971, 1, 'description for object.filetest'),
 (2971, 2, 'description for object.filetest'),
 (2971, 3, 'description for object.filetest'),
 (2972, 1, 'object.imagetest'),
 (2972, 2, 'object.imagetest'),
 (2972, 3, 'object.imagetest'),
 (2973, 1, 'description for object.imagetest'),
 (2973, 2, 'description for object.imagetest'),
 (2973, 3, 'description for object.imagetest'),
 (2974, 1, 'object.foldertest'),
 (2974, 2, 'object.foldertest'),
 (2974, 3, 'object.foldertest'),
 (2975, 1, 'description for object.foldertest'),
 (2975, 2, 'description for object.foldertest'),
 (2975, 3, 'description for object.foldertest'),
 (2976, 1, 'object.pagetest'),
 (2976, 2, 'object.pagetest'),
 (2976, 3, 'object.pagetest'),
 (2977, 1, 'description for object.pagetest'),
 (2977, 2, 'description for object.pagetest'),
 (2977, 3, 'description for object.pagetest');

INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES
 (2978, 1, 'Deutsch', 4431),
 (2978, 2, 'Deutsch', 4432),
 (2979, 1, '', 4433),
 (2979, 2, '', 4434);
 
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2980,1,'Parttype Test Tagtyp',4435);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2980,2,'Parttype Test Tagtyp',4436);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2980,3,'Parttype Test Tagtyp',4437);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2981,1,'',4438);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2981,2,'',4439);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2981,3,'',4440);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2982,1,'Datenquelle',4441);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2982,2,'Datenquelle',4442);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2982,3,'Datenquelle',4443);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2983,1,'DHTML',4444);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2983,2,'DHTML',4445);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2983,3,'DHTML',4446);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2984,1,'File (localpath)',4447);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2984,2,'File (localpath)',4448);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2984,3,'File (localpath)',4449);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2985,1,'Bildhöhe',4450);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2985,2,'Bildhöhe',4451);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2985,3,'Bildhöhe',4452);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2986,1,'Bildbreite',4453);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2986,2,'Bildbreite',4454);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2986,3,'Bildbreite',4455);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2987,1,'Java Editor',4456);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2987,2,'Java Editor',4457);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2987,3,'Java Editor',4458);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2988,1,'Geordnete Liste',4459);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2988,2,'Geordnete Liste',4460);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2988,3,'Geordnete Liste',4461);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2989,1,'Ungeordnete Liste',4462);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2989,2,'Ungeordnete Liste',4463);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2989,3,'Ungeordnete Liste',4464);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2992,1,'Datenquelle',4471);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2992,2,'Datenquelle',4472);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2992,3,'Datenquelle',4473);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2993,1,'DHTML',4474);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2993,2,'DHTML',4475);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2993,3,'DHTML',4476);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2994,1,'File (localpath)',4477);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2994,2,'File (localpath)',4478);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2994,3,'File (localpath)',4479);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2995,1,'Bildhöhe',4480);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2995,2,'Bildhöhe',4481);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2995,3,'Bildhöhe',4482);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2996,1,'Bildbreite',4483);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2996,2,'Bildbreite',4484);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2996,3,'Bildbreite',4485);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2997,1,'Java Editor',4486);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2997,2,'Java Editor',4487);
INSERT INTO `dicuser` (`output_id`, `language_id`, `value`, `id`) VALUES (2997,3,'Java Editor',4488);

INSERT INTO `ds` (`id`,`templatetag_id`,`contenttag_id`,`globaltag_id`,`o_type`,`is_folder`,`orderkind`,`orderway`,`max_obj`,`recursion`,`objtag_id`) VALUES
 (246,0,9,0,10002,2,1,1,0,0,0),
 (247,0,13,0,10007,2,1,1,0,0,0),
 (248,0,21,0,10008,2,1,1,0,0,0),
 (249,0,27,0,10011,2,1,1,0,0,0);
INSERT INTO `ds` (`id`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `o_type`, `is_folder`, `orderkind`, `orderway`, `max_obj`, `recursion`, `objtag_id`) VALUES (250,0,54,0,10011,1,2,2,0,1,0);

INSERT INTO `ds_obj` (`ds_id`,`templatetag_id`,`contenttag_id`,`globaltag_id`,`o_id`,`obj_order`,`id`,`adate`,`auser`,`objtag_id`) VALUES
 (246,0,9,0,7,1,691,1258465383,1,0),
 (246,0,9,0,8,2,692,1258465383,1,0),
 (246,0,9,0,9,3,693,1258465383,1,0),
 (247,0,13,0,3,1,694,1258465826,1,0),
 (247,0,13,0,4,2,695,1258465826,1,0),
 (248,0,21,0,3,1,696,1258466067,1,0),
 (248,0,21,0,4,2,697,1258466067,1,0),
 (249,0,27,0,5,1,698,1258466401,1,0),
 (249,0,27,0,6,2,699,1258466401,1,0);

INSERT INTO `folder` (`id`,`mother`,`name`,`type_id`,`pub_dir`,`node_id`,`creator`,`cdate`,`editor`,`edate`,`description`,`startpage_id`) VALUES
 (5,0,'Node A',10001,'/',3,1,1258375494,1,1258375494,'',0),
 (6,5,'Folder 1',10002,'/',3,1,1258375881,1,1258375881,'',0),
 (7,6,'Folder 1.1',10002,'/',3,1,1258375915,1,1258375915,'',0),
 (8,6,'Folder 1.2',10002,'/',3,1,1258375934,1,1258375934,'',0),
 (9,6,'Folder 1.3',10002,'/',3,1,1258375949,1,1258375949,'',0),
 (10,5,'Folder 2',10002,'/',3,1,1258375903,1,1258375903,'',0),
 (11,10,'Folder 2.1',10002,'/',3,1,1258375964,1,1258375964,'',0),
 (12,10,'Folder 2.2',10002,'/',3,1,1258375974,1,1258375974,'',0),
 (13,0,'Node B',10001,'/',4,1,1258375521,1,1258375521,'',0),
 (14,13,'Folder 1',10002,'/',4,1,1258376354,1,1258376354,'',0),
 (15,13,'Folder 2',10002,'/',4,1,1258376364,1,1258376364,'',0),
 (16,13,'DirtTests',10002,'/',4,1,1258464511,1,1258464511,'',0),
 (17,16,'Dirt1',10002,'/',4,1,1258464526,1,1258464526,'',0),
 (18,16,'Dirt2',10002,'/',4,1,1258464534,1,1258464534,'',0),
 (19,16,'Dirt3',10002,'/',4,1,1258464541,1,1258464541,'',0),
 (20,16,'Dirt4',10002,'/',4,1,1258464548,1,1258464548,'',0),
 (21,16,'Dirt5',10002,'/',4,1,1258464555,1,1258464555,'',0),
 (22,13,'PageFileTemplateTest',10002,'/',4,1,1259065031,1,1259065031,'',0);

INSERT INTO `node` (`id`,`folder_id`,`pub_dir`,`host`,`ftphost`,`ftplogin`,`ftppassword`,`ftpsync`,`ftpwwwroot`,`creator`,`cdate`,`editor`,`edate`,`utf8`,`publish_fs`,`publish_contentmap`,`contentmap_handle`,`disable_publish`,`contentrepository_id`) VALUES
 (3,5,'/Content.Node','test.nodea.com','','','',0,'.',1,1258375494,1,1258375494,1,1,1,'',0,1),
 (4,13,'/Content.Node','test.nodeb.com','','','',0,'.',1,1258375521,1,1258375521,1,1,1,'',0,1);

INSERT INTO `node_contentgroup` (node_id, contentgroup_id, sortorder) VALUES  
 (4,1,1),
 (4,2,2);
 
INSERT INTO `objprop` (`id`,`name_id`,`description_id`,`o_type`,`keyword`,`creator`,`cdate`,`editor`,`edate`,`objtag_id`,`category_id`) VALUES
 (54,2968,2969,0,'',1,0,1,0,9196,-9999),
 (55,2970,2971,0,'',1,0,1,0,9197,-9999),
 (56,2972,2973,0,'',1,0,1,0,9198,-9999),
 (57,2974,2975,0,'',1,0,1,0,9199,-9999),
 (58,2976,2977,0,'',1,0,1,0,9200,-9999);

INSERT INTO `objtag` (`id`,`obj_id`,`obj_type`,`construct_id`,`enabled`,`name`,`intag`,`inheritable`,`required`) VALUES
 (9179,68,10006,12,1,'object.templatetest',0,0,0),
 (9180,69,10006,12,1,'object.templatetest',0,0,0),
 (9181,4,10008,12,1,'object.filetest',0,0,0),
 (9182,5,10011,12,1,'object.imagetest',0,0,0),
 (9183,7,10002,12,1,'object.foldertest',0,0,0),
 (9184,8,10002,12,1,'object.foldertest',0,0,0),
 (9185,9,10002,12,1,'object.foldertest',0,0,0),
 (9186,6,10002,12,1,'object.foldertest',0,0,0),
 (9187,11,10002,12,1,'object.foldertest',0,0,0),
 (9188,12,10002,12,1,'object.foldertest',0,0,0),
 (9189,10,10002,12,1,'object.foldertest',0,0,0),
 (9190,70,10006,12,1,'object.templatetest',0,0,0),
 (9191,7,10007,12,1,'object.pagetest',0,0,0),
 (9192,7,10008,12,1,'object.filetest',0,0,0),
 (9193,8,10011,12,1,'object.imagetest',0,0,0),
 (9194,14,10002,12,1,'object.foldertest',0,0,0),
 (9195,15,10002,12,1,'object.foldertest',0,0,0),
 (9196,0,10006,12,0,'object.templatetest',0,0,0),
 (9197,0,10008,12,0,'object.filetest',0,0,0),
 (9198,0,10011,12,0,'object.imagetest',0,0,0),
 (9199,0,10002,12,0,'object.foldertest',0,0,0),
 (9200,0,10007,12,0,'object.pagetest',0,0,0);

INSERT INTO `objtag` (`id`, `obj_id`, `obj_type`, `construct_id`, `enabled`, `name`, `intag`, `inheritable`, `required`) VALUES (9201,28,10007,12,0,'object.pagetest',0,0,0);


INSERT INTO `page` (`id`,`name`,`description`,`filename`,`priority`,`status`,`time_start`,`time_end`,`time_mon`,`time_tue`,`time_wed`,`time_thu`,`time_fri`,`time_sat`,`time_sun`,`content_id`,`template_id`,`folder_id`,`creator`,`cdate`,`editor`,`edate`,`pdate`,`publisher`,`time_pub`,`contentgroup_id`,`contentset_id`,`delay_publish`) VALUES
 (3,'Page 1.1','','Page-11.html',1,1,0,0,0,0,0,0,0,0,0,3,68,7,1,1258376611,1,1258376611,1258376708,1,0,0,0,0),
 (4,'Page 1.2','','Page-12.html',1,1,0,0,0,0,0,0,0,0,0,4,69,7,1,1258376636,1,1258376636,1258376708,1,0,0,0,0),
 (5,'Page 1.1','','Page-111.html',1,1,0,0,0,0,0,0,0,0,0,5,68,8,1,1258376611,1,1258376611,1258377407,1,0,0,1454,0),
 (6,'Page 1.2','','Page-121.html',1,1,0,0,0,0,0,0,0,0,0,6,69,8,1,1258376636,1,1258376636,1258377407,1,0,0,1455,0),
 (7,'Page 1','','Page-1.html',1,1,0,0,0,0,0,0,0,0,0,7,70,14,1,1258377466,1,1258377466,1258378633,1,0,0,0,0),
 (8,'Dirt1-1','','Dirt1-1.html',1,1,0,0,0,0,0,0,0,0,0,8,70,17,1,1258465345,1,1258465345,1258465422,1,0,0,0,0),
 (9,'Dirt1-2','','Dirt1-2.html',1,1,0,0,0,0,0,0,0,0,0,9,70,17,1,1258465533,1,1258465533,1258465757,1,0,0,0,0),
 (10,'Dirt2-1','','Dirt2-1.html',1,1,0,0,0,0,0,0,0,0,0,10,70,18,1,1258465789,1,1258465789,1258465857,1,0,0,0,0),
 (11,'Dirt2-2','','Dirt2-2.html',1,1,0,0,0,0,0,0,0,0,0,11,70,18,1,1258465872,1,1258465872,1258465923,1,0,0,0,0),
 (12,'Dirt2-3','','Dirt2-3.html',1,1,0,0,0,0,0,0,0,0,0,12,70,18,1,1258465943,1,1258465943,1258465971,1,0,0,0,0),
 (13,'Dirt2-4','','Dirt2-4.html',1,1,0,0,0,0,0,0,0,0,0,13,70,18,1,1258465985,1,1258465985,1258466014,1,0,0,0,0),
 (14,'Dirt3-1','','Dirt3-1.html',1,1,0,0,0,0,0,0,0,0,0,14,70,19,1,1258466031,1,1258466031,1258466070,1,0,0,0,0),
 (15,'Dirt3-2','','Dirt3-2.html',1,1,0,0,0,0,0,0,0,0,0,15,70,19,1,1258466083,1,1258466083,1258466146,1,0,0,0,0),
 (16,'Dirt3-3','','Dirt3-3.html',1,1,0,0,0,0,0,0,0,0,0,16,70,19,1,1258466157,1,1258466157,1258466343,1,0,0,0,0),
 (17,'Dirt4-1','','Dirt4-1.html',1,1,0,0,0,0,0,0,0,0,0,17,70,20,1,1258466365,1,1258466365,1258466403,1,0,0,0,0),
 (18,'Dirt4-2','','Dirt4-2.html',1,1,0,0,0,0,0,0,0,0,0,18,70,20,1,1258466418,1,1258466418,1258466456,1,0,0,0,0),
 (19,'Dirt4-3','','Dirt4-3.html',1,1,0,0,0,0,0,0,0,0,0,19,70,20,1,1258466465,1,1258466465,1258466489,1,0,0,0,0),
 (20,'Dirt5-1','','Dirt5-1.html',1,1,0,0,0,0,0,0,0,0,0,20,70,21,1,1258466502,1,1258466502,1258466540,1,0,0,0,0),
 (21,'Dirt5-2','','Dirt5-2.html',1,1,0,0,0,0,0,0,0,0,0,21,70,21,1,1258466554,1,1258466554,1258466637,1,0,0,0,0),
 (22,'page','','page.de.html',1,1,0,0,0,0,0,0,0,0,0,22,70,22,1,1259066563,1,1259066563,1259066583,1,0,1,1471,0),
 (23,'page','','page.en.html',1,1,0,0,0,0,0,0,0,0,0,23,70,22,1,1259066563,1,1259066837,1259066968,1,0,2,1471,0),
 (24,'page1','','page1.html',1,1,0,0,0,0,0,0,0,0,0,24,69,5,1,1259147447,1,1259147447,1259147453,1,0,0,0,0);
INSERT INTO `page` (`id`, `name`, `description`, `filename`, `priority`, `status`, `time_start`, `time_end`, `time_mon`, `time_tue`, `time_wed`, `time_thu`, `time_fri`, `time_sat`, `time_sun`, `content_id`, `template_id`, `folder_id`, `creator`, `cdate`, `editor`, `edate`, `pdate`, `publisher`, `time_pub`, `contentgroup_id`, `contentset_id`, `delay_publish`) VALUES (28,'Neue Seite','','Neue-Seite.html',1,1,0,0,0,0,0,0,0,0,0,28,69,7,1,1260973907,1,1260973907,1260981662,1,0,0,0,0);

INSERT INTO `perm` (`usergroup_id`,`o_type`,`o_id`,`perm`) VALUES
 (13,1,7,'11000000000000000000000000000000'),
 (13,2,12,'00000000000000000000000000000000'),
 (13,3,8,'11000000111000000000000000000000'),
 (13,4,14,'11000000111111000000000000000000'),
 (13,5,15,'00000000000000000000000000000000'),
 (13,6,1,'00000000000000000000000000000000'),
 (13,7,16,'00000000000000000000000000000000'),
 (13,8,21,'11000000000000000000000000000000'),
 (13,9,24,'00000000000000000000000000000000'),
 (13,11,25,'11000000000000000000000000000000'),
 (13,12,26,'11000000000000000000000000000000'),
 (13,14,10002,'11000000000000000000000000000000'),
 (13,14,10006,'11000000000000000000000000000000'),
 (13,14,10007,'11000000000000000000000000000000'),
 (13,14,10008,'11000000000000000000000000000000'),
 (13,14,10011,'11000000000000000000000000000000'),
 (13,15,27,'00000000000000000000000000000000'),
 (13,16,32,'00000000000000000000000000000000'),
 (13,17,34,'11000000000000000000000000000000'),
 (13,18,37,'00000000000000000000000000000000'),
 (13,21,40,'00000000000000000000000000000000'),
 (13,22,1,'11000000000000000000000000000000'),
 (13,40,1,'11000000000000000000000000000000'),
 (13,40,2,'11000000000000000000000000000000'),
 (13,40,15,'11000000000000000000000000000000'),
 (13,40,30,'11000000000000000000000000000000'),
 (13,40,31,'11000000000000000000000000000000'),
 (13,40,32,'11000000000000000000000000000000'),
 (13,40,33,'11000000000000000000000000000000'),
 (13,40,34,'11000000000000000000000000000000'),
 (13,40,45,'11000000000000000000000000000000'),
 (13,40,53,'11000000000000000000000000000000'),
 (13,40,125,'11000000000000000000000000000000'),
 (13,40,275,'11000000000000000000000000000000'),
 (13,40,276,'11000000000000000000000000000000'),
 (13,40,443,'11000000000000000000000000000000'),
 (13,40,444,'11000000000000000000000000000000'),
 (13,40,445,'11000000000000000000000000000000'),
 (13,40,446,'11000000000000000000000000000000'),
 (13,40,447,'11000000000000000000000000000000'),
 (13,40,448,'11000000000000000000000000000000'),
 (13,40,713,'11000000000000000000000000000000'),
 (13,40,714,'11000000000000000000000000000000'),
 (13,40,715,'11000000000000000000000000000000'),
 (13,40,736,'11000000000000000000000000000000'),
 (13,40,737,'11000000000000000000000000000000'),
 (13,40,738,'11000000000000000000000000000000'),
 (13,40,828,'11000000000000000000000000000000'),
 (13,40,1800,'11000000000000000000000000000000'),
 (13,40,2565,'11000000000000000000000000000000'),
 (13,40,2567,'11000000000000000000000000000000'),
 (13,40,3667,'11000000000000000000000000000000'),
 (13,40,3668,'11000000000000000000000000000000'),
 (13,40,3669,'11000000000000000000000000000000'),
 (13,40,3670,'11000000000000000000000000000000'),
 (13,40,4240,'11000000000000000000000000000000'),
 (13,40,4323,'11000000000000000000000000000000'),
 (13,40,4876,'11000000000000000000000000000000'),
 (13,40,4878,'11000000000000000000000000000000'),
 (13,40,4975,'11000000000000000000000000000000'),
 (13,40,5034,'11000000000000000000000000000000'),
 (13,40,5036,'11000000000000000000000000000000'),
 (13,40,5338,'11000000000000000000000000000000'),
 (13,40,5847,'11000000000000000000000000000000'),
 (13,40,6591,'11000000000000000000000000000000'),
 (13,40,6814,'11000000000000000000000000000000'),
 (13,40,7069,'11000000000000000000000000000000'),
 (13,40,7510,'11000000000000000000000000000000'),
 (13,40,7537,'11000000000000000000000000000000'),
 (13,40,7637,'11000000000000000000000000000000'),
 (13,40,7639,'11000000000000000000000000000000'),
 (13,40,9145,'11000000000000000000000000000000'),
 (13,10000,1,'11000000100000000000000000000000'),
 (13,10001,1,'11000000111111111111111111111111'),
 (13,10001,2,'11000000111111111111111111111111'),
 (13,10001,3,'11000000111111111111111111111111'),
 (13,10001,4,'11000000111111111111111111111111'),
 (13,10001,4,'11000000111111111111111111111111'),
 (13,10001,19,'00000000000000000000000000000000'),
 (13,10001,72,'11000000111111111111111111111111'),
 (13,10001,113,'11000000111111111111111111111111'),
 (13,10001,206,'11000000111111111111111111111111'),
 (13,10001,240,'11000000111111111111111111111111'),
 (13,10001,264,'11000000111111111111111111111111'),
 (13,10001,293,'11000000111111111111111111111111'),
 (13,10001,306,'11000000111111111111111111111111'),
 (13,10003,9,'11000000000000000000000000000000'),
 (13,10005,17,'00000000000000000000000000000000'),
 (13,10010,11,'11000000000000000000000000000000'),
 (13,10012,31,'00000000000000000000000000000000'),
 (13,10013,35,'11000000000000000000000000000000'),
 (13,10014,44,'11000000000000000000000000000000'),
 (13,20001,10,'00000000000000000000000000000000'),
 (13,60000,41,'11000000000000000000000000000000'),
 (13,60001,3,'11000000000000000000000000000000'),
 (13,60004,2,'11000000000000000000000000000000'),
 (13,70000,45,'11000000000000000000000000000000'),
 (13,70001,1,'11000000000000000000000000000000'),
 (13,70002,50,'11000000000000000000000000000000'),
 (13,70004,270004,'11000000000000000000000000000000'),
 (13,70004,370004,'11000000111111111111111111111111'),
 (13,70005,4,'00000000000000000000000000000000'),
 (13,70005,5,'00000000000000000000000000000000'),
 (13,70005,6,'00000000000000000000000000000000'),
 (13,70006,270006,'11000000000000000000000000000000'),
 (13,70006,370006,'11000000111111111111111111111111'),
 (13,70007,3,'00000000000000000000000000000000'),
 (13,70007,4,'00000000000000000000000000000000'),
 (13,70007,5,'00000000000000000000000000000000'),
 (13,70007,6,'00000000000000000000000000000000'),
 (13,70008,270008,'11000000000000000000000000000000'),
 (13,70008,370008,'11000000111111111111111111111111'),
 (13,70009,3,'00000000000000000000000000000000'),
 (13,70009,4,'00000000000000000000000000000000'),
 (13,70009,5,'00000000000000000000000000000000'),
 (13,70010,270010,'11000000000000000000000000000000'),
 (13,70010,370010,'11000000111111111111111111111111'),
 (13,70013,270013,'11000000000000000000000000000000'),
 (13,70013,370013,'11000000111111111111111111111111'),
 (13,70014,2,'00000000000000000000000000000000'),
 (13,70015,270015,'11000000000000000000000000000000'),
 (13,70015,370015,'11000000111111111111111111111111'),
 (13,70016,270016,'11000000000000000000000000000000'),
 (13,70016,370016,'11000000111111111111111111111111'),
 (13,70020,270020,'11000000000000000000000000000000'),
 (13,70020,370020,'11000000111111111111111111111111'),
 (13,70040,270040,'11000000000000000000000000000000'),
 (13,70040,370040,'11000000111111111111111111111111'),
 (13,70050,270050,'11000000000000000000000000000000'),
 (13,70050,370050,'11000000111111111111111111111111'),
 (13,70060,270060,'11000000000000000000000000000000'),
 (13,70060,370060,'11000000111111111111111111111111'),
 (13,70070,270070,'11000000000000000000000000000000'),
 (13,70070,370070,'11000000111111111111111111111111'),
 (13,70080,370080,'11000000111111111111111111111111'),
 (13,70090,370090,'11000000111111111111111111111111'),
 (13,70100,270100,'00000000000000000000000000000000'),
 (13,70100,370100,'00000000000000000000000000000000'),
 (13,70110,270110,'11000000000000000000000000000000'),
 (13,70110,370110,'11000000111111111111111111111111'),
 (13,70120,1,'00000000000000000000000000000000'),
 (13,70120,2,'00000000000000000000000000000000'),
 (13,70120,3,'00000000000000000000000000000000'),
 (13,10001,5,'10000000001100110010000000000000'),
 (13,10002,6,'10000000001100110010000000000000'),
 (13,10002,7,'10000000000100110010000000000000'),
 (13,10002,8,'10000000001100110010000000000000'),
 (13,10002,9,'10000000001100110010000000000000'),
 (13,10002,10,'10000000001100110010000000000000'),
 (13,10002,11,'10000000001100110010000000000000'),
 (13,10002,12,'10000000001100110010000000000000'),
 (13,10002,5,'10000000001100110010000000000000'),
 (14,1,7,'11000000000000000000000000000000'),
 (14,2,12,'00000000000000000000000000000000'),
 (14,3,8,'11000000111000000000000000000000'),
 (14,4,14,'11000000111111000000000000000000'),
 (14,5,15,'00000000000000000000000000000000'),
 (14,6,1,'00000000000000000000000000000000'),
 (14,7,16,'00000000000000000000000000000000'),
 (14,8,21,'11000000000000000000000000000000'),
 (14,9,24,'00000000000000000000000000000000'),
 (14,11,25,'11000000000000000000000000000000'),
 (14,12,26,'11000000000000000000000000000000'),
 (14,14,10002,'11000000000000000000000000000000'),
 (14,14,10006,'11000000000000000000000000000000'),
 (14,14,10007,'11000000000000000000000000000000'),
 (14,14,10008,'11000000000000000000000000000000'),
 (14,14,10011,'11000000000000000000000000000000'),
 (14,15,27,'00000000000000000000000000000000'),
 (14,16,32,'00000000000000000000000000000000'),
 (14,17,34,'11000000000000000000000000000000'),
 (14,18,37,'00000000000000000000000000000000'),
 (14,21,40,'00000000000000000000000000000000'),
 (14,22,1,'11000000000000000000000000000000'),
 (14,40,1,'11000000000000000000000000000000'),
 (14,40,2,'11000000000000000000000000000000'),
 (14,40,15,'11000000000000000000000000000000'),
 (14,40,30,'11000000000000000000000000000000'),
 (14,40,31,'11000000000000000000000000000000'),
 (14,40,32,'11000000000000000000000000000000'),
 (14,40,33,'11000000000000000000000000000000'),
 (14,40,34,'11000000000000000000000000000000'),
 (14,40,45,'11000000000000000000000000000000'),
 (14,40,53,'11000000000000000000000000000000'),
 (14,40,125,'11000000000000000000000000000000'),
 (14,40,275,'11000000000000000000000000000000'),
 (14,40,276,'11000000000000000000000000000000'),
 (14,40,443,'11000000000000000000000000000000'),
 (14,40,444,'11000000000000000000000000000000'),
 (14,40,445,'11000000000000000000000000000000'),
 (14,40,446,'11000000000000000000000000000000'),
 (14,40,447,'11000000000000000000000000000000'),
 (14,40,448,'11000000000000000000000000000000'),
 (14,40,713,'11000000000000000000000000000000'),
 (14,40,714,'11000000000000000000000000000000'),
 (14,40,715,'11000000000000000000000000000000'),
 (14,40,736,'11000000000000000000000000000000'),
 (14,40,737,'11000000000000000000000000000000'),
 (14,40,738,'11000000000000000000000000000000'),
 (14,40,828,'11000000000000000000000000000000'),
 (14,40,1800,'11000000000000000000000000000000'),
 (14,40,2565,'11000000000000000000000000000000'),
 (14,40,2567,'11000000000000000000000000000000'),
 (14,40,3667,'11000000000000000000000000000000'),
 (14,40,3668,'11000000000000000000000000000000'),
 (14,40,3669,'11000000000000000000000000000000'),
 (14,40,3670,'11000000000000000000000000000000'),
 (14,40,4240,'11000000000000000000000000000000'),
 (14,40,4323,'11000000000000000000000000000000'),
 (14,40,4876,'11000000000000000000000000000000'),
 (14,40,4878,'11000000000000000000000000000000'),
 (14,40,4975,'11000000000000000000000000000000'),
 (14,40,5034,'11000000000000000000000000000000'),
 (14,40,5036,'11000000000000000000000000000000'),
 (14,40,5338,'11000000000000000000000000000000'),
 (14,40,5847,'11000000000000000000000000000000'),
 (14,40,6591,'11000000000000000000000000000000'),
 (14,40,6814,'11000000000000000000000000000000'),
 (14,40,7069,'11000000000000000000000000000000'),
 (14,40,7510,'11000000000000000000000000000000'),
 (14,40,7537,'11000000000000000000000000000000'),
 (14,40,7637,'11000000000000000000000000000000'),
 (14,40,7639,'11000000000000000000000000000000'),
 (14,40,9145,'11000000000000000000000000000000'),
 (14,10000,1,'11000000100000000000000000000000'),
 (14,10001,1,'11000000111111111111111111111111'),
 (14,10001,2,'11000000111111111111111111111111'),
 (14,10001,3,'11000000111111111111111111111111'),
 (14,10001,4,'11000000111111111111111111111111'),
 (14,10001,4,'11000000111111111111111111111111'),
 (14,10001,19,'00000000000000000000000000000000'),
 (14,10001,72,'11000000111111111111111111111111'),
 (14,10001,113,'11000000111111111111111111111111'),
 (14,10001,206,'11000000111111111111111111111111'),
 (14,10001,240,'11000000111111111111111111111111'),
 (14,10001,264,'11000000111111111111111111111111'),
 (14,10001,293,'11000000111111111111111111111111'),
 (14,10001,306,'11000000111111111111111111111111'),
 (14,10003,9,'11000000000000000000000000000000'),
 (14,10005,17,'00000000000000000000000000000000'),
 (14,10010,11,'11000000000000000000000000000000'),
 (14,10012,31,'00000000000000000000000000000000'),
 (14,10013,35,'11000000000000000000000000000000'),
 (14,10014,44,'11000000000000000000000000000000'),
 (14,20001,10,'00000000000000000000000000000000'),
 (14,60000,41,'11000000000000000000000000000000'),
 (14,60001,3,'11000000000000000000000000000000'),
 (14,60004,2,'11000000000000000000000000000000'),
 (14,70000,45,'11000000000000000000000000000000'),
 (14,70001,1,'11000000000000000000000000000000'),
 (14,70002,50,'11000000000000000000000000000000'),
 (14,70004,270004,'11000000000000000000000000000000'),
 (14,70004,370004,'11000000111111111111111111111111'),
 (14,70005,4,'00000000000000000000000000000000'),
 (14,70005,5,'00000000000000000000000000000000'),
 (14,70005,6,'00000000000000000000000000000000'),
 (14,70006,270006,'11000000000000000000000000000000'),
 (14,70006,370006,'11000000111111111111111111111111'),
 (14,70007,3,'00000000000000000000000000000000'),
 (14,70007,4,'00000000000000000000000000000000'),
 (14,70007,5,'00000000000000000000000000000000'),
 (14,70007,6,'00000000000000000000000000000000'),
 (14,70008,270008,'11000000000000000000000000000000'),
 (14,70008,370008,'11000000111111111111111111111111'),
 (14,70009,3,'00000000000000000000000000000000'),
 (14,70009,4,'00000000000000000000000000000000'),
 (14,70009,5,'00000000000000000000000000000000'),
 (14,70010,270010,'11000000000000000000000000000000'),
 (14,70010,370010,'11000000111111111111111111111111'),
 (14,70013,270013,'11000000000000000000000000000000'),
 (14,70013,370013,'11000000111111111111111111111111'),
 (14,70014,2,'00000000000000000000000000000000'),
 (14,70015,270015,'11000000000000000000000000000000'),
 (14,70015,370015,'11000000111111111111111111111111'),
 (14,70016,270016,'11000000000000000000000000000000'),
 (14,70016,370016,'11000000111111111111111111111111'),
 (14,70020,270020,'11000000000000000000000000000000'),
 (14,70020,370020,'11000000111111111111111111111111'),
 (14,70040,270040,'11000000000000000000000000000000'),
 (14,70040,370040,'11000000111111111111111111111111'),
 (14,70050,270050,'11000000000000000000000000000000'),
 (14,70050,370050,'11000000111111111111111111111111'),
 (14,70060,270060,'11000000000000000000000000000000'),
 (14,70060,370060,'11000000111111111111111111111111'),
 (14,70070,270070,'11000000000000000000000000000000'),
 (14,70070,370070,'11000000111111111111111111111111'),
 (14,70080,370080,'11000000111111111111111111111111'),
 (14,70090,370090,'11000000111111111111111111111111'),
 (14,70100,270100,'00000000000000000000000000000000'),
 (14,70100,370100,'00000000000000000000000000000000'),
 (14,70110,270110,'11000000000000000000000000000000'),
 (14,70110,370110,'11000000111111111111111111111111'),
 (14,70120,1,'00000000000000000000000000000000'),
 (14,70120,2,'00000000000000000000000000000000'),
 (14,70120,3,'00000000000000000000000000000000'),
 (14,10001,5,'10000000001100110010000000000000'),
 (14,10002,6,'10000000001100110010000000000000'),
 (14,10002,7,'10000000001100010010000000000000'),
 (14,10002,8,'10000000001100110010000000000000'),
 (14,10002,9,'10000000001100110010000000000000'),
 (14,10002,10,'10000000001100110010000000000000'),
 (14,10002,11,'10000000001100110010000000000000'),
 (14,10002,12,'10000000001100110010000000000000'),
 (14,10002,5,'10000000001100110010000000000000'),
 (15,1,7,'11000000000000000000000000000000'),
 (15,2,12,'00000000000000000000000000000000'),
 (15,3,8,'11000000111000000000000000000000'),
 (15,4,14,'11000000111111000000000000000000'),
 (15,5,15,'00000000000000000000000000000000'),
 (15,6,1,'00000000000000000000000000000000'),
 (15,7,16,'00000000000000000000000000000000'),
 (15,8,21,'11000000000000000000000000000000'),
 (15,9,24,'00000000000000000000000000000000'),
 (15,11,25,'11000000000000000000000000000000'),
 (15,12,26,'11000000000000000000000000000000'),
 (15,14,10002,'11000000000000000000000000000000'),
 (15,14,10006,'11000000000000000000000000000000'),
 (15,14,10007,'11000000000000000000000000000000'),
 (15,14,10008,'11000000000000000000000000000000'),
 (15,14,10011,'11000000000000000000000000000000'),
 (15,15,27,'00000000000000000000000000000000'),
 (15,16,32,'00000000000000000000000000000000'),
 (15,17,34,'11000000000000000000000000000000'),
 (15,18,37,'00000000000000000000000000000000'),
 (15,21,40,'00000000000000000000000000000000'),
 (15,22,1,'11000000000000000000000000000000'),
 (15,40,1,'11000000000000000000000000000000'),
 (15,40,2,'11000000000000000000000000000000'),
 (15,40,15,'11000000000000000000000000000000'),
 (15,40,30,'11000000000000000000000000000000'),
 (15,40,31,'11000000000000000000000000000000'),
 (15,40,32,'11000000000000000000000000000000'),
 (15,40,33,'11000000000000000000000000000000'),
 (15,40,34,'11000000000000000000000000000000'),
 (15,40,45,'11000000000000000000000000000000'),
 (15,40,53,'11000000000000000000000000000000'),
 (15,40,125,'11000000000000000000000000000000'),
 (15,40,275,'11000000000000000000000000000000'),
 (15,40,276,'11000000000000000000000000000000'),
 (15,40,443,'11000000000000000000000000000000'),
 (15,40,444,'11000000000000000000000000000000'),
 (15,40,445,'11000000000000000000000000000000'),
 (15,40,446,'11000000000000000000000000000000'),
 (15,40,447,'11000000000000000000000000000000'),
 (15,40,448,'11000000000000000000000000000000'),
 (15,40,713,'11000000000000000000000000000000'),
 (15,40,714,'11000000000000000000000000000000'),
 (15,40,715,'11000000000000000000000000000000'),
 (15,40,736,'11000000000000000000000000000000'),
 (15,40,737,'11000000000000000000000000000000'),
 (15,40,738,'11000000000000000000000000000000'),
 (15,40,828,'11000000000000000000000000000000'),
 (15,40,1800,'11000000000000000000000000000000'),
 (15,40,2565,'11000000000000000000000000000000'),
 (15,40,2567,'11000000000000000000000000000000'),
 (15,40,3667,'11000000000000000000000000000000'),
 (15,40,3668,'11000000000000000000000000000000'),
 (15,40,3669,'11000000000000000000000000000000'),
 (15,40,3670,'11000000000000000000000000000000'),
 (15,40,4240,'11000000000000000000000000000000'),
 (15,40,4323,'11000000000000000000000000000000'),
 (15,40,4876,'11000000000000000000000000000000'),
 (15,40,4878,'11000000000000000000000000000000'),
 (15,40,4975,'11000000000000000000000000000000'),
 (15,40,5034,'11000000000000000000000000000000'),
 (15,40,5036,'11000000000000000000000000000000'),
 (15,40,5338,'11000000000000000000000000000000'),
 (15,40,5847,'11000000000000000000000000000000'),
 (15,40,6591,'11000000000000000000000000000000'),
 (15,40,6814,'11000000000000000000000000000000'),
 (15,40,7069,'11000000000000000000000000000000'),
 (15,40,7510,'11000000000000000000000000000000'),
 (15,40,7537,'11000000000000000000000000000000'),
 (15,40,7637,'11000000000000000000000000000000'),
 (15,40,7639,'11000000000000000000000000000000'),
 (15,40,9145,'11000000000000000000000000000000'),
 (15,10000,1,'11000000100000000000000000000000'),
 (15,10001,1,'11000000111111111111111111111111'),
 (15,10001,2,'11000000111111111111111111111111'),
 (15,10001,3,'11000000111111111111111111111111'),
 (15,10001,4,'11000000111111111111111111111111'),
 (15,10001,4,'11000000111111111111111111111111'),
 (15,10001,19,'00000000000000000000000000000000'),
 (15,10001,72,'11000000111111111111111111111111'),
 (15,10001,113,'11000000111111111111111111111111'),
 (15,10001,206,'11000000111111111111111111111111'),
 (15,10001,240,'11000000111111111111111111111111'),
 (15,10001,264,'11000000111111111111111111111111'),
 (15,10001,293,'11000000111111111111111111111111'),
 (15,10001,306,'11000000111111111111111111111111'),
 (15,10003,9,'11000000000000000000000000000000'),
 (15,10005,17,'00000000000000000000000000000000'),
 (15,10010,11,'11000000000000000000000000000000'),
 (15,10012,31,'00000000000000000000000000000000'),
 (15,10013,35,'11000000000000000000000000000000'),
 (15,10014,44,'11000000000000000000000000000000'),
 (15,20001,10,'00000000000000000000000000000000'),
 (15,60000,41,'11000000000000000000000000000000'),
 (15,60001,3,'11000000000000000000000000000000'),
 (15,60004,2,'11000000000000000000000000000000'),
 (15,70000,45,'11000000000000000000000000000000'),
 (15,70001,1,'11000000000000000000000000000000'),
 (15,70002,50,'11000000000000000000000000000000'),
 (15,70004,270004,'11000000000000000000000000000000'),
 (15,70004,370004,'11000000111111111111111111111111'),
 (15,70005,4,'00000000000000000000000000000000'),
 (15,70005,5,'00000000000000000000000000000000'),
 (15,70005,6,'00000000000000000000000000000000'),
 (15,70006,270006,'11000000000000000000000000000000'),
 (15,70006,370006,'11000000111111111111111111111111'),
 (15,70007,3,'00000000000000000000000000000000'),
 (15,70007,4,'00000000000000000000000000000000'),
 (15,70007,5,'00000000000000000000000000000000'),
 (15,70007,6,'00000000000000000000000000000000'),
 (15,70008,270008,'11000000000000000000000000000000'),
 (15,70008,370008,'11000000111111111111111111111111'),
 (15,70009,3,'00000000000000000000000000000000'),
 (15,70009,4,'00000000000000000000000000000000'),
 (15,70009,5,'00000000000000000000000000000000'),
 (15,70010,270010,'11000000000000000000000000000000'),
 (15,70010,370010,'11000000111111111111111111111111'),
 (15,70013,270013,'11000000000000000000000000000000'),
 (15,70013,370013,'11000000111111111111111111111111'),
 (15,70014,2,'00000000000000000000000000000000'),
 (15,70015,270015,'11000000000000000000000000000000'),
 (15,70015,370015,'11000000111111111111111111111111'),
 (15,70016,270016,'11000000000000000000000000000000'),
 (15,70016,370016,'11000000111111111111111111111111'),
 (15,70020,270020,'11000000000000000000000000000000'),
 (15,70020,370020,'11000000111111111111111111111111'),
 (15,70040,270040,'11000000000000000000000000000000'),
 (15,70040,370040,'11000000111111111111111111111111'),
 (15,70050,270050,'11000000000000000000000000000000'),
 (15,70050,370050,'11000000111111111111111111111111'),
 (15,70060,270060,'11000000000000000000000000000000'),
 (15,70060,370060,'11000000111111111111111111111111'),
 (15,70070,270070,'11000000000000000000000000000000'),
 (15,70070,370070,'11000000111111111111111111111111'),
 (15,70080,370080,'11000000111111111111111111111111'),
 (15,70090,370090,'11000000111111111111111111111111'),
 (15,70100,270100,'00000000000000000000000000000000'),
 (15,70100,370100,'00000000000000000000000000000000'),
 (15,70110,270110,'11000000000000000000000000000000'),
 (15,70110,370110,'11000000111111111111111111111111'),
 (15,70120,1,'00000000000000000000000000000000'),
 (15,70120,2,'00000000000000000000000000000000'),
 (15,70120,3,'00000000000000000000000000000000'),
 (15,10001,5,'10000000001100110010000000000000'),
 (15,10002,6,'10000000001100110010000000000000'),
 (15,10002,7,'10000000001100110000000000000000'),
 (15,10002,8,'10000000001100110010000000000000'),
 (15,10002,9,'10000000001100110010000000000000'),
 (15,10002,10,'10000000001100110010000000000000'),
 (15,10002,11,'10000000001100110010000000000000'),
 (15,10002,12,'10000000001100110010000000000000'),
 (15,10002,5,'10000000001100110010000000000000'),
 (16,1,7,'11000000000000000000000000000000'),
 (16,2,12,'00000000000000000000000000000000'),
 (16,3,8,'11000000111000000000000000000000'),
 (16,4,14,'11000000111111000000000000000000'),
 (16,5,15,'00000000000000000000000000000000'),
 (16,6,1,'00000000000000000000000000000000'),
 (16,7,16,'00000000000000000000000000000000'),
 (16,8,21,'11000000000000000000000000000000'),
 (16,9,24,'00000000000000000000000000000000'),
 (16,11,25,'11000000000000000000000000000000'),
 (16,12,26,'11000000000000000000000000000000'),
 (16,14,10002,'11000000000000000000000000000000'),
 (16,14,10006,'11000000000000000000000000000000'),
 (16,14,10007,'11000000000000000000000000000000'),
 (16,14,10008,'11000000000000000000000000000000'),
 (16,14,10011,'11000000000000000000000000000000'),
 (16,15,27,'00000000000000000000000000000000'),
 (16,16,32,'00000000000000000000000000000000'),
 (16,17,34,'11000000000000000000000000000000'),
 (16,18,37,'00000000000000000000000000000000'),
 (16,21,40,'00000000000000000000000000000000'),
 (16,22,1,'11000000000000000000000000000000'),
 (16,40,1,'11000000000000000000000000000000'),
 (16,40,2,'11000000000000000000000000000000'),
 (16,40,15,'11000000000000000000000000000000'),
 (16,40,30,'11000000000000000000000000000000'),
 (16,40,31,'11000000000000000000000000000000'),
 (16,40,32,'11000000000000000000000000000000'),
 (16,40,33,'11000000000000000000000000000000'),
 (16,40,34,'11000000000000000000000000000000'),
 (16,40,45,'11000000000000000000000000000000'),
 (16,40,53,'11000000000000000000000000000000'),
 (16,40,125,'11000000000000000000000000000000'),
 (16,40,275,'11000000000000000000000000000000'),
 (16,40,276,'11000000000000000000000000000000'),
 (16,40,443,'11000000000000000000000000000000'),
 (16,40,444,'11000000000000000000000000000000'),
 (16,40,445,'11000000000000000000000000000000'),
 (16,40,446,'11000000000000000000000000000000'),
 (16,40,447,'11000000000000000000000000000000'),
 (16,40,448,'11000000000000000000000000000000'),
 (16,40,713,'11000000000000000000000000000000'),
 (16,40,714,'11000000000000000000000000000000'),
 (16,40,715,'11000000000000000000000000000000'),
 (16,40,736,'11000000000000000000000000000000'),
 (16,40,737,'11000000000000000000000000000000'),
 (16,40,738,'11000000000000000000000000000000'),
 (16,40,828,'11000000000000000000000000000000'),
 (16,40,1800,'11000000000000000000000000000000'),
 (16,40,2565,'11000000000000000000000000000000'),
 (16,40,2567,'11000000000000000000000000000000'),
 (16,40,3667,'11000000000000000000000000000000'),
 (16,40,3668,'11000000000000000000000000000000'),
 (16,40,3669,'11000000000000000000000000000000'),
 (16,40,3670,'11000000000000000000000000000000'),
 (16,40,4240,'11000000000000000000000000000000'),
 (16,40,4323,'11000000000000000000000000000000'),
 (16,40,4876,'11000000000000000000000000000000'),
 (16,40,4878,'11000000000000000000000000000000'),
 (16,40,4975,'11000000000000000000000000000000'),
 (16,40,5034,'11000000000000000000000000000000'),
 (16,40,5036,'11000000000000000000000000000000'),
 (16,40,5338,'11000000000000000000000000000000'),
 (16,40,5847,'11000000000000000000000000000000'),
 (16,40,6591,'11000000000000000000000000000000'),
 (16,40,6814,'11000000000000000000000000000000'),
 (16,40,7069,'11000000000000000000000000000000'),
 (16,40,7510,'11000000000000000000000000000000'),
 (16,40,7537,'11000000000000000000000000000000'),
 (16,40,7637,'11000000000000000000000000000000'),
 (16,40,7639,'11000000000000000000000000000000'),
 (16,40,9145,'11000000000000000000000000000000'),
 (16,10000,1,'11000000100000000000000000000000'),
 (16,10001,1,'11000000111111111111111111111111'),
 (16,10001,2,'11000000111111111111111111111111'),
 (16,10001,3,'11000000111111111111111111111111'),
 (16,10001,4,'11000000111111111111111111111111'),
 (16,10001,4,'11000000111111111111111111111111'),
 (16,10001,19,'00000000000000000000000000000000'),
 (16,10001,72,'11000000111111111111111111111111'),
 (16,10001,113,'11000000111111111111111111111111'),
 (16,10001,206,'11000000111111111111111111111111'),
 (16,10001,240,'11000000111111111111111111111111'),
 (16,10001,264,'11000000111111111111111111111111'),
 (16,10001,293,'11000000111111111111111111111111'),
 (16,10001,306,'11000000111111111111111111111111'),
 (16,10003,9,'11000000000000000000000000000000'),
 (16,10005,17,'00000000000000000000000000000000'),
 (16,10010,11,'11000000000000000000000000000000'),
 (16,10012,31,'00000000000000000000000000000000'),
 (16,10013,35,'11000000000000000000000000000000'),
 (16,10014,44,'11000000000000000000000000000000'),
 (16,20001,10,'00000000000000000000000000000000'),
 (16,60000,41,'11000000000000000000000000000000'),
 (16,60001,3,'11000000000000000000000000000000'),
 (16,60004,2,'11000000000000000000000000000000'),
 (16,70000,45,'11000000000000000000000000000000'),
 (16,70001,1,'11000000000000000000000000000000'),
 (16,70002,50,'11000000000000000000000000000000'),
 (16,70004,270004,'11000000000000000000000000000000'),
 (16,70004,370004,'11000000111111111111111111111111'),
 (16,70005,4,'00000000000000000000000000000000'),
 (16,70005,5,'00000000000000000000000000000000'),
 (16,70005,6,'00000000000000000000000000000000'),
 (16,70006,270006,'11000000000000000000000000000000'),
 (16,70006,370006,'11000000111111111111111111111111'),
 (16,70007,3,'00000000000000000000000000000000'),
 (16,70007,4,'00000000000000000000000000000000'),
 (16,70007,5,'00000000000000000000000000000000'),
 (16,70007,6,'00000000000000000000000000000000'),
 (16,70008,270008,'11000000000000000000000000000000'),
 (16,70008,370008,'11000000111111111111111111111111'),
 (16,70009,3,'00000000000000000000000000000000'),
 (16,70009,4,'00000000000000000000000000000000'),
 (16,70009,5,'00000000000000000000000000000000'),
 (16,70010,270010,'11000000000000000000000000000000'),
 (16,70010,370010,'11000000111111111111111111111111'),
 (16,70013,270013,'11000000000000000000000000000000'),
 (16,70013,370013,'11000000111111111111111111111111'),
 (16,70014,2,'00000000000000000000000000000000'),
 (16,70015,270015,'11000000000000000000000000000000'),
 (16,70015,370015,'11000000111111111111111111111111'),
 (16,70016,270016,'11000000000000000000000000000000'),
 (16,70016,370016,'11000000111111111111111111111111'),
 (16,70020,270020,'11000000000000000000000000000000'),
 (16,70020,370020,'11000000111111111111111111111111'),
 (16,70040,270040,'11000000000000000000000000000000'),
 (16,70040,370040,'11000000111111111111111111111111'),
 (16,70050,270050,'11000000000000000000000000000000'),
 (16,70050,370050,'11000000111111111111111111111111'),
 (16,70060,270060,'11000000000000000000000000000000'),
 (16,70060,370060,'11000000111111111111111111111111'),
 (16,70070,270070,'11000000000000000000000000000000'),
 (16,70070,370070,'11000000111111111111111111111111'),
 (16,70080,370080,'11000000111111111111111111111111'),
 (16,70090,370090,'11000000111111111111111111111111'),
 (16,70100,270100,'00000000000000000000000000000000'),
 (16,70100,370100,'00000000000000000000000000000000'),
 (16,70110,270110,'11000000000000000000000000000000'),
 (16,70110,370110,'11000000111111111111111111111111'),
 (16,70120,1,'00000000000000000000000000000000'),
 (16,70120,2,'00000000000000000000000000000000'),
 (16,70120,3,'00000000000000000000000000000000'),
 (17,1,7,'11000000000000000000000000000000'),
 (17,2,12,'00000000000000000000000000000000'),
 (17,3,8,'11000000111000000000000000000000'),
 (17,4,14,'11000000111111000000000000000000'),
 (17,5,15,'00000000000000000000000000000000'),
 (17,6,1,'00000000000000000000000000000000'),
 (17,7,16,'00000000000000000000000000000000'),
 (17,8,21,'11000000000000000000000000000000'),
 (17,9,24,'00000000000000000000000000000000'),
 (17,11,25,'11000000000000000000000000000000'),
 (17,12,26,'11000000000000000000000000000000'),
 (17,14,10002,'11000000000000000000000000000000'),
 (17,14,10006,'11000000000000000000000000000000'),
 (17,14,10007,'11000000000000000000000000000000'),
 (17,14,10008,'11000000000000000000000000000000'),
 (17,14,10011,'11000000000000000000000000000000'),
 (17,15,27,'00000000000000000000000000000000'),
 (17,16,32,'00000000000000000000000000000000'),
 (17,17,34,'11000000000000000000000000000000'),
 (17,18,37,'00000000000000000000000000000000'),
 (17,21,40,'00000000000000000000000000000000'),
 (17,22,1,'11000000000000000000000000000000'),
 (17,40,1,'11000000000000000000000000000000'),
 (17,40,2,'11000000000000000000000000000000'),
 (17,40,15,'11000000000000000000000000000000'),
 (17,40,30,'11000000000000000000000000000000'),
 (17,40,31,'11000000000000000000000000000000'),
 (17,40,32,'11000000000000000000000000000000'),
 (17,40,33,'11000000000000000000000000000000'),
 (17,40,34,'11000000000000000000000000000000'),
 (17,40,45,'11000000000000000000000000000000'),
 (17,40,53,'11000000000000000000000000000000'),
 (17,40,125,'11000000000000000000000000000000'),
 (17,40,275,'11000000000000000000000000000000'),
 (17,40,276,'11000000000000000000000000000000'),
 (17,40,443,'11000000000000000000000000000000'),
 (17,40,444,'11000000000000000000000000000000'),
 (17,40,445,'11000000000000000000000000000000'),
 (17,40,446,'11000000000000000000000000000000'),
 (17,40,447,'11000000000000000000000000000000'),
 (17,40,448,'11000000000000000000000000000000'),
 (17,40,713,'11000000000000000000000000000000'),
 (17,40,714,'11000000000000000000000000000000'),
 (17,40,715,'11000000000000000000000000000000'),
 (17,40,736,'11000000000000000000000000000000'),
 (17,40,737,'11000000000000000000000000000000'),
 (17,40,738,'11000000000000000000000000000000'),
 (17,40,828,'11000000000000000000000000000000'),
 (17,40,1800,'11000000000000000000000000000000'),
 (17,40,2565,'11000000000000000000000000000000'),
 (17,40,2567,'11000000000000000000000000000000'),
 (17,40,3667,'11000000000000000000000000000000'),
 (17,40,3668,'11000000000000000000000000000000'),
 (17,40,3669,'11000000000000000000000000000000'),
 (17,40,3670,'11000000000000000000000000000000'),
 (17,40,4240,'11000000000000000000000000000000'),
 (17,40,4323,'11000000000000000000000000000000'),
 (17,40,4876,'11000000000000000000000000000000'),
 (17,40,4878,'11000000000000000000000000000000'),
 (17,40,4975,'11000000000000000000000000000000'),
 (17,40,5034,'11000000000000000000000000000000'),
 (17,40,5036,'11000000000000000000000000000000'),
 (17,40,5338,'11000000000000000000000000000000'),
 (17,40,5847,'11000000000000000000000000000000'),
 (17,40,6591,'11000000000000000000000000000000'),
 (17,40,6814,'11000000000000000000000000000000'),
 (17,40,7069,'11000000000000000000000000000000'),
 (17,40,7510,'11000000000000000000000000000000'),
 (17,40,7537,'11000000000000000000000000000000'),
 (17,40,7637,'11000000000000000000000000000000'),
 (17,40,7639,'11000000000000000000000000000000'),
 (17,40,9145,'11000000000000000000000000000000'),
 (17,10000,1,'11000000100000000000000000000000'),
 (17,10001,1,'11000000111111111111111111111111'),
 (17,10001,2,'11000000111111111111111111111111'),
 (17,10001,3,'11000000111111111111111111111111'),
 (17,10001,4,'11000000111111111111111111111111'),
 (17,10001,4,'11000000111111111111111111111111'),
 (17,10001,19,'00000000000000000000000000000000'),
 (17,10001,72,'11000000111111111111111111111111'),
 (17,10001,113,'11000000111111111111111111111111'),
 (17,10001,206,'11000000111111111111111111111111'),
 (17,10001,240,'11000000111111111111111111111111'),
 (17,10001,264,'11000000111111111111111111111111'),
 (17,10001,293,'11000000111111111111111111111111'),
 (17,10001,306,'11000000111111111111111111111111'),
 (17,10003,9,'11000000000000000000000000000000'),
 (17,10005,17,'00000000000000000000000000000000'),
 (17,10010,11,'11000000000000000000000000000000'),
 (17,10012,31,'00000000000000000000000000000000'),
 (17,10013,35,'11000000000000000000000000000000'),
 (17,10014,44,'11000000000000000000000000000000'),
 (17,20001,10,'00000000000000000000000000000000'),
 (17,60000,41,'11000000000000000000000000000000'),
 (17,60001,3,'11000000000000000000000000000000'),
 (17,60004,2,'11000000000000000000000000000000'),
 (17,70000,45,'11000000000000000000000000000000'),
 (17,70001,1,'11000000000000000000000000000000'),
 (17,70002,50,'11000000000000000000000000000000'),
 (17,70004,270004,'11000000000000000000000000000000'),
 (17,70004,370004,'11000000111111111111111111111111'),
 (17,70005,4,'00000000000000000000000000000000'),
 (17,70005,5,'00000000000000000000000000000000'),
 (17,70005,6,'00000000000000000000000000000000'),
 (17,70006,270006,'11000000000000000000000000000000'),
 (17,70006,370006,'11000000111111111111111111111111'),
 (17,70007,3,'00000000000000000000000000000000'),
 (17,70007,4,'00000000000000000000000000000000'),
 (17,70007,5,'00000000000000000000000000000000'),
 (17,70007,6,'00000000000000000000000000000000'),
 (17,70008,270008,'11000000000000000000000000000000'),
 (17,70008,370008,'11000000111111111111111111111111'),
 (17,70009,3,'00000000000000000000000000000000'),
 (17,70009,4,'00000000000000000000000000000000'),
 (17,70009,5,'00000000000000000000000000000000'),
 (17,70010,270010,'11000000000000000000000000000000'),
 (17,70010,370010,'11000000111111111111111111111111'),
 (17,70013,270013,'11000000000000000000000000000000'),
 (17,70013,370013,'11000000111111111111111111111111'),
 (17,70014,2,'00000000000000000000000000000000'),
 (17,70015,270015,'11000000000000000000000000000000'),
 (17,70015,370015,'11000000111111111111111111111111'),
 (17,70016,270016,'11000000000000000000000000000000'),
 (17,70016,370016,'11000000111111111111111111111111'),
 (17,70020,270020,'11000000000000000000000000000000'),
 (17,70020,370020,'11000000111111111111111111111111'),
 (17,70040,270040,'11000000000000000000000000000000'),
 (17,70040,370040,'11000000111111111111111111111111'),
 (17,70050,270050,'11000000000000000000000000000000'),
 (17,70050,370050,'11000000111111111111111111111111'),
 (17,70060,270060,'11000000000000000000000000000000'),
 (17,70060,370060,'11000000111111111111111111111111'),
 (17,70070,270070,'11000000000000000000000000000000'),
 (17,70070,370070,'11000000111111111111111111111111'),
 (17,70080,370080,'11000000111111111111111111111111'),
 (17,70090,370090,'11000000111111111111111111111111'),
 (17,70100,270100,'00000000000000000000000000000000'),
 (17,70100,370100,'00000000000000000000000000000000'),
 (17,70110,270110,'11000000000000000000000000000000'),
 (17,70110,370110,'11000000111111111111111111111111'),
 (17,70120,1,'00000000000000000000000000000000'),
 (17,70120,2,'00000000000000000000000000000000'),
 (17,70120,3,'00000000000000000000000000000000'),
 (16,10001,5,'10000000001100110010000000000000'),
 (16,10002,6,'10000000001100110010000000000000'),
 (16,10002,7,'10000000001100110010000000000000'),
 (16,10002,8,'10000000001100110010000000000000'),
 (16,10002,9,'10000000001100010010000000000000'),
 (16,10002,10,'10000000001100110010000000000000'),
 (16,10002,11,'10000000001100110010000000000000'),
 (16,10002,12,'10000000001100110010000000000000'),
 (16,10002,5,'10000000001100110010000000000000'),
 (17,10001,5,'10000000001100110010000000000000'),
 (17,10002,6,'10000000001100110010000000000000'),
 (17,10002,7,'10000000001100110010000000000000'),
 (17,10002,8,'10000000001100110010000000000000'),
 (17,10002,9,'10000000001100110000000000000000'),
 (17,10002,10,'10000000001100110010000000000000'),
 (17,10002,11,'10000000001100110010000000000000'),
 (17,10002,12,'10000000001100110010000000000000'),
 (17,10002,5,'10000000001100110010000000000000'),
  (18,11,25,'11000000000000000000000000000000'),
 (18,10001,1,'11000000111111111111111111111111'),
 (18,17,34,'11000000000000000000000000000000'),
 (18,10013,35,'11000000000000000000000000000000'),
 (18,1,7,'11000000000000000000000000000000'),
 (18,3,8,'11000000111000000000000000000000'),
 (18,4,14,'11000000111111000000000000000000'),
 (18,6,1,'00000000000000000000000000000000'),
 (18,8,21,'11000000000000000000000000000000'),
 (18,10010,11,'11000000000000000000000000000000'),
 (18,12,26,'11000000000000000000000000000000'),
 (18,10003,9,'11000000000000000000000000000000'),
 (18,20001,10,'00000000000000000000000000000000'),
 (18,7,16,'00000000000000000000000000000000'),
 (18,21,40,'00000000000000000000000000000000'),
 (18,16,32,'00000000000000000000000000000000'),
 (18,9,24,'00000000000000000000000000000000'),
 (18,10012,31,'00000000000000000000000000000000'),
 (18,18,37,'00000000000000000000000000000000'),
 (18,10005,17,'00000000000000000000000000000000'),
 (18,2,12,'00000000000000000000000000000000'),
 (18,5,15,'00000000000000000000000000000000'),
 (18,15,27,'00000000000000000000000000000000'),
 (18,10000,1,'11000000100000000000000000000000'),
 (18,10001,2,'11000000111111111111111111111111'),
 (18,10001,3,'11000000111111111111111111111111'),
 (18,10001,4,'11000000111111111111111111111111'),
 (18,60000,41,'11000000000000000000000000000000'),
 (18,70002,50,'11000000000000000000000000000000'),
 (18,22,1,'11000000000000000000000000000000'),
 (18,10001,19,'00000000000000000000000000000000'),
 (18,70004,370004,'11000000111111111111111111111111'),
 (18,70020,370020,'11000000111111111111111111111111'),
 (18,70006,370006,'11000000111111111111111111111111'),
 (18,70070,370070,'11000000111111111111111111111111'),
 (18,70100,370100,'00000000000000000000000000000000'),
 (18,70090,370090,'11000000111111111111111111111111'),
 (18,70008,370008,'11000000111111111111111111111111'),
 (18,70060,370060,'11000000111111111111111111111111'),
 (18,70010,370010,'11000000111111111111111111111111'),
 (18,70110,370110,'11000000111111111111111111111111'),
 (18,70050,370050,'11000000111111111111111111111111'),
 (18,70013,370013,'11000000111111111111111111111111'),
 (18,70040,370040,'11000000111111111111111111111111'),
 (18,70015,370015,'11000000111111111111111111111111'),
 (18,70016,370016,'11000000111111111111111111111111'),
 (18,70080,370080,'11000000111111111111111111111111'),
 (18,60004,2,'11000000000000000000000000000000'),
 (18,60001,3,'11000000000000000000000000000000'),
 (18,70000,45,'11000000000000000000000000000000'),
 (18,70001,1,'11000000000000000000000000000000'),
 (18,10014,44,'11000000000000000000000000000000'),
 (18,70004,270004,'11000000000000000000000000000000'),
 (18,70005,6,'00000000000000000000000000000000'),
 (18,70005,5,'00000000000000000000000000000000'),
 (18,70005,4,'00000000000000000000000000000000'),
 (18,70020,270020,'11000000000000000000000000000000'),
 (18,70006,270006,'11000000000000000000000000000000'),
 (18,70007,3,'00000000000000000000000000000000'),
 (18,70007,4,'00000000000000000000000000000000'),
 (18,70007,6,'00000000000000000000000000000000'),
 (18,70007,5,'00000000000000000000000000000000'),
 (18,70070,270070,'11000000000000000000000000000000'),
 (18,70100,270100,'00000000000000000000000000000000'),
 (18,70008,270008,'11000000000000000000000000000000'),
 (18,70009,3,'00000000000000000000000000000000'),
 (18,70009,4,'00000000000000000000000000000000'),
 (18,70009,5,'00000000000000000000000000000000'),
 (18,70060,270060,'11000000000000000000000000000000'),
 (18,70010,270010,'11000000000000000000000000000000'),
 (18,70110,270110,'11000000000000000000000000000000'),
 (18,70120,1,'00000000000000000000000000000000'),
 (18,70120,2,'00000000000000000000000000000000'),
 (18,70120,3,'00000000000000000000000000000000'),
 (18,70050,270050,'11000000000000000000000000000000'),
 (18,70013,270013,'11000000000000000000000000000000'),
 (18,70014,2,'00000000000000000000000000000000'),
 (18,70040,270040,'11000000000000000000000000000000'),
 (18,70015,270015,'11000000000000000000000000000000'),
 (18,70016,270016,'11000000000000000000000000000000'),
 (18,10001,72,'11000000111111111111111111111111'),
 (18,10001,113,'11000000111111111111111111111111'),
 (18,10001,206,'11000000111111111111111111111111'),
 (18,10001,240,'11000000111111111111111111111111'),
 (18,10001,264,'11000000111111111111111111111111'),
 (18,10001,4,'11000000111111111111111111111111'),
 (18,10001,293,'11000000111111111111111111111111'),
 (18,10001,306,'11000000111111111111111111111111'),
 (18,14,10002,'11000000000000000000000000000000'),
 (18,14,10006,'11000000000000000000000000000000'),
 (18,14,10007,'11000000000000000000000000000000'),
 (18,14,10011,'11000000000000000000000000000000'),
 (18,14,10008,'11000000000000000000000000000000'),
 (18,40,7637,'11000000000000000000000000000000'),
 (18,40,30,'11000000000000000000000000000000'),
 (18,40,446,'11000000000000000000000000000000'),
 (18,40,3667,'11000000000000000000000000000000'),
 (18,40,3668,'11000000000000000000000000000000'),
 (18,40,33,'11000000000000000000000000000000'),
 (18,40,5034,'11000000000000000000000000000000'),
 (18,40,443,'11000000000000000000000000000000'),
 (18,40,45,'11000000000000000000000000000000'),
 (18,40,5847,'11000000000000000000000000000000'),
 (18,40,4323,'11000000000000000000000000000000'),
 (18,40,1800,'11000000000000000000000000000000'),
 (18,40,53,'11000000000000000000000000000000'),
 (18,40,125,'11000000000000000000000000000000'),
 (18,40,2,'11000000000000000000000000000000'),
 (18,40,4876,'11000000000000000000000000000000'),
 (18,40,738,'11000000000000000000000000000000'),
 (18,40,828,'11000000000000000000000000000000'),
 (18,40,1,'11000000000000000000000000000000'),
 (18,40,737,'11000000000000000000000000000000'),
 (18,40,275,'11000000000000000000000000000000'),
 (18,40,276,'11000000000000000000000000000000'),
 (18,40,32,'11000000000000000000000000000000'),
 (18,40,713,'11000000000000000000000000000000'),
 (18,40,4878,'11000000000000000000000000000000'),
 (18,40,5036,'11000000000000000000000000000000'),
 (18,40,4240,'11000000000000000000000000000000'),
 (18,40,15,'11000000000000000000000000000000'),
 (18,40,736,'11000000000000000000000000000000'),
 (18,40,444,'11000000000000000000000000000000'),
 (18,40,7537,'11000000000000000000000000000000'),
 (18,40,447,'11000000000000000000000000000000'),
 (18,40,7510,'11000000000000000000000000000000'),
 (18,40,5338,'11000000000000000000000000000000'),
 (18,40,2565,'11000000000000000000000000000000'),
 (18,40,2567,'11000000000000000000000000000000'),
 (18,40,4975,'11000000000000000000000000000000'),
 (18,40,31,'11000000000000000000000000000000'),
 (18,40,714,'11000000000000000000000000000000'),
 (18,40,7069,'11000000000000000000000000000000'),
 (18,40,6591,'11000000000000000000000000000000'),
 (18,40,7639,'11000000000000000000000000000000'),
 (18,40,6814,'11000000000000000000000000000000'),
 (18,40,9145,'11000000000000000000000000000000'),
 (18,40,445,'11000000000000000000000000000000'),
 (18,40,3669,'11000000000000000000000000000000'),
 (18,40,34,'11000000000000000000000000000000'),
 (18,40,715,'11000000000000000000000000000000'),
 (18,40,448,'11000000000000000000000000000000'),
 (18,40,3670,'11000000000000000000000000000000'),
 (18,10002,22,'11000000100110011000000000000000'),
 (19,11,25,'11000000000000000000000000000000'),
 (19,10001,1,'11000000111111111111111111111111'),
 (19,17,34,'11000000000000000000000000000000'),
 (19,10013,35,'11000000000000000000000000000000'),
 (19,1,7,'11000000000000000000000000000000'),
 (19,3,8,'11000000111000000000000000000000'),
 (19,4,14,'11000000111111000000000000000000'),
 (19,6,1,'00000000000000000000000000000000'),
 (19,8,21,'11000000000000000000000000000000'),
 (19,10010,11,'11000000000000000000000000000000'),
 (19,12,26,'11000000000000000000000000000000'),
 (19,10003,9,'11000000000000000000000000000000'),
 (19,20001,10,'00000000000000000000000000000000'),
 (19,7,16,'00000000000000000000000000000000'),
 (19,21,40,'00000000000000000000000000000000'),
 (19,16,32,'00000000000000000000000000000000'),
 (19,9,24,'00000000000000000000000000000000'),
 (19,10012,31,'00000000000000000000000000000000'),
 (19,18,37,'00000000000000000000000000000000'),
 (19,10005,17,'00000000000000000000000000000000'),
 (19,2,12,'00000000000000000000000000000000'),
 (19,5,15,'00000000000000000000000000000000'),
 (19,15,27,'00000000000000000000000000000000'),
 (19,10000,1,'11000000100000000000000000000000'),
 (19,10001,2,'11000000111111111111111111111111'),
 (19,10001,3,'11000000111111111111111111111111'),
 (19,10001,4,'11000000111111111111111111111111'),
 (19,60000,41,'11000000000000000000000000000000'),
 (19,70002,50,'11000000000000000000000000000000'),
 (19,22,1,'11000000000000000000000000000000'),
 (19,10001,19,'00000000000000000000000000000000'),
 (19,70004,370004,'11000000111111111111111111111111'),
 (19,70020,370020,'11000000111111111111111111111111'),
 (19,70006,370006,'11000000111111111111111111111111'),
 (19,70070,370070,'11000000111111111111111111111111'),
 (19,70100,370100,'00000000000000000000000000000000'),
 (19,70090,370090,'11000000111111111111111111111111'),
 (19,70008,370008,'11000000111111111111111111111111'),
 (19,70060,370060,'11000000111111111111111111111111'),
 (19,70010,370010,'11000000111111111111111111111111'),
 (19,70110,370110,'11000000111111111111111111111111'),
 (19,70050,370050,'11000000111111111111111111111111'),
 (19,70013,370013,'11000000111111111111111111111111'),
 (19,70040,370040,'11000000111111111111111111111111'),
 (19,70015,370015,'11000000111111111111111111111111'),
 (19,70016,370016,'11000000111111111111111111111111'),
 (19,70080,370080,'11000000111111111111111111111111'),
 (19,60004,2,'11000000000000000000000000000000'),
 (19,60001,3,'11000000000000000000000000000000'),
 (19,70000,45,'11000000000000000000000000000000'),
 (19,70001,1,'11000000000000000000000000000000'),
 (19,10014,44,'11000000000000000000000000000000'),
 (19,70004,270004,'11000000000000000000000000000000'),
 (19,70005,6,'00000000000000000000000000000000'),
 (19,70005,5,'00000000000000000000000000000000'),
 (19,70005,4,'00000000000000000000000000000000'),
 (19,70020,270020,'11000000000000000000000000000000'),
 (19,70006,270006,'11000000000000000000000000000000'),
 (19,70007,3,'00000000000000000000000000000000'),
 (19,70007,4,'00000000000000000000000000000000'),
 (19,70007,6,'00000000000000000000000000000000'),
 (19,70007,5,'00000000000000000000000000000000'),
 (19,70070,270070,'11000000000000000000000000000000'),
 (19,70100,270100,'00000000000000000000000000000000'),
 (19,70008,270008,'11000000000000000000000000000000'),
 (19,70009,3,'00000000000000000000000000000000'),
 (19,70009,4,'00000000000000000000000000000000'),
 (19,70009,5,'00000000000000000000000000000000'),
 (19,70060,270060,'11000000000000000000000000000000'),
 (19,70010,270010,'11000000000000000000000000000000'),
 (19,70110,270110,'11000000000000000000000000000000'),
 (19,70120,1,'00000000000000000000000000000000'),
 (19,70120,2,'00000000000000000000000000000000'),
 (19,70120,3,'00000000000000000000000000000000'),
 (19,70050,270050,'11000000000000000000000000000000'),
 (19,70013,270013,'11000000000000000000000000000000'),
 (19,70014,2,'00000000000000000000000000000000'),
 (19,70040,270040,'11000000000000000000000000000000'),
 (19,70015,270015,'11000000000000000000000000000000'),
 (19,70016,270016,'11000000000000000000000000000000'),
 (19,10001,72,'11000000111111111111111111111111'),
 (19,10001,113,'11000000111111111111111111111111'),
 (19,10001,206,'11000000111111111111111111111111'),
 (19,10001,240,'11000000111111111111111111111111'),
 (19,10001,264,'11000000111111111111111111111111'),
 (19,10001,4,'11000000111111111111111111111111'),
 (19,10001,293,'11000000111111111111111111111111'),
 (19,10001,306,'11000000111111111111111111111111'),
 (19,14,10002,'11000000000000000000000000000000'),
 (19,14,10006,'11000000000000000000000000000000'),
 (19,14,10007,'11000000000000000000000000000000'),
 (19,14,10011,'11000000000000000000000000000000'),
 (19,14,10008,'11000000000000000000000000000000'),
 (19,40,7637,'11000000000000000000000000000000'),
 (19,40,30,'11000000000000000000000000000000'),
 (19,40,446,'11000000000000000000000000000000'),
 (19,40,3667,'11000000000000000000000000000000'),
 (19,40,3668,'11000000000000000000000000000000'),
 (19,40,33,'11000000000000000000000000000000'),
 (19,40,5034,'11000000000000000000000000000000'),
 (19,40,443,'11000000000000000000000000000000'),
 (19,40,45,'11000000000000000000000000000000'),
 (19,40,5847,'11000000000000000000000000000000'),
 (19,40,4323,'11000000000000000000000000000000'),
 (19,40,1800,'11000000000000000000000000000000'),
 (19,40,53,'11000000000000000000000000000000'),
 (19,40,125,'11000000000000000000000000000000'),
 (19,40,2,'11000000000000000000000000000000'),
 (19,40,4876,'11000000000000000000000000000000'),
 (19,40,738,'11000000000000000000000000000000'),
 (19,40,828,'11000000000000000000000000000000'),
 (19,40,1,'11000000000000000000000000000000'),
 (19,40,737,'11000000000000000000000000000000'),
 (19,40,275,'11000000000000000000000000000000'),
 (19,40,276,'11000000000000000000000000000000'),
 (19,40,32,'11000000000000000000000000000000'),
 (19,40,713,'11000000000000000000000000000000'),
 (19,40,4878,'11000000000000000000000000000000'),
 (19,40,5036,'11000000000000000000000000000000'),
 (19,40,4240,'11000000000000000000000000000000'),
 (19,40,15,'11000000000000000000000000000000'),
 (19,40,736,'11000000000000000000000000000000'),
 (19,40,444,'11000000000000000000000000000000'),
 (19,40,7537,'11000000000000000000000000000000'),
 (19,40,447,'11000000000000000000000000000000'),
 (19,40,7510,'11000000000000000000000000000000'),
 (19,40,5338,'11000000000000000000000000000000'),
 (19,40,2565,'11000000000000000000000000000000'),
 (19,40,2567,'11000000000000000000000000000000'),
 (19,40,4975,'11000000000000000000000000000000'),
 (19,40,31,'11000000000000000000000000000000'),
 (19,40,714,'11000000000000000000000000000000'),
 (19,40,7069,'11000000000000000000000000000000'),
 (19,40,6591,'11000000000000000000000000000000'),
 (19,40,7639,'11000000000000000000000000000000'),
 (19,40,6814,'11000000000000000000000000000000'),
 (19,40,9145,'11000000000000000000000000000000'),
 (19,40,445,'11000000000000000000000000000000'),
 (19,40,3669,'11000000000000000000000000000000'),
 (19,40,34,'11000000000000000000000000000000'),
 (19,40,715,'11000000000000000000000000000000'),
 (19,40,448,'11000000000000000000000000000000'),
 (19,40,3670,'11000000000000000000000000000000'),
 (19,10002,22,'00000000000000000000000000000000');

DELETE FROM `role_usergroup_assignment`;
DELETE FROM `role_usergroup`;
DELETE FROM `roleperm_obj`;
DELETE FROM `roleperm`;
DELETE FROM `role`;
 
INSERT INTO `role` VALUES  
 (1,2521,2522),
 (2,2978,2979);
 
INSERT INTO `role_usergroup` VALUES 
 (1,1,12),
 (2,1,8),
 (3,1,9),
 (4,1,19);

INSERT INTO `role_usergroup_assignment` VALUES  
 (1,1,4,10002),
 (2,1,4,10001),
 (3,2,307,10002),
 (4,2,306,10002),
 (5,2,306,10001),
 (6,3,307,10002),
 (7,3,306,10002),
 (8,3,306,10001),
 (9,2,312,10002),
 (10,3,312,10002),
 (11,2,317,10002),
 (12,3,317,10002),
 (13,2,321,10002),
 (14,3,321,10002),
 (15,2,322,10002),
 (16,3,322,10002),
 (17,2,326,10002),
 (18,3,326,10002),
 (19,2,327,10002),
 (20,3,327,10002),
 (21,2,328,10002),
 (22,3,328,10002),
 (23,2,329,10002),
 (24,3,329,10002),
 (25,4,22,10002);

INSERT INTO `roleperm` VALUES
 (6,1,'00000000001111110000000000000000'),
 (7,1,'00000000001111000000000000000000'),
 (8,2,'00000000001111110000000000000000');

INSERT INTO `roleperm_obj` VALUES
 (8,6,10007,NULL),
 (9,6,10031,2),
 (10,7,10008,NULL),
 (11,8,10007,NULL),
 (12,8,10031,1);
 
INSERT INTO `systemuser` (`id`,`firstname`,`lastname`,`login`,`password`,`email`,`bonus`,`active`,`creator`,`cdate`,`editor`,`edate`,`description`,`isldapuser`,`inboxtoemail`) VALUES
 (20,'perm','ission1','permission1','b7dd508f9d938ff9936cf117592495eb','',0,1,1,1258467955,1,1258467955,'',0,NULL),
 (21,'perm','ission2','permission2','d62f3e4a187c67cbe459ff6caa91d623','',0,1,1,1258467969,1,1258467969,'',0,NULL),
 (22,'perm','ission3','permission3','9733dcb2f1c37bc342c86dc5ef77f18c','',0,1,1,1258467984,1,1258467984,'',0,NULL),
 (23,'perm','ission4','permission4','27877953b162acc79b8be7299744636d','',0,1,1,1258467996,1,1258467996,'',0,NULL),
 (24,'perm','ission5','permission5','e04fcba0e741c06be2fab51bbc66b9bc','',0,1,1,1258468008,1,1258468008,'',0,NULL),
 (25,'NoDeleteRightOnPageFileTemplateTest','NoDeleteRightOnPageFileTemplateTest','NoDeleteRightOnPageFileTemplateTest','6334ff1f0d52a8db82feb6dd85f5e7c4','',0,1,1,1259065498,1,1259065498,'',0,NULL),
 (26,'test','test','RoleEnglish','ddd3e973068605e111454277075f9c96','',0,1,1,1259066384,1,1259066384,'',0,NULL);
 
INSERT INTO `template` (`id`,`folder_id`,`templategroup_id`,`name`,`locked`,`locked_by`,`ml_id`,`ml`,`creator`,`cdate`,`editor`,`edate`,`description`) VALUES
 (68,5,68,'Shared Template',0,0,1,'<node html>',1,1258376014,1,1258376272,''),
 (70,13,70,'Template for Node B',0,0,1,'<node html>',1,1258376432,1,1258376432,'');

INSERT INTO `template` (`id`, `folder_id`, `templategroup_id`, `name`, `locked`, `locked_by`, `ml_id`, `ml`, `creator`, `cdate`, `editor`, `edate`, `description`) VALUES (69,5,69,'Template for Node A',0,0,1,'<node html>\r\n\r\n<node text>',1,1258376090,1,1260973964,'');
 
INSERT INTO `template_folder` (`template_id`,`folder_id`) VALUES
 (68,5),
 (68,6),
 (68,12),
 (68,11),
 (68,10),
 (68,8),
 (68,7),
 (68,15),
 (68,14),
 (68,13),
 (69,5),
 (69,6),
 (69,12),
 (69,11),
 (69,10),
 (69,8),
 (69,7),
 (70,13),
 (70,15),
 (70,14),
 (68,16),
 (70,16),
 (68,17),
 (70,17),
 (68,18),
 (70,18),
 (68,19),
 (70,19),
 (68,20),
 (70,20),
 (68,21),
 (70,21),
 (68,22),
 (70,22);
 
INSERT INTO `templategroup` (`id`,`nada`) VALUES 
 (68,0),
 (69,0),
 (70,0);

INSERT INTO `templatetag` (`id`,`templategroup_id`,`template_id`,`construct_id`,`pub`,`enabled`,`name`) VALUES 
 (646,68,68,12,1,3,'html'),
 (647,69,69,12,1,3,'html'),
 (648,70,70,14,1,3,'html');

INSERT INTO `user_group` (`user_id`,`usergroup_id`,`cdate`,`creator`) VALUES
 (20,13,1258467955,1),
 (21,14,1258467969,1),
 (22,15,1258467984,1),
 (23,16,1258467996,1),
 (24,17,1258468008,1),
 (25,18,1259065498,1),
 (26,19,1259066384,1);

INSERT INTO `usergroup` (`id`,`name`,`mother`,`creator`,`cdate`,`editor`,`edate`,`description`) VALUES
 (13,'permission1',1,1,1258467488,1,1258467488,''),
 (14,'permission2',1,1,1258467710,1,1258467710,''),
 (15,'permission3',1,1,1258467805,1,1258467805,''),
 (16,'permission4',1,1,1258467853,1,1258467853,''),
 (17,'permission5',1,1,1258467861,1,1258467861,''),
 (18,'NoDeleteRightOnPageFileTemplateTest',1,1,1259065301,1,1259065301,''),
 (19,'RoleEnglish',1,1,1259066343,1,1259066343,'');

UPDATE `value` SET info=0, value_ref=0 WHERE id IN (17031, 17146, 19686, 19690, 19687, 19688, 19689, 19692, 25923, 26288, 27147, 16391, 18102, 18898, 19254, 18893, 19654);

INSERT INTO `value` (`id`,`part_id`,`info`,`static`,`templatetag_id`,`contenttag_id`,`globaltag_id`,`objtag_id`,`value_text`,`value_ref`) VALUES
 (37000,38,0,0,0,3,0,0,'Some test content',0),
 (37001,38,0,0,0,0,0,9179,'blubb',0),
 (37002,38,0,0,0,4,0,0,'Some test content',0),
 (37003,38,0,0,0,0,0,9180,'blubb',0),
 (37004,38,0,0,0,0,0,9181,'bla',0),
 (37005,38,0,0,0,0,0,9182,'bli',0),
 (37006,38,0,0,0,0,0,9183,'blubbblubb',0),
 (37007,38,0,0,0,5,0,0,'Some test content',0),
 (37008,38,0,0,0,6,0,0,'Some test content',0),
 (37009,38,0,0,0,0,0,9184,'blubbblubb',0),
 (37010,38,0,0,0,0,0,9185,'blubbblubb',0),
 (37011,38,0,0,0,0,0,9186,'blubbblubb',0),
 (37012,38,0,0,0,0,0,9187,'blubbblubb',0),
 (37013,38,0,0,0,0,0,9188,'blubbblubb',0),
 (37014,38,0,0,0,0,0,9189,'blubbblubb',0),
 (37015,44,0,0,0,7,0,0,'Some test content',0),
 (37016,38,0,0,0,0,0,9190,'adsfasdf',0),
 (37017,38,0,0,0,0,0,9191,'dfasdf',0),
 (37018,38,0,0,0,0,0,9192,'adsfasdf',0),
 (37019,38,0,0,0,0,0,9193,'adsfadsf',0),
 (37020,38,0,0,0,0,0,9194,'asdfasdf',0),
 (37021,38,0,0,0,0,0,9195,'asdfasdf',0),
 (37022,44,0,0,0,8,0,0,'<node dsoverview1>\r\n  <node name>\r\n</node dsoverview1>',0),
 (37023,71,0,0,0,9,0,0,'',0),
 (37024,44,0,0,0,10,0,0,'<node vtl1>',0),
 (37025,521,0,0,0,11,0,0,'#set($folder = $cms.imps.loader.getFolder(7))\r\n$folder.name',0),
 (37026,44,0,0,0,12,0,0,'<node dsoverview1>\r\n  <node page.name>\r\n</node dsoverview1>',0),
 (37027,71,0,0,0,13,0,0,'',0),
 (37028,44,0,0,0,14,0,0,'<node vtl1>',0),
 (37029,521,0,0,0,15,0,0,'#set($page = $cms.imps.loader.getPage(3))\r\n$page.name',0),
 (37030,44,0,0,0,16,0,0,'<node seitenurl1>',0),
 (37031,272,1,0,0,17,0,0,'',3),
 (37032,44,0,0,0,18,0,0,'<node seitentag1>',0),
 (37033,45,3,0,0,19,0,0,'p',3),
 (37034,44,0,0,0,20,0,0,'<node dsoverview1>\r\n  <node file.name>\r\n</node dsoverview1>',0),
 (37035,71,0,0,0,21,0,0,'',0),
 (37036,44,0,0,0,22,0,0,'<node vtl1>',0),
 (37037,521,0,0,0,23,0,0,'#set($file = $cms.imps.loader.getFile(3))\r\n$file.name',0),
 (37038,44,0,0,0,24,0,0,'<node dateiurl1>',0),
 (37039,274,0,0,0,25,0,0,'',3),
 (37040,44,0,0,0,26,0,0,'<node dsoverview1>\r\n  <node image.name>\r\n</node dsoverview1>',0),
 (37041,71,0,0,0,27,0,0,'',0),
 (37042,44,0,0,0,28,0,0,'<node vtl1>',0),
 (37043,521,0,0,0,29,0,0,'#set($image = $cms.imps.loader.getImage(5))\r\n$image.name',0),
 (37044,44,0,0,0,30,0,0,'<node bildurl1>',0),
 (37045,273,0,0,0,31,0,0,'',5),
 (37046,44,0,0,0,32,0,0,'<node templatetag1>',0),
 (37047,898,69,0,0,33,0,0,'t',647),
 (37048,44,0,0,0,34,0,0,'<node vtl1>',0),
 (37049,521,0,0,0,35,0,0,'#set($template = $cms.imps.loader.getObject(10006, 69))\r\n$template.name',0),
 (37050,44,0,0,0,36,0,0,'',0),
 (37051,44,0,0,0,37,0,0,'',0),
 (37052,38,0,0,0,38,0,0,'',0);

INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37057,38,0,0,0,42,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37058,44,0,0,0,43,0,0,'<node bread1>\r\n<node liste1>\r\n<node download1>\r\n<node googlemap1>\r\n<node form1>\r\n<node nav11>\r\n<node gallerie1>\r\n<node seitentag1>\r\n<node link1>\r\n<node tabext1>\r\n<node templatetag1>\r\n<node lightboximage1>\r\n<node parttypetest1>',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37059,885,0,0,0,44,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37060,886,0,0,0,44,0,0,'1',15);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37061,887,0,0,0,44,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37062,549,0,0,0,45,0,0,'',5);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37063,551,0,0,0,45,0,0,'1',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37064,550,0,0,0,45,0,0,'test',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37065,79,0,0,0,46,0,0,'adsf\r\nadsf\r\ndasfadsf',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37066,272,0,0,0,47,0,0,'#',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37067,250,0,0,0,48,0,0,'',3);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37068,649,0,0,0,49,0,0,'ABQIAAAAvlWGtZGTvlUSxP5Vj8IB9BQTyGTscSzC9OpCk1FeVSge0LC5axQsqaA4kCIfhJLYgkbM74bF792E4Q',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37069,650,0,0,0,49,0,0,'ABQIAAAAvlWGtZGTvlUSxP5Vj8IB9BSvWGUKwJeV7KejCX44x9Y9KvixdRTf0CgtFCQxayBAhcOTEhIA5Bvdeg',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37070,651,0,0,0,49,0,0,'48.215487',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37071,652,0,0,0,49,0,0,'16.370251',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37072,653,0,0,0,49,0,0,'17',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37073,654,0,0,0,49,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37074,655,0,0,0,49,0,0,'113',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37075,656,0,0,0,49,0,0,'26',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37076,657,0,0,0,49,0,0,'48.215487',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37077,658,0,0,0,49,0,0,'16.369051',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37078,659,0,0,0,49,0,0,'Gentics Software GmbH',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37079,660,0,0,0,49,0,0,'Gonzagagasse 11/25<br />A-1010 Wien<br />Austria<br /><br />Tel.: +43 1 710 99 04 - 0<br />Fax.: +43 1 710 99 04 - 4',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37080,661,0,0,0,49,0,0,'500',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37081,662,0,0,0,49,0,0,'500',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37082,336,0,0,0,50,0,0,'adsfa',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37083,337,0,0,0,50,0,0,'sdfasf',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37084,338,0,0,0,50,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37085,339,0,0,0,50,0,0,'sdfasdf\r\ndsf\r\nasdf\r\nsdaf\r\nsadf',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37086,340,0,0,0,50,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37087,318,0,0,0,51,0,0,'Senden',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37088,319,0,0,0,51,0,0,'Abbrechen',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37089,320,0,0,0,51,0,0,'<font style=\"color:red\">',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37090,321,0,0,0,51,0,0,'</font>',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37091,322,0,0,0,51,0,0,'Bitte füllen Sie alle Pflichtfelder aus!',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37092,323,0,0,0,51,0,0,'danke.',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37093,324,0,0,0,51,0,0,'test@test.com',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37094,325,0,0,0,51,0,0,'test@test.com',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37095,326,0,0,0,51,0,0,'test@test.com',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37096,327,0,0,0,51,0,0,'\r\n<node checkbox1>',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37097,566,0,0,0,52,0,0,'',6);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37098,567,0,0,0,52,0,0,'',11);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37099,568,0,0,0,52,0,0,'-9999',4);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37100,569,0,0,0,52,0,0,'-9999',3);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37101,570,0,0,0,52,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37102,571,0,0,0,52,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37103,566,0,0,0,53,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37104,567,0,0,0,53,0,0,'',11);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37105,568,0,0,0,53,0,0,'',4);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37106,569,0,0,0,53,0,0,'',3);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37107,570,0,0,0,53,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37108,571,0,0,0,53,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37109,565,0,0,0,52,0,0,'asdfsdafasdfasdfasdfasdfsad',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37110,877,0,0,0,54,0,0,'<img src=\"/GenticsImageStore/100/50/prop/<node file.url>\">',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37111,45,3,0,0,55,0,0,'p',3);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37112,2,0,0,0,56,0,0,'google.com',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37113,4,0,0,0,56,0,0,'Google',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37114,628,14,0,0,57,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37115,629,0,0,0,57,0,0,'0',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37116,630,0,0,0,57,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37117,631,0,0,0,57,0,0,'top',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37118,632,0,0,0,57,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37119,633,0,0,0,57,0,0,'left',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37120,634,0,0,0,57,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37121,628,14,0,0,58,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37122,632,0,0,0,58,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37123,628,14,0,0,60,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37124,630,0,0,0,60,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37125,631,0,0,0,60,0,0,'top',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37126,633,0,0,0,60,0,0,'left',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37127,634,0,0,0,60,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37128,628,14,0,0,62,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37129,630,0,0,0,62,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37130,631,0,0,0,62,0,0,'top',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37131,633,0,0,0,62,0,0,'left',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37132,634,0,0,0,62,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37133,628,14,0,0,64,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37134,630,0,0,0,64,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37135,631,0,0,0,64,0,0,'top',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37136,633,0,0,0,64,0,0,'left',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37137,634,0,0,0,64,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37138,628,14,0,0,65,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37139,632,0,0,0,65,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37140,628,14,0,0,67,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37141,630,0,0,0,67,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37142,631,0,0,0,67,0,0,'top',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37143,633,0,0,0,67,0,0,'left',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37144,634,0,0,0,67,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37145,628,14,0,0,69,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37146,630,0,0,0,69,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37147,631,0,0,0,69,0,0,'top',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37148,633,0,0,0,69,0,0,'left',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37149,634,0,0,0,69,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37150,628,14,0,0,71,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37151,630,0,0,0,71,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37152,631,0,0,0,71,0,0,'top',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37153,633,0,0,0,71,0,0,'left',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37154,634,0,0,0,71,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37155,628,14,0,0,72,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37156,632,0,0,0,72,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37157,628,14,0,0,74,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37158,630,0,0,0,74,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37159,631,0,0,0,74,0,0,'top',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37160,633,0,0,0,74,0,0,'left',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37161,634,0,0,0,74,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37162,628,14,0,0,76,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37163,630,0,0,0,76,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37164,631,0,0,0,76,0,0,'top',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37165,633,0,0,0,76,0,0,'left',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37166,634,0,0,0,76,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37167,628,14,0,0,78,0,0,'3;3',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37168,630,0,0,0,78,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37169,631,0,0,0,78,0,0,'top',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37170,633,0,0,0,78,0,0,'left',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37171,634,0,0,0,78,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37172,44,0,0,0,59,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37173,44,0,0,0,61,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37174,44,0,0,0,63,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37175,44,0,0,0,66,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37176,44,0,0,0,68,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37177,44,0,0,0,70,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37178,44,0,0,0,73,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37179,44,0,0,0,75,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37180,44,0,0,0,77,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37181,898,68,0,0,79,0,0,'t',646);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37182,641,0,0,0,80,0,0,'',6);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37183,642,0,0,0,80,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37184,644,0,0,0,80,0,0,'0',15);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37185,900,0,0,0,0,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37186,901,0,0,0,0,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37187,902,0,0,0,0,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37188,903,0,0,0,0,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37189,904,0,0,0,0,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37191,906,0,0,0,0,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37192,907,0,0,0,0,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37201,885,0,0,0,81,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37202,886,0,0,0,81,0,0,'1',15);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37203,887,0,0,0,81,0,0,'',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37204,900,0,0,0,82,0,0,'',33);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37205,901,0,0,0,82,0,0,'alert(\'yo\');',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37206,902,0,0,0,82,0,0,'\\\\servername\\dir\\file.txt',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37207,903,0,0,0,82,0,0,'70',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37208,904,0,0,0,82,0,0,'160',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37209,906,0,0,0,82,0,0,'geordnete liste',0);
INSERT INTO `value` (`id`, `part_id`, `info`, `static`, `templatetag_id`, `contenttag_id`, `globaltag_id`, `objtag_id`, `value_text`, `value_ref`) VALUES (37210,907,0,0,0,82,0,0,'ungeordnete liste',0);