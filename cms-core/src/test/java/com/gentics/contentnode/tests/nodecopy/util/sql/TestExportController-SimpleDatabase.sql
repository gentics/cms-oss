INSERT INTO `mappedglobalidsequence` VALUES
 (1);

INSERT INTO `page` (`id`, `name`, `description`, `filename`, `priority`, `status`, `time_start`, `time_end`, `time_mon`, `time_tue`, `time_wed`, `time_thu`, `time_fri`, `time_sat`, `time_sun`, `content_id`, `template_id`, `folder_id`, `creator`, `cdate`, `editor`, `edate`, `pdate`, `publisher`, `time_pub`, `contentgroup_id`, `contentset_id`, `delay_publish`) VALUES 
 (1, 'Page_With_Template', 'This page is used to test DeepCopy=true ', 'Page_with_Template',									 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
 (2, 'Second_Page', 'Just another page in Var_Folder', 'Second_Page',														     1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
 (3, 'First_Page_From_Template', 'This is a page which was created from an template', 'first_page_from_template',                1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
 (4, 'Second_Page_From_Template', 'This is a page which was created by using a template', 'second_page_from_template',           1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
 (5, 'Page_With_Contentgroup', 'This is a page with a contentgroup to test DeepCopyAsk=True', 'page_with_contentgroup',          1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
 (6, 'Page_Without_Contentgroup','This is a page with a contentgroup to test DeepCopyAsk=True', 'page_without_contentgroup',     1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

INSERT INTO `template` (`id`, `folder_id`, `templategroup_id`, `name`, `locked`, `locked_by`, `ml_id`, `ml`, `creator`, `cdate`, `editor`, `edate`, `description`) VALUES 
 (1, 3, 0, 'Template_In_Usr_Folder', 0, 0, 0, '', 0, 0, 0, 0, ''),
 (2, 4, 0, 'Template_In_Var_Folder', 0, 0, 0, '', 0, 0, 0, 0, ''),
 (3, 4, 0, 'Template_In_Var_Folder for Page_With_Template', 0, 0, 0, '', 0, 0, 0, 0, '');

INSERT INTO `content` VALUES  
 (1,0,0,0,0,0,0,0);

INSERT INTO `contentgroup` VALUES  
 (1,'this is a contentgroup','');

INSERT INTO `folder` VALUES 
 (1,0,'ROOT',10001,'',0,0,0,0,0,'',0);

INSERT INTO `folder` VALUES
 (2,1,'Empty_Folder',10002,'',0,0,0,0,0,'',0);

INSERT INTO `folder` VALUES
 (3,1,'Usr_Folder',10002,'',0,0,0,0,0,'',0);

INSERT INTO `folder` VALUES
 (4,1,'Var_Folder',10002,'',0,0,0,0,0,'',0);

INSERT INTO `folder` VALUES
 (5,1,'Home_Folder',10002,'',0,0,0,0,0,'',0);

INSERT INTO `bundlecontainedobject` VALUES
 (1,1,10002,2,0,0,0,0,0,0,0,1),
 (2,2,10007,1,0,0,0,0,0,0,0,2),
 (3,3,10006,2,0,0,0,0,0,0,0,3),
 (4,4,10007,5,0,0,0,0,0,0,0,4),
 (5,5,10007,6,0,0,0,0,0,0,0,5),
 (6,6,10002,2,0,0,0,0,0,0,0,6),
 (7,7,10002,4,0,0,0,0,0,0,0,7),
 (8,8,10006,1,0,0,0,0,0,0,0,8);

INSERT INTO `bundle` VALUES
 (1,'myprefix',0,'Test1','testExport() - General Export Test - (EmptyFolder)'          			,'',0,0,0,0,0,0,0,0,'','',-1),
 (2,'myprefix',0,'Test2','testExportDeepCopyTrue() - Case 1 - Export page with template'			,'',0,0,0,0,0,0,0,0,'','',-1),
 (3,'myprefix',0,'Test3','testExportDeepCopyFalse() - Case 2 - Export of template'     			,'',0,0,0,0,0,0,0,0,'','',-1),
 (4,'myprefix',0,'Test4','testExportDeepCopyAsk() - Case 1 - Export of page with contengroup'	,'',0,0,0,0,0,0,0,0,'','',-1),
 (5,'myprefix',0,'Test5','testExportDeepCopyAsk() - Case 2 - Export of pahe without contentgroup'    	  	,'',0,0,0,0,0,0,0,0,'','',-1),
 (6,'myprefix',0,'Test6','testExportForeignDeepCopyTrue() - Case 1 - Export of empty folder'	,'',0,0,0,0,0,0,0,0,'','',-1),
 (7,'myprefix',0,'Test7','testExportForeignDeepCopyTrue() - Case 2 - Export of folder that contains two pages','',0,0,0,0,0,0,0,0,'','',-1),
 (8,'myprefix',0,'Test8','testExportForeignDeepCopyFalse() - Case 1 - Export of template'		,'',0,0,0,0,0,0,0,0,'','',-1);

INSERT INTO `bundlebuild` VALUES
 (1,1,'',0,'',0),
 (2,2,'',0,'',0),
 (3,3,'',0,'',0),
 (4,4,'',0,'',0),
 (5,5,'',0,'',0),
 (6,6,'',0,'',0),
 (7,7,'',0,'',0),
 (8,8,'',0,'',0);