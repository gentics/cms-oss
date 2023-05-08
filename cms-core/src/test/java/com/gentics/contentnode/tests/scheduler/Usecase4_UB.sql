INSERT INTO `job` VALUES  (1,'RootTask1TT','',3,'int','a:1:{s:1:\"s\";s:103:\"a:5:{s:5:\"start\";i:1211234400;s:3:\"end\";i:0;s:5:\"every\";s:1:\"1\";s:5:\"scale\";s:3:\"min\";s:7:\"runnext\";N;}\";}',1,3,1211292358,3,1211292498,0,''),
 (2,'ParallelRootTask1TT','',1,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"1\";}s:7:\"runnext\";b:0;}\";}',1,3,1211292475,3,1211292498,0,''),
 (3,'ParallelRootTask2TT','',2,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"2\";}s:7:\"runnext\";b:0;}\";}',1,3,1211292483,3,1211292498,0,'');

INSERT INTO `jobdependency` VALUES  (1,2,1),
 (2,3,2);

INSERT INTO `task` VALUES  (1,2,'ParallelSubTask1T',3,1211292222,3,1211292249),
 (2,3,'ParallelSubTask2T',3,1211292240,3,1211292254),
 (3,1,'RootTask1T',3,1211292286,3,1211292286);

INSERT INTO `tasktemplate` VALUES  (1,'RootTask1','RootTask','sleep 10',3,1211291353,3,1211291353),
 (2,'ParallelSubTask1','','sleep 60',3,1211292185,3,1211292185),
 (3,'ParallelSubTask2','','sleep 30',3,1211292195,3,1211292195);
