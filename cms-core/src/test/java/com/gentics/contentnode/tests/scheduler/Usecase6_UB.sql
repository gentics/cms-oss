INSERT INTO `job` VALUES  (1,'ParallelRootTaskATT','',1,'manual','a:1:{s:1:\"s\";s:4:\"b:1;\";}',1,3,1211358149,3,1211358335,1,''),
 (2,'ParallelRootTaskBTT','',3,'manual','a:1:{s:1:\"s\";s:4:\"b:1;\";}',1,3,1211358242,3,1211358335,1,''),
 (3,'ParallelSubTaskA1TT','',2,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"1\";}s:7:\"runnext\";b:0;}\";}',1,3,1211358316,3,1211358335,1,''),
 (4,'ParallelSubTaskB2TT','',4,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"2\";}s:7:\"runnext\";b:0;}\";}',1,3,1211358330,3,1211358335,1,''),
 (5,'ParallelSubTaskB1TT','',5,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"2\";}s:7:\"runnext\";b:0;}\";}',1,3,1211358326,3,1211358335,1,''),
 (6,'ParallelSubTaskA2TT','',6,'followup','a:1:{s:1:\"s\";s:53:\"a:2:{s:4:\"jobs\";a:1:{i:0;s:1:\"1\";}s:7:\"runnext\";b:0;}\";}',1,3,1211358322,3,1211358335,1,'');

INSERT INTO `jobdependency` VALUES  (5,3,1),
 (6,6,1),
 (7,5,2),
 (8,4,2);

INSERT INTO `task` VALUES  (1,1,'ParallelRootTaskAT',3,1211357674,3,1211357674),
 (2,2,'ParallelSubTaskA1T',3,1211357693,3,1211357693),
 (3,4,'ParallelRootTaskBT',3,1211357711,3,1211357711),
 (4,6,'ParallelSubTaskB2T',3,1211357728,3,1211357728),
 (5,5,'ParallelSubTaskB1T',3,1211357776,3,1211357776),
 (6,3,'ParallelSubTaskA2T',3,1211357825,3,1211357825);

INSERT INTO `tasktemplate` VALUES  (1,'ParallelRootTaskA','','sleep 5',3,1211357377,3,1211357377),
 (2,'ParallelSubTaskA1','','sleep 10',3,1211357385,3,1211357535),
 (3,'ParallelSubTaskA2','','sleep 10',3,1211357395,3,1211357526),
 (4,'ParallelRootTaskB','','sleep 5',3,1211357405,3,1211357554),
 (5,'ParallelSubTaskB1','','sleep 10',3,1211357497,3,1211357497),
 (6,'ParallelSubTaskB2','','sleep 10',3,1211357515,3,1211357515);

