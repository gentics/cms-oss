INSERT INTO `job` VALUES  (1,'RootTaskTT','',1,'manual','a:1:{s:1:\"s\";s:4:\"b:1;\";}',1,3,1211279910,3,1211358603,0,''),
 (2,'SubTask1TT','',2,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"1\";}s:7:\"runnext\";b:0;}\";}',1,3,1211280027,3,1211358603,0,''),
 (3,'SubTask2TT','',3,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"2\";}s:7:\"runnext\";b:0;}\";}',1,3,1211280042,3,1211358603,0,'');

INSERT INTO `jobdependency` VALUES  (1,2,1),
 (2,3,2);

INSERT INTO `task` VALUES  (1,1,'RootTask1T',3,1211279851,3,1211279851),
 (2,2,'SubTask1T',3,1211279865,3,1211279865),
 (3,3,'SubTask2T',3,1211279877,3,1211279877);

INSERT INTO `tasktemplate` VALUES  (1,'RootTask1','Manual started root task - waits 5 second','sleep 5',3,1211279749,3,1211279749),
 (2,'SubTask1','Subtask1 sleeps 10 seconds','sleep 10',3,1211279785,3,1211279831),
 (3,'SubTask2','Subtask2 sleeps 15 seconds','sleep 15',3,1211279812,3,1211279826);
