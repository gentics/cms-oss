INSERT INTO `job` VALUES  (1,'RootTaskTT','',1,'manual','a:1:{s:1:\"s\";s:4:\"b:1;\";}',1,3,1211281542,3,1211281954,0,''),
 (2,'ParallelSubTask1TT','',2,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"1\";}s:7:\"runnext\";b:0;}\";}',1,3,1211281610,3,1211281954,1,''),
 (3,'ParallelSubTask2TT','',3,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"1\";}s:7:\"runnext\";b:0;}\";}',1,3,1211281600,3,1211282289,1,'');

INSERT INTO `jobdependency` VALUES  (1,3,1),
 (2,2,1);

INSERT INTO `task` VALUES  (1,1,'RootTask1T',3,1211281476,3,1211281476),
 (2,2,'ParallelSubTask1T',2,1211281493,3,1211281493),
 (3,3,'ParallelSubTask2T',3,1211281504,3,1211281504);

INSERT INTO `tasktemplate` VALUES  (1,'RootTask1','Manual started root task','sleep 1',3,1211280206,3,1211280206),
 (2,'ParallelSubTask1','ParallelSubTask have a duration of 20 sec','sleep 20',3,1211280251,3,1211280251),
 (3,'ParallelSubTask2','ParallelSubTask have a duration of 10 sec','sleep 10',3,1211280341,3,1211280349);

