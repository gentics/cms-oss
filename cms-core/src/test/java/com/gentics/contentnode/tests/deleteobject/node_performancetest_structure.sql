SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `autoupdatelog` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `date` datetime NOT NULL,
  `updatefile` varchar(255) NOT NULL,
  `status` varchar(45) NOT NULL,
  `user_id` int(11) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=8 DEFAULT CHARSET=latin1;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `bug` (
  `id` int(11) NOT NULL auto_increment,
  `logerror_id` int(11) NOT NULL default '0',
  `user_id` int(11) NOT NULL default '0',
  `description` text NOT NULL,
  `supposed` text NOT NULL,
  `timestamp` int(11) NOT NULL default '0',
  `fixer` varchar(32) NOT NULL default '',
  `fixtime` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `bundle` (
  `id` int(11) NOT NULL auto_increment,
  `globalprefix` varchar(255) default NULL,
  `globalid` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` mediumtext NOT NULL,
  `password` varchar(255) NOT NULL,
  `isimported` int(11) NOT NULL default '0',
  `keepoldbuilds` int(11) NOT NULL default '0',
  `enabledownload` int(11) NOT NULL default '0',
  `creator` int(11) default NULL,
  `editor` int(11) default NULL,
  `edate` int(11) default NULL,
  `cdate` int(11) default NULL,
  `importfolder_id` int(11) default NULL,
  `updateurl` varchar(255) default NULL,
  `source` varchar(255) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `bundlebuild` (
  `id` int(11) NOT NULL auto_increment,
  `bundle_id` int(11) NOT NULL,
  `changelog` mediumtext NOT NULL,
  `date` int(11) NOT NULL,
  `filename` varchar(255) default NULL,
  `statuscode` int(11) NOT NULL default '-1',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `bundlecontainedobject` (
  `id` int(11) NOT NULL auto_increment,
  `bundle_id` int(11) NOT NULL,
  `obj_type` int(11) NOT NULL,
  `obj_id` int(11) NOT NULL,
  `referrer_obj_id` int(11) NOT NULL,
  `accepted` int(11) NOT NULL default '0',
  `referrer_obj_type` int(11) NOT NULL,
  `autoadded` int(11) NOT NULL default '0',
  `cause_obj_type` int(11) default NULL,
  `cause_obj_id` int(11) default NULL,
  `excluded` int(11) NOT NULL default '0',
  `bundlebuild_id` int(11) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `bundleimport` (
  `id` int(11) NOT NULL auto_increment,
  `user_id` int(11) NOT NULL,
  `date` int(11) NOT NULL,
  `bundlebuild_id` int(11) NOT NULL,
  `group_id` int(11) NOT NULL default '-1',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `bundleimportconflict` (
  `id` int(11) NOT NULL auto_increment,
  `bundleimportobject_id` int(11) NOT NULL,
  `name` varchar(255) default NULL,
  `reason_obj_type` int(11) default NULL,
  `reason_obj_id` int(11) default NULL,
  `reason` varchar(255) default NULL,
  `conflict_behaviour` varchar(255) default NULL,
  `recoverable` int(11) NOT NULL default '1',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `bundleimportobject` (
  `id` int(11) NOT NULL auto_increment,
  `bundleimport_id` int(11) NOT NULL,
  `obj_type` int(11) NOT NULL,
  `action` varchar(255) default NULL,
  `udate` int(11) NOT NULL,
  `globalprefix` varchar(255) default NULL,
  `globalid` int(11) default NULL,
  `copy_id` int(11) NOT NULL default '0',
  `maxudate` int(11) NOT NULL default '-1',
  `numobjects` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `cn_folder_map` (
  `folder_id` int(11) NOT NULL default '0',
  `cn_map_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`folder_id`,`cn_map_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `cn_map` (
  `id` int(11) NOT NULL auto_increment,
  `name` char(255) NOT NULL default '',
  `keyword` char(255) NOT NULL default '',
  `description` char(255) NOT NULL default '',
  `open_target` char(255) default NULL,
  `page_id` int(11) NOT NULL default '0',
  `is_link_int` tinyint(4) default NULL,
  `node_id` int(11) NOT NULL default '0',
  `mother_id` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `cn_mapprop` (
  `id` int(11) NOT NULL auto_increment,
  `cn_map_id` int(11) NOT NULL default '0',
  `ptype` int(11) default NULL,
  `pkey` varchar(100) default NULL,
  `value` text,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `cnstatconfig` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) NOT NULL default '',
  `statkeys` text NOT NULL,
  `active` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `cnstatval` (
  `id` int(11) NOT NULL auto_increment,
  `cnstatconfig_id` int(11) NOT NULL default '0',
  `timestamp` int(11) NOT NULL default '0',
  `field` varchar(255) NOT NULL default '',
  `col` int(11) NOT NULL default '0',
  `textvalue` text NOT NULL,
  `intvalue` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `cnstatconfig_id` (`cnstatconfig_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `construct` (
  `id` int(11) NOT NULL auto_increment,
  `name_id` int(11) NOT NULL default '0',
  `ml_id` int(11) NOT NULL default '1',
  `keyword` char(64) NOT NULL default '',
  `childable` tinyint(4) NOT NULL default '0',
  `intext` tinyint(4) NOT NULL default '0',
  `locked` int(11) NOT NULL default '0',
  `locked_by` int(11) NOT NULL default '0',
  `global` int(11) NOT NULL default '0',
  `icon` char(64) NOT NULL default '',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `description_id` int(11) NOT NULL default '0',
  `autoenable` int(11) NOT NULL default '0',
  `category_id` int(11) default NULL,
  `hopedithook` mediumtext,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `construct_category` (
  `id` int(11) NOT NULL auto_increment,
  `name_id` int(11) NOT NULL,
  `sortorder` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `construct_node` (
  `construct_id` int(11) NOT NULL default '0',
  `node_id` int(11) NOT NULL default '0',
  KEY `construct_id` (`construct_id`),
  KEY `node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `content` (
  `id` int(11) NOT NULL auto_increment,
  `node_id` int(11) NOT NULL default '0',
  `locked` int(11) NOT NULL default '0',
  `locked_by` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `contentfile` (
  `id` int(11) NOT NULL auto_increment,
  `name` char(255) NOT NULL default '',
  `filetype` varchar(255) default NULL,
  `folder_id` int(11) NOT NULL default '0',
  `filesize` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `description` char(255) NOT NULL default '',
  `sizex` int(11) NOT NULL default '0',
  `sizey` int(11) NOT NULL default '0',
  `md5` char(32) NOT NULL default '',
  `dpix` int(11) NOT NULL default '0',
  `dpiy` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `name` (`name`),
  KEY `folder_id` (`folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `contentfiledata` (
  `id` int(11) NOT NULL auto_increment,
  `contentfile_id` int(11) NOT NULL,
  `binarycontent` longblob,
  PRIMARY KEY  (`id`),
  KEY `contentfile_id` (`contentfile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `contentgroup` (
  `id` int(11) NOT NULL auto_increment,
  `name` char(255) NOT NULL default '',
  `code` char(5) NOT NULL default '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `contentrepository` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) NOT NULL,
  `dbtype` varchar(255) NOT NULL default '',
  `username` varchar(255) NOT NULL default '',
  `password` varchar(255) NOT NULL default '',
  `url` varchar(255) NOT NULL default '',
  `checkdate` int(11) NOT NULL default '0',
  `checkstatus` int(11) NOT NULL default '-1',
  `checkresult` text,
  `statusdate` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `contentset` (
  `id` int(11) NOT NULL auto_increment,
  `nada` tinyint(4) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `contenttable` (
  `templatetag_id` int(11) NOT NULL default '0',
  `contenttag_id` int(11) NOT NULL default '0',
  `globaltag_id` int(11) NOT NULL default '0',
  `objtag_id` int(11) NOT NULL default '0',
  `part_id` int(11) NOT NULL default '0',
  `th` tinyint(4) NOT NULL default '0',
  `td_f_even` text NOT NULL,
  `td_f_odd` text NOT NULL,
  `th_f` text NOT NULL,
  `td_type` int(11) NOT NULL default '0',
  `th_type` int(11) NOT NULL default '0',
  `table_f` text NOT NULL,
  KEY `templatetag_id` (`templatetag_id`),
  KEY `contenttag_id` (`contenttag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `contenttag` (
  `id` int(11) NOT NULL auto_increment,
  `content_id` int(11) NOT NULL default '0',
  `construct_id` int(11) NOT NULL default '0',
  `enabled` tinyint(4) NOT NULL default '0',
  `name` char(127) NOT NULL default '',
  `unused` tinyint(4) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `content_id` (`content_id`),
  KEY `construct_id` (`construct_id`),
  KEY `name` (`name`),
  KEY `content_id_2` (`content_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `contenttag_nodeversion` (
  `id` int(11) NOT NULL default '0',
  `content_id` int(11) NOT NULL default '0',
  `construct_id` int(11) NOT NULL default '0',
  `enabled` tinyint(4) NOT NULL default '0',
  `name` char(127) NOT NULL default '',
  `nodeversiontimestamp` int(11) NOT NULL default '0',
  `nodeversion_user` int(11) NOT NULL default '0',
  `nodeversionlatest` int(11) NOT NULL default '0',
  `nodeversionremoved` int(11) NOT NULL default '0',
  `unused` tinyint(4) NOT NULL default '0',
  KEY `id` (`id`),
  KEY `content_id` (`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `datasource` (
  `id` int(11) NOT NULL auto_increment,
  `source_type` int(11) NOT NULL default '0',
  `name` varchar(255) default NULL,
  `param_id` int(11) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `datasource_value` (
  `id` int(11) NOT NULL auto_increment,
  `datasource_id` int(11) NOT NULL default '0',
  `sorder` int(11) default NULL,
  `dskey` varchar(255) default NULL,
  `value` text,
  `dsid` int(11) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `dependencymap` (
  `id` int(11) NOT NULL auto_increment,
  `mod_type` int(11) NOT NULL default '0',
  `mod_id` int(11) NOT NULL default '0',
  `mod_event_type` int(11) default '-1',
  `dep_type` int(11) NOT NULL default '0',
  `dep_id` int(11) NOT NULL default '0',
  `link_type` int(11) NOT NULL default '0',
  `ignore_events` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `mod_type` (`mod_type`,`mod_id`),
  KEY `dep_type` (`dep_type`,`dep_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `dependencymap2` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `mod_obj_type` int(10) unsigned default NULL,
  `mod_obj_id` int(10) unsigned default NULL,
  `mod_ele_type` int(10) unsigned default NULL,
  `mod_ele_id` int(10) unsigned default NULL,
  `mod_prop` varchar(255) default NULL,
  `dep_obj_type` int(10) unsigned default NULL,
  `dep_obj_id` int(10) unsigned default NULL,
  `dep_ele_type` int(10) unsigned default NULL,
  `dep_ele_id` int(10) unsigned default NULL,
  `eventmask` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `mod_obj` (`mod_obj_type`,`mod_obj_id`),
  KEY `mod_ele` (`mod_ele_type`,`mod_ele_id`),
  KEY `dep_obj` (`dep_obj_type`,`dep_obj_id`),
  KEY `dep_ele` (`dep_ele_type`,`dep_ele_id`),
  KEY `dep_obj_type` (`dep_obj_type`,`dep_obj_id`,`dep_ele_type`,`dep_ele_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `dicuser` (
  `output_id` int(11) NOT NULL default '0',
  `language_id` int(11) NOT NULL default '0',
  `value` text NOT NULL,
  `id` int(11) NOT NULL auto_increment,
  PRIMARY KEY  (`id`),
  KEY `value` (`value`(40),`language_id`),
  KEY `output_id` (`output_id`,`language_id`),
  KEY `output_id_2` (`output_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `dirtanalysis` (
  `id` int(11) NOT NULL auto_increment,
  `sid` varchar(15) NOT NULL,
  `finished` tinyint(4) NOT NULL default '0',
  `timestamp` int(11) NOT NULL,
  `analysis` mediumtext,
  `jstree_analysis` mediumtext,
  `dirted` int(11) NOT NULL default '0',
  `obj_type` int(11) NOT NULL default '0',
  `obj_id` int(11) NOT NULL default '0',
  `action` varchar(255) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `dirtqueue` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `timestamp` int(10) unsigned NOT NULL default '0',
  `obj_type` int(10) unsigned default NULL,
  `obj_id` int(10) unsigned default NULL,
  `events` int(10) unsigned default NULL,
  `property` mediumtext,
  `simulation` tinyint(4) default '0',
  `sid` varchar(15) default NULL,
  PRIMARY KEY  (`id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `ds` (
  `id` int(11) NOT NULL auto_increment,
  `templatetag_id` int(11) NOT NULL default '0',
  `contenttag_id` int(11) NOT NULL default '0',
  `globaltag_id` int(11) NOT NULL default '0',
  `o_type` int(11) NOT NULL default '0',
  `is_folder` tinyint(4) NOT NULL default '0',
  `orderkind` tinyint(4) NOT NULL default '0',
  `orderway` tinyint(4) NOT NULL default '0',
  `max_obj` int(11) NOT NULL default '0',
  `recursion` tinyint(4) NOT NULL default '0',
  `objtag_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `templatetag_id` (`templatetag_id`),
  KEY `objtag_id` (`objtag_id`),
  KEY `is_folder` (`is_folder`),
  KEY `contenttag_id` (`contenttag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `ds_nodeversion` (
  `id` int(11) NOT NULL default '0',
  `templatetag_id` int(11) NOT NULL default '0',
  `contenttag_id` int(11) NOT NULL default '0',
  `globaltag_id` int(11) NOT NULL default '0',
  `o_type` int(11) NOT NULL default '0',
  `is_folder` tinyint(4) NOT NULL default '0',
  `orderkind` tinyint(4) NOT NULL default '0',
  `orderway` tinyint(4) NOT NULL default '0',
  `max_obj` int(11) NOT NULL default '0',
  `recursion` tinyint(4) NOT NULL default '0',
  `nodeversiontimestamp` int(11) NOT NULL default '0',
  `nodeversion_user` int(11) NOT NULL default '0',
  `nodeversionlatest` int(11) NOT NULL default '0',
  `nodeversionremoved` int(11) NOT NULL default '0',
  `objtag_id` int(11) NOT NULL default '0',
  KEY `id` (`id`),
  KEY `contenttag_id` (`contenttag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `ds_obj` (
  `ds_id` int(11) NOT NULL default '0',
  `templatetag_id` int(11) NOT NULL default '0',
  `contenttag_id` int(11) NOT NULL default '0',
  `globaltag_id` int(11) NOT NULL default '0',
  `o_id` int(11) NOT NULL default '0',
  `obj_order` smallint(6) NOT NULL default '0',
  `id` int(11) NOT NULL auto_increment,
  `adate` int(11) NOT NULL default '0',
  `auser` int(11) NOT NULL default '0',
  `objtag_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `o_id` (`o_id`),
  KEY `contenttag_id` (`contenttag_id`),
  KEY `templatetag_id` (`templatetag_id`),
  KEY `objtag_id` (`objtag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `ds_obj_nodeversion` (
  `id` int(11) NOT NULL default '0',
  `ds_id` int(11) NOT NULL default '0',
  `templatetag_id` int(11) NOT NULL default '0',
  `contenttag_id` int(11) NOT NULL default '0',
  `globaltag_id` int(11) NOT NULL default '0',
  `o_id` int(11) NOT NULL default '0',
  `obj_order` smallint(6) NOT NULL default '0',
  `nodeversiontimestamp` int(11) NOT NULL default '0',
  `nodeversion_user` int(11) NOT NULL default '0',
  `nodeversionlatest` int(11) NOT NULL default '0',
  `nodeversionremoved` int(11) NOT NULL default '0',
  `adate` int(11) NOT NULL default '0',
  `auser` int(11) NOT NULL default '0',
  `objtag_id` int(11) NOT NULL default '0',
  KEY `id` (`id`),
  KEY `contenttag_id` (`contenttag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `eventprop` (
  `id` int(11) NOT NULL auto_increment,
  `triggerevent_id` int(11) NOT NULL default '0',
  `keyword` varchar(255) NOT NULL default '',
  `value` varchar(255) NOT NULL default '',
  `info` text NOT NULL,
  `mapkey` varchar(255) NOT NULL default '',
  `workflowlink_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `eventpropeditable` (
  `id` int(11) NOT NULL auto_increment,
  `eventprop_id` int(11) NOT NULL default '0',
  `description` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `faqcontent` (
  `id` int(11) NOT NULL auto_increment,
  `name` text,
  `description` text,
  `maintext` text,
  `keywords` text,
  `url` text,
  `folder_id` int(11) default NULL,
  `foldername` varchar(255) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `faqinfo` (
  `faq_id` int(11) default NULL,
  `visited` int(11) default NULL,
  `time` int(11) default NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `faqsearch` (
  `search` varchar(255) default NULL,
  `counter` int(11) default NULL,
  `timestamp` int(11) default NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `faqvote` (
  `faq_id` int(11) default NULL,
  `vote` int(11) default NULL,
  `time` int(11) default NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `folder` (
  `id` int(11) NOT NULL auto_increment,
  `mother` int(11) NOT NULL default '0',
  `name` char(255) NOT NULL default '',
  `type_id` smallint(6) NOT NULL default '0',
  `pub_dir` char(255) NOT NULL default '',
  `node_id` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `description` char(255) NOT NULL default '',
  `startpage_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `mother` (`mother`),
  KEY `type_id` (`type_id`),
  KEY `node_id` (`node_id`),
  KEY `startpage_id` (`startpage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `globaltag` (
  `id` int(11) NOT NULL auto_increment,
  `construct_id` int(11) NOT NULL default '0',
  `name` char(255) NOT NULL default '',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `description` char(255) NOT NULL default '',
  `enabled` tinyint(4) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `globaltag_node` (
  `globaltag_id` int(11) NOT NULL default '0',
  `node_id` int(11) NOT NULL default '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `halt` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) NOT NULL default '',
  `description` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `imgresizecache` (
  `id` int(11) NOT NULL auto_increment,
  `filename` varchar(255) NOT NULL default '',
  `normprops` text NOT NULL,
  `keyprop` text NOT NULL,
  `cachefile` varchar(255) NOT NULL default '',
  PRIMARY KEY  (`id`),
  KEY `filename` (`filename`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `import_page` (
  `id` int(11) NOT NULL auto_increment,
  `import_id` int(11) NOT NULL default '0',
  `page_id` int(11) NOT NULL default '0',
  `url` varchar(255) NOT NULL default '',
  `type` varchar(5) NOT NULL default '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `import_part` (
  `id` int(11) NOT NULL auto_increment,
  `import_page_id` int(11) NOT NULL default '0',
  `tag_id` int(11) NOT NULL default '0',
  `part_id` int(11) NOT NULL default '0',
  `type_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `invokerqueue` (
  `id` int(11) NOT NULL auto_increment,
  `type` varchar(20) NOT NULL,
  `idparam` varchar(200) NOT NULL,
  `additionalparams` varchar(255) NOT NULL,
  `date` int(11) NOT NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `type` (`type`,`idparam`),
  KEY `date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `job` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) default NULL,
  `description` text,
  `task_id` int(11) NOT NULL default '0',
  `schedule_type` varchar(20) default NULL,
  `schedule_data` text,
  `status` tinyint(4) default NULL,
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) default NULL,
  `edate` int(11) default NULL,
  `parallel` tinyint(1) NOT NULL default '0',
  `failedemails` varchar(255) NOT NULL default '',
  `last_valid_jobrun_id` int(11) default NULL,
  `jobruncount` int(11) NOT NULL default '0',
  `jobrunaverage` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `jobdependency` (
  `id` int(11) NOT NULL auto_increment,
  `jobid` int(11) default NULL,
  `depid` int(11) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `jobrun` (
  `id` int(11) NOT NULL auto_increment,
  `job_id` int(11) default NULL,
  `starttime` int(11) default NULL,
  `endtime` int(11) default NULL,
  `returnvalue` int(11) default NULL,
  `valid` tinyint(4) default NULL,
  `output` text,
  PRIMARY KEY  (`id`),
  KEY `job_id` (`job_id`,`valid`),
  KEY `job_id_2` (`job_id`,`valid`,`endtime`),
  KEY `job_id_3` (`job_id`,`valid`,`starttime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `language` (
  `id` int(11) NOT NULL auto_increment,
  `name` char(255) default NULL,
  `active` int(11) NOT NULL default '1',
  `short` char(2) default '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `logcmd` (
  `id` int(11) NOT NULL auto_increment,
  `user_id` int(11) NOT NULL default '0',
  `cmd_desc_id` int(11) NOT NULL default '0',
  `o_type` int(11) NOT NULL default '0',
  `o_id` int(11) NOT NULL default '0',
  `o_id2` int(11) NOT NULL default '0',
  `timestamp` int(11) NOT NULL default '0',
  `info` varchar(255) NOT NULL default '',
  PRIMARY KEY  (`id`),
  KEY `user_id` (`user_id`),
  KEY `o_type` (`o_type`),
  KEY `user_id_2` (`user_id`,`o_type`),
  KEY `o_id` (`o_id`),
  KEY `o_id_2` (`o_id`,`o_type`,`timestamp`,`cmd_desc_id`),
  KEY `o_id_3` (`o_id`,`o_type`,`timestamp`),
  KEY `timestamp` (`timestamp`,`cmd_desc_id`,`o_type`),
  KEY `cmd_desc_id` (`cmd_desc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `logerror` (
  `id` int(11) NOT NULL auto_increment,
  `sid` varchar(15) NOT NULL default '',
  `user_id` int(11) NOT NULL default '0',
  `halt_id` int(11) NOT NULL default '0',
  `request` text NOT NULL,
  `errordo` int(11) NOT NULL default '0',
  `timestamp` int(11) NOT NULL default '0',
  `detail` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `logevent` (
  `id` int(11) NOT NULL auto_increment,
  `logevent_id` int(11) NOT NULL default '0',
  `timestamp_start` int(11) NOT NULL default '0',
  `timestamp_end` int(11) NOT NULL default '0',
  `timestamp_request` int(11) NOT NULL default '0',
  `user_id` int(11) NOT NULL default '0',
  `do` int(11) NOT NULL default '0',
  `magic` int(11) NOT NULL default '0',
  `module` varchar(16) NOT NULL default '',
  `obj_type` int(11) NOT NULL default '0',
  `obj_command` varchar(32) NOT NULL default '',
  `status` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `loginfo` (
  `logevent_id` int(11) NOT NULL default '0',
  `name` varchar(32) NOT NULL default '',
  `value_int` int(11) NOT NULL default '0',
  `value_text` varchar(255) NOT NULL default '',
  `obj_type` int(11) NOT NULL default '0',
  `obj_id` int(11) NOT NULL default '0',
  KEY `logevent_id` (`logevent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `logrequest` (
  `id` int(11) NOT NULL auto_increment,
  `user_id` int(11) NOT NULL default '0',
  `request` text NOT NULL,
  `timestamp` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `mappedglobalid` (
  `globalprefix` varchar(255) default NULL,
  `globalid` int(11) default NULL,
  `localid` int(11) default NULL,
  `localid2` int(11) default NULL,
  `tablename` varchar(255) default NULL,
  `id` int(11) NOT NULL auto_increment,
  PRIMARY KEY  (`id`),
  KEY `tablename` (`tablename`,`localid`,`localid2`),
  KEY `globalprefix` (`globalprefix`,`globalid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `mappedglobalidsequence` (
  `id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `missingreference` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `source_tablename` varchar(50) NOT NULL,
  `source_id` int(10) unsigned NOT NULL,
  `reference_name` varchar(255) NOT NULL,
  `target_globalprefix` varchar(255) NOT NULL,
  `target_globalid` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `source` (`source_tablename`,`source_id`),
  KEY `reference` (`source_tablename`,`source_id`,`reference_name`),
  KEY `target` (`target_globalprefix`,`target_globalid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `ml` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) NOT NULL default '',
  `ext` varchar(8) NOT NULL default '',
  `contenttype` varchar(63) NOT NULL default '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `msg` (
  `id` int(11) NOT NULL auto_increment,
  `from_user_id` int(11) NOT NULL default '0',
  `to_user_id` int(11) NOT NULL default '0',
  `msg` text NOT NULL,
  `oldmsg` int(11) NOT NULL default '0',
  `timestamp` int(11) NOT NULL default '0',
  `instanttime` int(255) default '0',
  PRIMARY KEY  (`id`),
  KEY `from_user_id` (`from_user_id`),
  KEY `to_user_id` (`to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `node` (
  `id` int(11) NOT NULL auto_increment,
  `folder_id` int(11) NOT NULL default '0',
  `pub_dir` varchar(255) NOT NULL default '',
  `host` varchar(255) NOT NULL default '',
  `ftphost` varchar(255) NOT NULL default '',
  `ftplogin` varchar(255) NOT NULL default '',
  `ftppassword` varchar(255) NOT NULL default '',
  `ftpsync` tinyint(4) NOT NULL default '0',
  `ftpwwwroot` varchar(255) NOT NULL default '',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `utf8` int(11) NOT NULL default '0',
  `publish_fs` int(11) NOT NULL default '0',
  `publish_contentmap` int(11) NOT NULL default '0',
  `contentmap_handle` varchar(255) NOT NULL default '',
  `disable_publish` tinyint(1) NOT NULL default '0',
  `contentrepository_id` int(11) default NULL,
  PRIMARY KEY  (`id`),
  KEY `folder_id` (`folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `node_contentgroup` (
  `node_id` int(11) NOT NULL default '0',
  `contentgroup_id` int(11) NOT NULL default '0',
  `sortorder` int(11) NOT NULL default '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `nodecache` (
  `id` int(11) NOT NULL auto_increment,
  `parser` varchar(127) NOT NULL default '',
  `type` varchar(127) NOT NULL default '',
  `keyname` varchar(255) NOT NULL default '',
  `properties` varchar(255) NOT NULL default '',
  `tagkey` text NOT NULL,
  `input` mediumtext NOT NULL,
  `output` mediumtext NOT NULL,
  `lastupdate` int(11) NOT NULL default '0',
  `reload` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `nodecache_event` (
  `nodecache_id` int(11) NOT NULL default '0',
  `o_id` int(11) NOT NULL default '0',
  `o_type` int(11) NOT NULL default '0',
  `eventtype` int(11) NOT NULL default '0',
  `ignoreevent` int(11) NOT NULL default '0',
  KEY `nodecache_id` (`nodecache_id`,`o_id`,`o_type`,`eventtype`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `nodecache_object` (
  `nodecache_id` int(11) NOT NULL default '0',
  `eventtype` int(11) NOT NULL default '0',
  `o_id` int(11) NOT NULL default '0',
  `o_type` int(11) NOT NULL default '0',
  `linktype` int(11) NOT NULL default '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `nodesetup` (
  `name` char(255) NOT NULL default '',
  `intvalue` int(11) NOT NULL default '0',
  `textvalue` char(255) NOT NULL default '',
  KEY `name` (`name`),
  KEY `name_2` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `nodeversion` (
  `id` int(11) NOT NULL default '0',
  `timestamp` int(11) NOT NULL default '0',
  `user_id` int(11) NOT NULL default '0',
  `o_type` int(11) NOT NULL default '0',
  `o_id` int(11) NOT NULL default '0',
  `nodeversion` decimal(8,3) NOT NULL default '0.000'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `objprop` (
  `id` int(11) NOT NULL auto_increment,
  `name_id` int(11) default NULL,
  `description_id` int(11) default NULL,
  `o_type` int(11) NOT NULL default '0',
  `keyword` char(64) NOT NULL default '',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `objtag_id` int(11) NOT NULL,
  `category_id` int(11) NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `objtag_id` (`objtag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `objprop_category` (
  `id` int(11) NOT NULL auto_increment,
  `name_id` int(11) NOT NULL,
  `sortorder` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `objprop_node` (
  `objprop_id` int(11) NOT NULL,
  `node_id` int(11) NOT NULL,
  PRIMARY KEY  (`objprop_id`,`node_id`),
  KEY `objprop_id` (`objprop_id`),
  KEY `node_id` (`node_id`),
  CONSTRAINT `objprop_node_ibfk_1` FOREIGN KEY (`objprop_id`) REFERENCES `objprop` (`id`),
  CONSTRAINT `objprop_node_ibfk_2` FOREIGN KEY (`node_id`) REFERENCES `node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `objtag` (
  `id` int(11) NOT NULL auto_increment,
  `obj_id` int(11) NOT NULL default '0',
  `obj_type` int(11) NOT NULL default '0',
  `construct_id` int(11) NOT NULL default '0',
  `enabled` tinyint(4) NOT NULL default '0',
  `name` char(255) NOT NULL default '',
  `intag` int(11) NOT NULL default '0',
  `inheritable` int(11) NOT NULL default '0',
  `required` int(11) NOT NULL default '0',
  `inpage` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `obj_id` (`obj_id`),
  KEY `obj_type` (`obj_type`),
  KEY `name` (`name`),
  KEY `intag` (`intag`),
  KEY `name_2` (`name`,`obj_id`,`obj_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `objtranslatecache` (
  `o_id` int(11) NOT NULL default '0',
  `o_type` int(11) NOT NULL default '0',
  `cachekey` varchar(255) NOT NULL default '',
  `value_tags` mediumtext NOT NULL,
  KEY `cachekey` (`cachekey`),
  KEY `o_id` (`o_id`,`o_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `outputuser` (
  `id` int(11) NOT NULL auto_increment,
  `info` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `page` (
  `id` int(11) NOT NULL auto_increment,
  `name` char(255) NOT NULL default '',
  `description` char(255) NOT NULL default '',
  `filename` char(64) NOT NULL default '',
  `priority` tinyint(4) NOT NULL default '1',
  `status` tinyint(4) NOT NULL default '0',
  `time_start` int(11) NOT NULL default '0',
  `time_end` int(11) NOT NULL default '0',
  `time_mon` tinyint(4) NOT NULL default '0',
  `time_tue` tinyint(4) NOT NULL default '0',
  `time_wed` tinyint(4) NOT NULL default '0',
  `time_thu` tinyint(4) NOT NULL default '0',
  `time_fri` tinyint(4) NOT NULL default '0',
  `time_sat` tinyint(4) NOT NULL default '0',
  `time_sun` tinyint(4) NOT NULL default '0',
  `content_id` int(11) default '0',
  `template_id` int(11) NOT NULL default '0',
  `folder_id` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `pdate` int(11) NOT NULL default '0',
  `publisher` int(11) NOT NULL default '0',
  `time_pub` int(11) NOT NULL default '0',
  `contentgroup_id` int(11) NOT NULL default '0',
  `contentset_id` int(11) NOT NULL default '0',
  `delay_publish` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `content_id` (`content_id`),
  KEY `folder_id` (`folder_id`),
  KEY `template_id` (`template_id`),
  KEY `name` (`name`),
  KEY `contentset_id` (`contentset_id`),
  KEY `contentgroup_id` (`contentgroup_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `page_nodeversion` (
  `id` int(11) NOT NULL default '0',
  `name` char(255) NOT NULL default '',
  `description` char(255) NOT NULL default '',
  `filename` char(64) NOT NULL default '',
  `priority` tinyint(4) NOT NULL default '1',
  `content_id` int(11) default '0',
  `template_id` int(11) NOT NULL default '0',
  `folder_id` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `publisher` int(11) NOT NULL default '0',
  `contentgroup_id` int(11) NOT NULL default '0',
  `contentset_id` int(11) NOT NULL default '0',
  `nodeversiontimestamp` int(11) NOT NULL default '0',
  `nodeversion_user` int(11) NOT NULL default '0',
  `nodeversionlatest` int(11) NOT NULL default '0',
  `nodeversionremoved` int(11) NOT NULL default '0',
  KEY `id` (`id`),
  KEY `content_id` (`content_id`),
  KEY `folder_id` (`folder_id`),
  KEY `template_id` (`template_id`),
  KEY `name` (`name`),
  KEY `contentset_id` (`contentset_id`),
  KEY `contentgroup_id` (`contentgroup_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `param` (
  `paramid` int(11) default NULL,
  `name` varchar(30) default NULL,
  `value` text
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `part` (
  `id` int(11) NOT NULL auto_increment,
  `construct_id` int(11) NOT NULL default '0',
  `type_id` int(11) NOT NULL default '0',
  `name_id` int(11) NOT NULL default '0',
  `required` tinyint(4) NOT NULL default '0',
  `editable` tinyint(4) NOT NULL default '0',
  `partoption_id` int(11) NOT NULL default '0',
  `partorder` smallint(6) NOT NULL default '1',
  `keyword` varchar(64) NOT NULL default '',
  `hidden` tinyint(4) NOT NULL default '0',
  `ml_id` int(11) default '0',
  `info_int` int(11) NOT NULL default '0',
  `info_text` text,
  PRIMARY KEY  (`id`),
  KEY `construct_id` (`construct_id`),
  KEY `partorder` (`partorder`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `perm` (
  `usergroup_id` int(11) NOT NULL default '0',
  `o_type` int(11) NOT NULL default '0',
  `o_id` int(11) NOT NULL default '0',
  `perm` char(32) NOT NULL default '',
  KEY `usergroup_id` (`usergroup_id`),
  KEY `o_id` (`o_id`),
  KEY `perm` (`perm`),
  KEY `o_type` (`o_type`,`usergroup_id`),
  KEY `usergroup_id_2` (`usergroup_id`,`o_type`,`o_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `pmdiary` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) NOT NULL default '',
  `folder_id` int(11) NOT NULL default '0',
  `body` text NOT NULL,
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `csid` varchar(64) NOT NULL default '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `pmdiary_group` (
  `pmdiary_id` int(11) NOT NULL default '0',
  `usergroup_id` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `pmdiary_notify` (
  `pmdiary_id` int(11) NOT NULL default '0',
  `systemuser_id` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `pmfile` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) NOT NULL default '',
  `o_id` int(11) NOT NULL default '0',
  `o_type` int(11) NOT NULL default '0',
  `description` text NOT NULL,
  `filename` varchar(255) NOT NULL default '',
  `filetype` varchar(128) NOT NULL default '',
  `filesize` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `pmfolder` (
  `id` int(11) NOT NULL auto_increment,
  `mother` int(11) NOT NULL default '0',
  `type_id` int(11) NOT NULL default '0',
  `name` char(100) NOT NULL default '',
  `description` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `item_order` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `mother` (`mother`),
  KEY `type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `pmtime` (
  `id` int(11) NOT NULL auto_increment,
  `user_id` int(11) NOT NULL default '0',
  `project_id` int(11) NOT NULL default '0',
  `starttime` int(11) NOT NULL default '0',
  `endtime` int(11) NOT NULL default '0',
  `name` text NOT NULL,
  `description` text NOT NULL,
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `project_id` (`project_id`),
  KEY `user_id` (`user_id`,`starttime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `pmtodo` (
  `id` int(11) NOT NULL auto_increment,
  `folder_id` int(11) NOT NULL default '0',
  `name` varchar(255) NOT NULL default '',
  `description` text NOT NULL,
  `starttime` int(11) NOT NULL default '0',
  `endtime` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `priority` int(11) NOT NULL default '0',
  `category` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `folder_id` (`folder_id`),
  KEY `creator` (`creator`),
  KEY `editor` (`editor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `pmtodo_comment` (
  `id` int(11) NOT NULL auto_increment,
  `todo_id` int(11) NOT NULL default '0',
  `comment` text NOT NULL,
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `pmtodo_file` (
  `id` int(11) NOT NULL auto_increment,
  `todo_id` int(11) NOT NULL default '0',
  `filename` varchar(100) NOT NULL default '',
  `filesize` int(11) NOT NULL default '0',
  `filetype` varchar(100) NOT NULL default '',
  `friendlyname` varchar(100) NOT NULL default '',
  `name` varchar(255) NOT NULL default '',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `pmtodo_history` (
  `id` int(11) NOT NULL auto_increment,
  `todo_id` int(11) NOT NULL default '0',
  `user_id` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `action` int(11) NOT NULL default '0',
  `action_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `pmtodo_user` (
  `todo_id` int(11) NOT NULL default '0',
  `user_id` int(11) NOT NULL default '0',
  `state` int(11) NOT NULL default '0',
  `viewed` int(11) NOT NULL default '0',
  `last_send` int(11) NOT NULL default '0',
  KEY `todo_id` (`todo_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `ptree` (
  `id` int(11) NOT NULL auto_increment,
  `mother_id` int(11) NOT NULL default '0',
  `mother_type` int(11) NOT NULL default '0',
  `link` varchar(255) NOT NULL default '',
  `name` varchar(255) NOT NULL default '',
  `allowlink` varchar(255) NOT NULL default '',
  `folderopen` varchar(255) NOT NULL default '',
  `folderclosed` varchar(255) NOT NULL default '',
  `prepend` varchar(255) NOT NULL default '',
  `append` varchar(255) NOT NULL default '',
  `sqlstmt` varchar(255) NOT NULL default '',
  `fkt` varchar(255) NOT NULL default '',
  `bind_id` int(11) NOT NULL default '0',
  `bind_type` int(11) NOT NULL default '0',
  `linktarget` varchar(255) NOT NULL default '',
  `linkext` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `publish` (
  `id` int(11) NOT NULL auto_increment,
  `page_id` int(11) NOT NULL default '0',
  `node_id` int(11) NOT NULL default '0',
  `folder_id` int(11) NOT NULL default '0',
  `path` varchar(255) NOT NULL default '',
  `filename` varchar(255) NOT NULL default '',
  `source` mediumtext,
  `pdate` int(11) NOT NULL default '0',
  `ddate` int(11) NOT NULL default '0',
  `active` tinyint(4) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `page_id` (`page_id`),
  KEY `active` (`active`),
  KEY `folder_id` (`folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `reaction` (
  `id` int(11) NOT NULL auto_increment,
  `keyword` varchar(255) NOT NULL default '',
  `triggerevent_id` int(11) NOT NULL default '0',
  `workflow_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `reactionprop` (
  `id` int(11) NOT NULL auto_increment,
  `reaction_id` int(11) NOT NULL default '0',
  `keyword` varchar(255) NOT NULL default '',
  `value` text NOT NULL,
  `mapkey` varchar(255) NOT NULL default '',
  `workflowlink_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `reactionpropeditable` (
  `id` int(11) NOT NULL auto_increment,
  `reactionprop_id` int(11) NOT NULL default '0',
  `description` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `regex` (
  `id` int(11) NOT NULL auto_increment,
  `name_id` int(11) NOT NULL default '0',
  `desc_id` int(11) NOT NULL default '0',
  `regex` text NOT NULL,
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `system` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `name_id` (`name_id`),
  KEY `desc_id` (`desc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `role` (
  `id` int(11) NOT NULL auto_increment,
  `name_id` int(11) default NULL,
  `description_id` int(11) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `role_usergroup` (
  `id` int(11) NOT NULL auto_increment,
  `role_id` int(11) NOT NULL,
  `usergroup_id` int(11) NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `role_usergroup_assignment` (
  `id` int(11) NOT NULL auto_increment,
  `role_usergroup_id` int(11) NOT NULL,
  `obj_id` int(11) default NULL,
  `obj_type` int(11) NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `role_usergroup_id` (`role_usergroup_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `roleperm` (
  `id` int(11) NOT NULL auto_increment,
  `role_id` int(11) NOT NULL,
  `perm` char(32) default NULL,
  PRIMARY KEY  (`id`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `roleperm_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `roleperm_obj` (
  `id` int(11) NOT NULL auto_increment,
  `roleperm_id` int(11) NOT NULL,
  `obj_type` int(11) NOT NULL,
  `obj_id` int(11) default NULL,
  PRIMARY KEY  (`id`),
  KEY `roleperm_id` (`roleperm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `schedule2` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) NOT NULL default '',
  `description` text NOT NULL,
  `starttime2` int(11) NOT NULL default '0',
  `endtime2` int(11) NOT NULL default '0',
  `fullday` int(11) NOT NULL default '0',
  `dorepeat` tinyint(4) NOT NULL default '0',
  `repeatuntil` int(11) NOT NULL default '0',
  `private` tinyint(4) NOT NULL default '0',
  `state` int(11) NOT NULL default '0',
  `remind` tinyint(4) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `location` varchar(255) NOT NULL default '-',
  PRIMARY KEY  (`id`),
  KEY `starttime2` (`starttime2`,`endtime2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `systemsession` (
  `id` int(11) NOT NULL auto_increment,
  `sid` varchar(15) NOT NULL default '',
  `user_id` int(11) NOT NULL default '0',
  `ip` varchar(15) NOT NULL default '',
  `agent` text NOT NULL,
  `cookie` int(11) NOT NULL default '0',
  `since` int(11) NOT NULL default '0',
  `language` tinyint(4) NOT NULL default '0',
  `val` mediumtext NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `sid` (`sid`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `systemuser` (
  `id` int(11) NOT NULL auto_increment,
  `firstname` char(255) NOT NULL default '',
  `lastname` char(255) NOT NULL default '',
  `login` char(255) NOT NULL default '',
  `password` char(127) NOT NULL default '',
  `email` char(255) NOT NULL default '',
  `bonus` int(11) NOT NULL default '0',
  `active` tinyint(4) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `description` char(255) NOT NULL default '',
  `isldapuser` int(11) NOT NULL default '0',
  `inboxtoemail` int(11) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `tagmap` (
  `tagname` varchar(255) NOT NULL default '',
  `mapname` varchar(255) NOT NULL default '',
  `searchname` varchar(255) NOT NULL default '',
  `object` int(11) NOT NULL default '0',
  `objtype` int(11) NOT NULL default '0',
  `attributetype` int(11) NOT NULL default '0',
  `multivalue` int(1) NOT NULL default '0',
  `static` int(11) NOT NULL default '0',
  `optimized` int(11) NOT NULL default '0',
  `foreignlinkattribute` varchar(255) NOT NULL default '',
  `id` int(11) NOT NULL auto_increment,
  `foreignlinkattributerule` varchar(255) NOT NULL default '',
  `contentrepository_id` int(11) default NULL,
  `category` varchar(255) NOT NULL default '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `task` (
  `id` int(11) NOT NULL auto_increment,
  `tasktemplate_id` int(11) NOT NULL default '0',
  `name` varchar(255) default NULL,
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) default NULL,
  `edate` int(11) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `taskparam` (
  `id` int(11) NOT NULL auto_increment,
  `task_id` int(11) NOT NULL default '0',
  `templateparam_id` int(11) NOT NULL default '0',
  `value` mediumtext,
  `name` varchar(50) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `tasktemplate` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) NOT NULL default '',
  `description` text,
  `command` text,
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) default NULL,
  `edate` int(11) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `tasktemplateparam` (
  `id` int(11) NOT NULL auto_increment,
  `tasktemplate_id` int(11) NOT NULL default '0',
  `paramtype` int(11) NOT NULL default '0',
  `name` varchar(255) NOT NULL default '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `template` (
  `id` int(11) NOT NULL auto_increment,
  `folder_id` int(11) NOT NULL default '0',
  `templategroup_id` int(11) NOT NULL default '0',
  `name` varchar(255) NOT NULL default '',
  `locked` int(11) NOT NULL default '0',
  `locked_by` int(11) NOT NULL default '0',
  `ml_id` int(11) NOT NULL default '0',
  `ml` mediumtext NOT NULL,
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `description` varchar(255) NOT NULL default '',
  PRIMARY KEY  (`id`),
  KEY `folder_id` (`folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `template_folder` (
  `template_id` int(11) NOT NULL default '0',
  `folder_id` int(11) NOT NULL default '0',
  KEY `template_id` (`template_id`),
  KEY `folder_id` (`folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `templategroup` (
  `id` int(11) NOT NULL auto_increment,
  `nada` tinyint(4) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `templatetag` (
  `id` int(11) NOT NULL auto_increment,
  `templategroup_id` int(11) NOT NULL default '0',
  `template_id` int(11) NOT NULL default '0',
  `construct_id` int(11) NOT NULL default '1',
  `pub` tinyint(4) NOT NULL default '0',
  `enabled` tinyint(4) NOT NULL default '0',
  `name` char(255) NOT NULL default '',
  PRIMARY KEY  (`id`),
  KEY `template_id` (`template_id`),
  KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `tetris` (
  `id` int(11) NOT NULL auto_increment,
  `user_id` int(11) NOT NULL default '0',
  `score` int(11) NOT NULL default '0',
  `timestamp` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `tree` (
  `id` int(11) NOT NULL auto_increment,
  `itemorder` int(11) NOT NULL default '0',
  `mother` int(11) NOT NULL default '0',
  `name` char(255) NOT NULL default '',
  `type_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `mother` (`mother`),
  KEY `type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `triggerevent` (
  `id` int(11) NOT NULL auto_increment,
  `keyword` varchar(255) NOT NULL default '',
  `reaction_id` int(11) NOT NULL default '0',
  `active` int(11) NOT NULL default '0',
  `workflow_id` int(11) NOT NULL default '0',
  `templateevent_id` int(11) NOT NULL default '0',
  `workflowlink_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `type` (
  `id` int(11) NOT NULL auto_increment,
  `name` char(255) NOT NULL default '',
  `description` char(255) NOT NULL default '',
  `auto` tinyint(4) NOT NULL default '0',
  `javaclass` varchar(255) NOT NULL default '',
  PRIMARY KEY  (`id`),
  KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `udate` (
  `o_id` int(10) unsigned NOT NULL default '0',
  `tablename` varchar(100) NOT NULL default '',
  `udate` int(11) unsigned NOT NULL default '0',
  PRIMARY KEY  (`tablename`,`o_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `user_group` (
  `user_id` int(11) NOT NULL default '0',
  `usergroup_id` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  KEY `user_id` (`user_id`),
  KEY `usergroup_id` (`usergroup_id`),
  KEY `usergroup_id_2` (`usergroup_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `user_schedule2` (
  `id` int(11) NOT NULL auto_increment,
  `date_id` int(11) NOT NULL default '0',
  `user_id` int(11) NOT NULL default '0',
  `group_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `date_id` (`date_id`),
  KEY `user_id` (`user_id`),
  KEY `group_id` (`group_id`),
  KEY `date_id_2` (`date_id`,`group_id`),
  KEY `date_id_3` (`date_id`,`user_id`,`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `usergroup` (
  `id` int(11) NOT NULL auto_increment,
  `name` char(255) NOT NULL default '',
  `mother` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `description` char(255) NOT NULL default '',
  PRIMARY KEY  (`id`),
  KEY `mother` (`mother`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `value` (
  `id` int(11) NOT NULL auto_increment,
  `part_id` int(11) NOT NULL default '0',
  `info` int(11) NOT NULL default '0',
  `static` tinyint(4) NOT NULL default '0',
  `templatetag_id` int(11) NOT NULL default '0',
  `contenttag_id` int(11) NOT NULL default '0',
  `globaltag_id` int(11) NOT NULL default '0',
  `objtag_id` int(11) NOT NULL default '0',
  `value_text` mediumtext NOT NULL,
  `value_ref` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `part_id` (`part_id`),
  KEY `info` (`info`),
  KEY `templatetag_id` (`templatetag_id`),
  KEY `contenttag_id` (`contenttag_id`),
  KEY `globaltag_id` (`globaltag_id`),
  KEY `objtag_id` (`objtag_id`),
  KEY `value_ref` (`value_ref`),
  KEY `static` (`static`),
  KEY `part_id_2` (`part_id`,`templatetag_id`,`contenttag_id`,`objtag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `value_nodeversion` (
  `id` int(11) NOT NULL default '0',
  `part_id` int(11) NOT NULL default '0',
  `info` int(11) NOT NULL default '0',
  `static` tinyint(4) NOT NULL default '0',
  `templatetag_id` int(11) NOT NULL default '0',
  `contenttag_id` int(11) NOT NULL default '0',
  `globaltag_id` int(11) NOT NULL default '0',
  `objtag_id` int(11) NOT NULL default '0',
  `value_text` mediumtext NOT NULL,
  `value_ref` int(11) NOT NULL default '0',
  `nodeversiontimestamp` int(11) NOT NULL default '0',
  `nodeversion_user` int(11) NOT NULL default '0',
  `nodeversionlatest` int(11) NOT NULL default '0',
  `nodeversionremoved` int(11) NOT NULL default '0',
  KEY `id` (`id`),
  KEY `contenttag_id` (`contenttag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `workflow` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) NOT NULL default '',
  `description` text NOT NULL,
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  `o_type` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `workflowlink` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) NOT NULL default '',
  `workflow_id` int(11) NOT NULL default '0',
  `o_type` int(11) NOT NULL default '0',
  `o_id` int(11) NOT NULL default '0',
  `creator` int(11) NOT NULL default '0',
  `cdate` int(11) NOT NULL default '0',
  `editor` int(11) NOT NULL default '0',
  `edate` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
