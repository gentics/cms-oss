INSERT INTO `job` VALUES  (1,'RootTask1TT','',1,'manual','a:1:{s:1:\"s\";s:4:\"b:1;\";}',1,3,1211286291,3,1211288743,0,''),
 (2,'SubTask1TT','',2,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"1\";}s:7:\"runnext\";b:0;}\";}',1,3,1211290598,3,1211290633,0,''),
 (3,'SubTask2TT','',3,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"1\";}s:7:\"runnext\";b:0;}\";}',1,3,1211290610,3,1211290610,0,''),
 (4,'ParallelRootTask1TT','',4,'manual','a:1:{s:1:\"s\";s:4:\"b:1;\";}',1,3,1211286350,3,1211290626,1,''),
 (5,'ParallelRootTask2TT','',5,'manual','a:1:{s:1:\"s\";s:4:\"b:1;\";}',1,3,1211286417,3,1211288743,1,'');

INSERT INTO `jobdependency` VALUES  (1,2,1),
 (2,3,1);

INSERT INTO `task` VALUES  (1,1,'RootTask1T',3,1211286183,3,1211286183),
 (2,2,'SubTask1T',3,1211286218,3,1211286218),
 (3,3,'SubTask2T',3,1211286228,3,1211286228),
 (4,4,'ParallelRootTask1T',3,1211286258,3,1211286258),
 (5,5,'ParallelRootTask2T',3,1211286268,3,1211286621);

INSERT INTO `tasktemplate` VALUES  (1,'RootTask1','sleep 1','sleep 1',3,1211285900,3,1211286159),
 (2,'SubTask1','sleep 6','sleep 6',3,1211285915,3,1211285915),
 (3,'SubTask2','sleep 20','sleep 20',3,1211285926,3,1211286133),
 (4,'ParallelRootTask1','sleep 10','sleep 10',3,1211286025,3,1211286145),
 (5,'ParallelRootTask2','sleep 10','sleep 10',3,1211286053,3,1211286149);
