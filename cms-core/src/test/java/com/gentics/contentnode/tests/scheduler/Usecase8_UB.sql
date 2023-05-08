INSERT INTO `job` VALUES  (7,'IntervalRootTaskTT','',4,'int','a:1:{s:1:\"s\";s:105:\"a:5:{s:5:\"start\";i:1213567200;s:3:\"end\";i:0;s:5:\"every\";s:1:\"2\";s:5:\"scale\";s:3:\"min\";s:7:\"runnext\";b:1;}\";}',1,3,1213626214,3,1213626350,0,''),
 (8,'SubTaskA1TT','',1,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"7\";}s:7:\"runnext\";b:1;}\";}',1,3,1213626297,3,1213626341,0,''),
 (9,'SubTaskA2TT','',2,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"7\";}s:7:\"runnext\";b:1;}\";}',1,3,1213626305,3,1213626354,0,''),
 (10,'SubTaskA3TT','',3,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"7\";}s:7:\"runnext\";b:1;}\";}',1,3,1213626318,3,1213626359,0,'');

INSERT INTO `jobdependency` VALUES  (1,8,7),
 (2,9,7),
 (3,10,7);

INSERT INTO `task` VALUES  (1,2,'SubTaskA1T',3,1213626124,3,1213626124),
 (2,3,'SubTaskA2T',3,1213626134,3,1213626134),
 (3,4,'SubTaskA3T',3,1213626144,3,1213626144),
 (4,1,'IntervalRootTaskT',3,1213626161,3,1213626161);

INSERT INTO `tasktemplate` VALUES  (1,'IntervalRootTask','','sleep 1',3,1213626046,3,1213626046),
 (2,'SubTaskA1','','sleep 60',3,1213626072,3,1213626072),
 (3,'SubTaskA2','','sleep 80',3,1213626089,3,1213626089),
 (4,'SubTaskA3','','sleep 40',3,1213626107,3,1213626107);
