--
-- Table structure for table `contentattribute`
--

CREATE TABLE `contentattribute` (
  `id` int(11) NOT NULL auto_increment,
  `contentid` varchar(32) NOT NULL default '',
  `name` varchar(255) NOT NULL default '',
  `value_text` varchar(255),
  `value_bin` longblob,
  `value_int` int(11) default NULL,
  `sortorder` int(11) default NULL,
  `value_blob` longblob,
  `value_clob` mediumtext,
  `value_long` bigint(20) default NULL,
  `value_double` double default NULL,
  `value_date` datetime default NULL,
  PRIMARY KEY  (`id`),
  KEY `contentid` (`contentid`),
  KEY `name` (`name`),
  KEY `contentid_2` (`contentid`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `contentattribute_nodeversion`
--

CREATE TABLE `contentattribute_nodeversion` (
  `id` int(11) NOT NULL default '0',
  `contentid` varchar(32) NOT NULL default '',
  `name` varchar(255) NOT NULL default '',
  `value_text` varchar(255),
  `value_bin` longblob,
  `value_int` int(11) default NULL,
  `sortorder` int(11) default NULL,
  `value_blob` longblob,
  `nodeversiontimestamp` int(11) default NULL,
  `nodeversion_user` varchar(255) default NULL,
  `nodeversionlatest` int(11) default NULL,
  `nodeversionremoved` int(11) default NULL,
  `nodeversion_autoupdate` tinyint(4) NOT NULL default '0',
  `value_clob` mediumtext,
  `value_long` bigint(20) default NULL,
  `value_double` double default NULL,
  `value_date` datetime default NULL,
  KEY `id_2` (`id`,`nodeversiontimestamp`),
  KEY `contentid_2` (`contentid`,`name`),
  KEY `sortorder` (`sortorder`),
  KEY `contentid_3` (`contentid`,`name`(100),`value_text`(100)),
  KEY `contentid` (`contentid`),
  KEY `id` (`id`),
  KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `contentattributetype`
--

CREATE TABLE `contentattributetype` (
  `name` varchar(255) default NULL,
  `attributetype` int(11) default NULL,
  `optimized` int(11) default NULL,
  `quickname` varchar(255) default NULL,
  `multivalue` int(11) NOT NULL default '0',
  `objecttype` int(11) NOT NULL default '0',
  `linkedobjecttype` int(11) NOT NULL default '0',
  `foreignlinkattribute` varchar(255) default NULL,
  `foreignlinkattributerule` mediumtext,
  `exclude_versioning` int(11) NOT NULL default '0',
  `filesystem` int(11) NOT NULL default '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `contentmap`
--
-- gentics-start-table-contentmap
CREATE TABLE `contentmap` (
  `id` int(11) NOT NULL auto_increment,
  `contentid` varchar(32) NOT NULL default '',
  `obj_id` int(11) NOT NULL default '0',
  `obj_type` int(11) NOT NULL default '0',
  `mother_obj_id` int(11) NOT NULL default '0',
  `mother_obj_type` int(11) NOT NULL default '0',
  `updatetimestamp` int(11) NOT NULL default '0',
  `motherid` varchar(32) default NULL,
  PRIMARY KEY  (`id`),
  KEY `obj_id` (`obj_id`),
  KEY `obj_type` (`obj_type`),
  KEY `motherid` (`motherid`,`contentid`),
  UNIQUE KEY `contentid` (`contentid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- gentics-end-table-contentmap
--
-- Table structure for table `contentmap_nodeversion`
--
-- gentics-start-table-contentmap_nodeversion
CREATE TABLE `contentmap_nodeversion` (
  `id` int(11) NOT NULL default '0',
  `contentid` varchar(32) NOT NULL default '',
  `obj_id` int(11) NOT NULL default '0',
  `obj_type` int(11) NOT NULL default '0',
  `motherid` varchar(32) default NULL,
  `mother_obj_id` int(11) NOT NULL default '0',
  `mother_obj_type` int(11) NOT NULL default '0',
  `updatetimestamp` int(11) NOT NULL default '0',
  `nodeversiontimestamp` int(11) default NULL,
  `nodeversion_user` varchar(255) default NULL,
  `nodeversionlatest` int(11) default NULL,
  `nodeversionremoved` int(11) default NULL,
  `nodeversion_autoupdate` tinyint(4) NOT NULL default '0',
  KEY `obj_id` (`obj_id`),
  KEY `id` (`id`),
  KEY `obj_type` (`obj_type`),
  KEY `motherid_2` (`motherid`),
  KEY `contentid` (`contentid`),
  KEY `motherid` (`motherid`,`contentid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- gentics-end-table-contentmap_nodeversion
--
-- Table structure for table `contentobject`
--

CREATE TABLE `contentobject` (
  `name` varchar(32) default NULL,
  `type` int(11) NOT NULL default '0',
  `id_counter` int(11) NOT NULL default '0',
  `exclude_versioning` int(11) NOT NULL default '0',
  PRIMARY KEY  (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `contentstatus`
--

CREATE TABLE `contentstatus` (
  `name` varchar(255) NOT NULL default '',
  `intvalue` int(11) default NULL,
  `stringvalue` mediumtext,
  PRIMARY KEY  (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
