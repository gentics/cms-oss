CREATE TABLE `QRTZ_BLOB_TRIGGERS` (
  `TRIGGER_NAME` varchar(80) NOT NULL,
  `TRIGGER_GROUP` varchar(80) NOT NULL,
  `BLOB_DATA` blob DEFAULT NULL,
  PRIMARY KEY (`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `QRTZ_CALENDARS` (
  `CALENDAR_NAME` varchar(80) NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`CALENDAR_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `QRTZ_CRON_TRIGGERS` (
  `TRIGGER_NAME` varchar(80) NOT NULL,
  `TRIGGER_GROUP` varchar(80) NOT NULL,
  `CRON_EXPRESSION` varchar(80) NOT NULL,
  `TIME_ZONE_ID` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `QRTZ_FIRED_TRIGGERS` (
  `ENTRY_ID` varchar(95) NOT NULL,
  `TRIGGER_NAME` varchar(80) NOT NULL,
  `TRIGGER_GROUP` varchar(80) NOT NULL,
  `IS_VOLATILE` varchar(1) NOT NULL,
  `INSTANCE_NAME` varchar(80) NOT NULL,
  `FIRED_TIME` bigint(13) NOT NULL,
  `STATE` varchar(16) NOT NULL,
  `JOB_NAME` varchar(80) DEFAULT NULL,
  `JOB_GROUP` varchar(80) DEFAULT NULL,
  `IS_STATEFUL` varchar(1) DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`ENTRY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `QRTZ_JOB_DETAILS` (
  `JOB_NAME` varchar(80) NOT NULL,
  `JOB_GROUP` varchar(80) NOT NULL,
  `DESCRIPTION` varchar(120) DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(128) NOT NULL,
  `IS_DURABLE` varchar(1) NOT NULL,
  `IS_VOLATILE` varchar(1) NOT NULL,
  `IS_STATEFUL` varchar(1) NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) NOT NULL,
  `JOB_DATA` blob DEFAULT NULL,
  PRIMARY KEY (`JOB_NAME`,`JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `QRTZ_JOB_LISTENERS` (
  `JOB_NAME` varchar(80) NOT NULL,
  `JOB_GROUP` varchar(80) NOT NULL,
  `JOB_LISTENER` varchar(80) NOT NULL,
  PRIMARY KEY (`JOB_NAME`,`JOB_GROUP`,`JOB_LISTENER`),
  CONSTRAINT `QRTZ_JOB_LISTENERS_ibfk_1` FOREIGN KEY (`JOB_NAME`, `JOB_GROUP`) REFERENCES `QRTZ_JOB_DETAILS` (`JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `QRTZ_LOCKS` (
  `LOCK_NAME` varchar(40) NOT NULL,
  PRIMARY KEY (`LOCK_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS` (
  `TRIGGER_GROUP` varchar(80) NOT NULL,
  PRIMARY KEY (`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `QRTZ_SCHEDULER_STATE` (
  `INSTANCE_NAME` varchar(80) NOT NULL,
  `LAST_CHECKIN_TIME` bigint(13) NOT NULL,
  `CHECKIN_INTERVAL` bigint(13) NOT NULL,
  `RECOVERER` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`INSTANCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `QRTZ_SIMPLE_TRIGGERS` (
  `TRIGGER_NAME` varchar(80) NOT NULL,
  `TRIGGER_GROUP` varchar(80) NOT NULL,
  `REPEAT_COUNT` bigint(7) NOT NULL,
  `REPEAT_INTERVAL` bigint(12) NOT NULL,
  `TIMES_TRIGGERED` bigint(7) NOT NULL,
  PRIMARY KEY (`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `QRTZ_TRIGGERS` (
  `TRIGGER_NAME` varchar(80) NOT NULL,
  `TRIGGER_GROUP` varchar(80) NOT NULL,
  `JOB_NAME` varchar(80) NOT NULL,
  `JOB_GROUP` varchar(80) NOT NULL,
  `IS_VOLATILE` varchar(1) NOT NULL,
  `DESCRIPTION` varchar(120) DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PREV_FIRE_TIME` bigint(13) DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint(13) NOT NULL,
  `END_TIME` bigint(13) DEFAULT NULL,
  `CALENDAR_NAME` varchar(80) DEFAULT NULL,
  `MISFIRE_INSTR` smallint(2) DEFAULT NULL,
  `JOB_DATA` blob DEFAULT NULL,
  PRIMARY KEY (`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `JOB_NAME` (`JOB_NAME`,`JOB_GROUP`),
  CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`JOB_NAME`, `JOB_GROUP`) REFERENCES `QRTZ_JOB_DETAILS` (`JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `QRTZ_TRIGGER_LISTENERS` (
  `TRIGGER_NAME` varchar(80) NOT NULL,
  `TRIGGER_GROUP` varchar(80) NOT NULL,
  `TRIGGER_LISTENER` varchar(80) NOT NULL,
  PRIMARY KEY (`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_LISTENER`),
  CONSTRAINT `QRTZ_TRIGGER_LISTENERS_ibfk_1` FOREIGN KEY (`TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `autoupdatelog` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `date` datetime NOT NULL,
  `updatefile` varchar(255) NOT NULL,
  `status` varchar(45) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `backgroundjob` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nada` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `bug` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `logerror_id` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `description` text NOT NULL,
  `supposed` text NOT NULL,
  `timestamp` int(11) NOT NULL DEFAULT 0,
  `fixer` varchar(32) NOT NULL DEFAULT '',
  `fixtime` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `bundle` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `name` varchar(255) NOT NULL,
  `description` mediumtext NOT NULL,
  `password` varchar(255) NOT NULL,
  `isimported` int(11) NOT NULL DEFAULT 0,
  `keepoldbuilds` int(11) NOT NULL DEFAULT 0,
  `enabledownload` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) DEFAULT NULL,
  `editor` int(11) DEFAULT NULL,
  `edate` int(11) DEFAULT NULL,
  `cdate` int(11) DEFAULT NULL,
  `importfolder_id` int(11) DEFAULT NULL,
  `updateurl` varchar(255) DEFAULT NULL,
  `source` varchar(255) DEFAULT NULL,
  `importgroup_id` int(11) NOT NULL DEFAULT -1,
  `conflictbehavior` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `bundlebuild` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `bundle_id` int(11) NOT NULL,
  `changelog` mediumtext NOT NULL,
  `date` int(11) NOT NULL,
  `filename` varchar(255) DEFAULT NULL,
  `statuscode` int(11) NOT NULL DEFAULT -1,
  `statusmessage` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `bundlecontainedobject` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `bundle_id` int(11) NOT NULL,
  `obj_type` int(11) NOT NULL,
  `obj_id` int(11) NOT NULL,
  `channel_id` int(11) NOT NULL DEFAULT 0,
  `referrer_obj_id` int(11) NOT NULL,
  `accepted` int(11) NOT NULL DEFAULT 0,
  `referrer_obj_type` int(11) NOT NULL,
  `autoadded` int(11) NOT NULL DEFAULT 0,
  `cause_obj_type` int(11) DEFAULT NULL,
  `cause_obj_id` int(11) DEFAULT NULL,
  `excluded` int(11) NOT NULL DEFAULT 0,
  `bundlebuild_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `bundle1` (`obj_type`,`referrer_obj_id`,`referrer_obj_type`,`bundlebuild_id`),
  KEY `bundle2` (`bundle_id`,`bundlebuild_id`,`accepted`,`referrer_obj_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `bundleimport` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `date` int(11) NOT NULL,
  `bundlebuild_id` int(11) NOT NULL,
  `group_id` int(11) NOT NULL DEFAULT -1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `bundleimportconflict` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `bundleimportobject_id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `reason_obj_type` int(11) DEFAULT NULL,
  `reason_obj_id` int(11) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `conflict_behaviour` varchar(255) DEFAULT NULL,
  `recoverable` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `bundleimportobject_id` (`bundleimportobject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `bundleimportobject` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `bundleimport_id` int(11) NOT NULL,
  `obj_type` int(11) NOT NULL,
  `action` varchar(255) DEFAULT NULL,
  `udate` int(11) NOT NULL,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `copy_id` int(11) NOT NULL DEFAULT 0,
  `maxudate` int(11) NOT NULL DEFAULT -1,
  `numobjects` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `bundleimport_id` (`bundleimport_id`),
  KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `changelog_applied` (
  `id` varchar(10) NOT NULL,
  `cluster_member` varchar(255) NOT NULL DEFAULT '',
  `timestamp` int(11) NOT NULL DEFAULT 0,
  `build` varchar(255) NOT NULL DEFAULT '',
  `duration` int(11) NOT NULL DEFAULT 0,
  UNIQUE KEY `id` (`id`,`cluster_member`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `changelog_applied` VALUES ('CH-100','',1299852283,'V5-stable-20110311c',29),('CH-101','',1311333353,'',104),('CH-102','',1311333353,'',49),('CH-103','',1311333353,'',21),('CH-104','',1311333353,'',76),('CH-105','',1311333353,'',43),('CH-106','',1301563294,'V5-stable-20110331a',94),('CH-107','',1303809993,'',42),('CH-108','',1307105501,'',3),('CH-1207','',1311333353,'',314),('CH-1211','',1311333354,'',191),('CH-1212','',1311333354,'',54),('CH-1213','',1311333354,'',68),('CH-1214','',1311333354,'',2),('CH-1215','',1331024520,'',390),('CH-1216','',1331024520,'',152),('CH-1217','',1331024520,'',108),('CH-1220','',1323944482,'',387),('CH-1221','',1320741542,'',135),('CH-1222','',1331024521,'',211),('CH-1223','',1331024521,'',98),('CH-1224','',1323944481,'',50),('CH-1225','',1323944482,'',495),('CH-1226','',1331024520,'',4),('CH-1227','',1331024521,'',5),('CH-1228','',1331024521,'',9),('CH-1230','',1331024521,'',3),('CH-1231','',1331024521,'',4),('CH-1232','',1343646795,'',187),('CH-1233','',1343646796,'',406),('CH-1234','',1334663662,'',1130),('CH-1236','',1334663663,'',1016),('CH-1237','',1334663663,'',1),('CH-1238','',1334766974,'',12),('CH-1239','',1343646795,'',6),('CH-1240','',1343646795,'',63),('CH-1241','',1343646795,'',7),('CH-1242','',1343646795,'',10),('CH-1243','',1343646795,'',5),('CH-1244','',1343646795,'',4),('CH-1245','',1343646795,'',1),('CH-1246','',1343646795,'',7),('CH-1247','',1343646795,'',5),('CH-1248','',1343646795,'',4),('CH-1249','',1343646795,'',10),('CH-1250','',1343646795,'',3),('CH-1251','',1343646795,'',89),('CH-1252','',1343646795,'',42),('CH-1253','',1343646795,'',15),('CH-1254','',1349963856,'',539),('CH-1255','',1349963855,'',2),('CH-1256','',1352730673,'',215),('CH-1258','',1352730674,'',92),('CH-1259','',1352730679,'',5767),('CH-1260','',1352730680,'',296),('CH-1261','',1352730680,'',5),('CH-1262','',1352730680,'',4),('CH-1263','',1361273205,'',62),('CH-1264','',1361273205,'',20),('CH-1265','',1361273205,'',5),('CH-1266','',1361273205,'',1),('CH-1267','',1361273205,'',18),('CH-1268','',1361273205,'',1),('CH-1273','',1362405146,'',3),('CH-1276','',1362405146,'',28),('CH-1277','',1362405146,'',46),('CH-1278','',1362405146,'',4),('CH-1279','',1362405146,'',0),('CH-1280','',1362405146,'',7),('CH-1281','',1362405146,'',21),('CH-1282','',1362405146,'',44),('CH-1283','',1362405146,'',1),('CH-1284','',1362405146,'',22),('CH-1343','',1370335469,'',91),('CH-1345','',1370335470,'',789),('CH-1346','',1370335470,'',39),('CH-1347','',1370335467,'',0),('CH-1348','',1370335470,'',63),('CH-1349','',1370335470,'',349),('CH-1350','',1370335470,'',119),('CH-1351','',1370335470,'',9),('CH-1352','',1370335470,'',0),('CH-1353','',1370335467,'',61),('CH-1355','',1370335470,'',7),('CH-1357','',1370335467,'',295),('CH-1358','',1370335467,'',164),('CH-1359','',1370335467,'',166),('CH-1360','',1370335468,'',982),('CH-1361','',1370335469,'',582),('CH-1362','',1370335467,'',21),('CH-1363','',1370335467,'',21),('CH-1364','',1370335471,'',46),('CH-1365','',1370335471,'',1),('CH-1366','',1370335471,'',103),('CH-1368','',1404809681,'',23),('CH-1371','',1380184078,'',108),('CH-1372','',1380184078,'',96),('CH-1373','',1380184078,'',1),('CH-1374','',1380184078,'',225),('CH-1375','',1380184078,'',195),('CH-1376','',1380184079,'',613),('CH-1377','',1380184079,'',104),('CH-1378','',1380184079,'',83),('CH-1380','',1380184079,'',84),('CH-1381','',1380184079,'',218),('CH-1382','',1380184080,'',217),('CH-1383','',1380184080,'',707),('CH-1384','',1404809681,'',3),('CH-1386','',1380184080,'',1),('CH-1387','',1404809681,'',13),('CH-1388','',1404809681,'',9),('CH-1389','',1404809681,'',131),('CH-1390','',1404809681,'',3),('CH-1391','',1404809681,'',2),('CH-1392','',1404809681,'',1),('CH-1393','',1404809681,'',11),('CH-1394','',1404809681,'',7),('CH-1395','',1404809681,'',30),('CH-1396','',1404809681,'',4),('CH-1397','',1404809681,'',10),('CH-1398','',1404809681,'',74),('CH-1399','',1404809681,'',30),('CH-1404','',1445415693,'',28),('CH-1405','',1404809681,'',4),('CH-1406','',1404809681,'',5),('CH-1410','',1404809681,'',6),('CH-1411','',1404809681,'',2),('CH-1412','',1404809681,'',3),('CH-1413','',1404809681,'',5),('CH-1414','',1404809681,'',2),('CH-1416','',1445415645,'',3),('CH-1417','',1445415649,'',7),('CH-1418','',1445415649,'',2),('CH-1419','',1445415646,'',386),('CH-1420','',1445415693,'',40),('CH-1421','',1445415649,'',2),('CH-1423','',1445415693,'',44),('CH-1424','',1445415693,'',51),('CH-1425','',1445415646,'',3),('CH-1426','',1445415646,'',382),('CH-1427','',1445415646,'',89),('CH-1428','',1445415647,'',311),('CH-1429','',1445415647,'',45),('CH-1430','',1445415647,'',5),('CH-1431','',1445415647,'',23),('CH-1433','',1445415647,'',29),('CH-1434','',1445415649,'',2),('CH-1435','',1445415647,'',174),('CH-1436','',1445415647,'',131),('CH-1437','',1445415647,'',197),('CH-1439','',1445415647,'',11),('CH-1440','',1445415647,'',4),('CH-1441','',1445415647,'',7),('CH-1442','',1445415647,'',7),('CH-1443','',1445415647,'',6),('CH-1444','',1445415647,'',4),('CH-1445','',1445415647,'',94),('CH-1446','',1445415693,'',35),('CH-1447','',1445415647,'',15),('CH-1448','',1445415649,'',13),('CH-1449','',1445415649,'',15),('CH-1450','',1445415649,'',10),('CH-1451','',1445415647,'',3),('CH-1452','',1445415648,'',222),('CH-1453','',1445415649,'',904),('CH-1454','',1445415649,'',49),('CH-1455','',1445415694,'',0),('CH-1456','',1445415694,'',0),('CH-1457','',1445415694,'',0),('CH-1458','',1445415694,'',0),('CH-1459','',1445415694,'',41),('CH-1460','',1445415694,'',60),('CH-1462','',1458140582,'',19),('CH-1463','',1458140582,'',51),('CH-1464','',1458140582,'',22),('CH-1465','',1458140582,'',44),('CH-1466','',1458140582,'',31),('CH-1467','',1458140582,'',39),('CH-1468','',1458140582,'',23),('CH-1469','',1458140582,'',23),('CH-1470','',1458140582,'',28),('CH-1471','',1471954856,'',13),('CH-1472','',1458140582,'',1),('CH-1473','',1458140582,'',1),('CH-1474','',1458140582,'',14),('CH-1475','',1458140582,'',1),('CH-1477','',1471954856,'',15),('CH-1478','',1471954856,'',24),('CH-1479','',1471954856,'',22),('CH-1480','',1471954856,'',19),('CH-1481','',1458140582,'',33),('CH-1482','',1458140582,'',1),('CH-1483','',1471954856,'',7),('CH-1484','',1471954856,'',2),('CH-1485','',1471954856,'',10),('CH-1486','',1471954856,'',1),('CH-1487','',1488930533,'',5),('CH-1488','',1488930533,'',5),('CH-1489','',1488930533,'',4),('CH-1490','',1488930533,'',8),('CH-1492','',1471954856,'',55),('CH-1493','',1471954856,'',39),('CH-1494','',1471954856,'',5),('CH-1495','',1471954856,'',1),('CH-1496','',1562766222,'',7),('CH-1497','',1562766222,'',7),('CH-1498','',1562766222,'',8),('CH-1500','',1471954856,'',16),('CH-1501','',1471954856,'',8),('CH-1502','',1488930533,'',20),('CH-1503','',1488930533,'',8),('CH-1504','',1488930533,'',7),('CH-1506','',1488930533,'',5),('CH-1507','',1488930533,'',5),('CH-1508','',1488930533,'',5),('CH-1509','',1488930533,'',6),('CH-1510','',1488930533,'',5),('CH-1511','',1488930533,'',5),('CH-1512','',1488930533,'',6),('CH-1513','',1488930533,'',5),('CH-1514','',1488930533,'',5),('CH-1515','',1488930533,'',4),('CH-1516','',1488930533,'',5),('CH-1517','',1488930533,'',5),('CH-1518','',1488930533,'',6),('CH-1519','',1488930533,'',5),('CH-1520','',1488930533,'',4),('CH-1521','',1488930533,'',5),('CH-1522','',1488930533,'',5),('CH-1523','',1488930533,'',5),('CH-1524','',1488930533,'',5),('CH-1525','',1488930533,'',5),('CH-1526','',1488930533,'',4),('CH-1527','',1488930533,'',6),('CH-1528','',1488930533,'',5),('CH-1529','',1488930533,'',4),('CH-1530','',1488930533,'',4),('CH-1531','',1488930533,'',4),('CH-1532','',1488930533,'',5),('CH-1533','',1488930533,'',5),('CH-1534','',1488930533,'',5),('CH-1535','',1488930533,'',5),('CH-1536','',1488930533,'',5),('CH-1537','',1488930533,'',5),('CH-1538','',1488930533,'',5),('CH-1539','',1488930533,'',5),('CH-1540','',1488930533,'',5),('CH-1541','',1488930533,'',5),('CH-1542','',1488930533,'',5),('CH-1543','',1488930533,'',5),('CH-1544','',1488930533,'',4),('CH-1545','',1488930533,'',5),('CH-1546','',1488930533,'',5),('CH-1547','',1488930533,'',5),('CH-1548','',1488930533,'',5),('CH-1549','',1488930533,'',5),('CH-1550','',1488930533,'',5),('CH-1551','',1488930533,'',5),('CH-1552','',1488930533,'',6),('CH-1553','',1562766222,'',9),('CH-1555','',1562766222,'',8),('CH-1557','',1562766222,'',8),('CH-1558','',1562766222,'',12),('CH-1559','',1562766222,'',10),('CH-1560','',1562766222,'',9),('CH-1561','',1562766222,'',14),('CH-1562','',1562766222,'',10),('CH-1563','',1562766222,'',9),('CH-1567','',1562766222,'',12),('CH-1568','',1562766222,'',8),('CH-1570','',1562766222,'',10),('CH-1571','',1562766222,'',10),('CH-1572','',1562766222,'',11),('CH-1573','',1562766222,'',11),('CH-1574','',1562766222,'',10),('CH-1575','',1562766222,'',14),('CH-1576','',1562766222,'',6),('CH-1577','',1562766222,'',6),('CH-1579','',1562766222,'',6),('CH-1580','',1562766222,'',6),('CH-1585','',1562766222,'',25),('CH-1587','',1562766222,'',8),('CH-1589','',1562766222,'',13),('CH-1591','',1562766222,'',6),('CH-1592','',1562766222,'',6),('CH-1593','',1562766222,'',6),('CH-1594','',1562766222,'',8),('CH-1602','',1562766222,'',10),('CH-1611','',1562766222,'',7),('CH-1612','',1562766222,'',10),('CH-70','',1289407041,'',0),('CH-71','',1289407042,'',0),('CH-72','',1289407042,'',0),('CH-73','',1289407042,'',0),('CH-74','',1289407042,'',0),('CH-75','',1289407042,'',0),('CH-76','',1289407042,'',0),('CH-77','',1303809993,'',50),('CH-78','',1303809993,'',17),('CH-79','',1303809993,'',5),('CH-80','',1295348178,'V5-stable-20110118a',57),('CH-83','',1303809993,'',44),('CH-84','',1296634859,'V5-stable-20110202',10),('CH-85','',1296634859,'V5-stable-20110202',57),('CH-86','',1311333350,'',334),('CH-87','',1311333351,'',302),('CH-88','',1311333351,'',130),('CH-89','',1311333352,'',1559),('CH-90','',1311333352,'',27),('CH-91','',1303809993,'',45),('CH-92','',1311333353,'',367),('CH-93','',1299775576,'V5-stable-20110310a',3312),('CH-94','',1299775576,'V5-stable-20110310a',780),('CH-95','',1299775576,'V5-stable-20110310a',7),('CH-96','',1299775577,'V5-stable-20110310a',502),('CH-97','',1299775577,'V5-stable-20110310a',142),('CH-99','',1299852283,'V5-stable-20110311c',114);

CREATE TABLE `channelset` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `cn_folder_map` (
  `folder_id` int(11) NOT NULL DEFAULT 0,
  `cn_map_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`folder_id`,`cn_map_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `cn_map` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` char(255) NOT NULL DEFAULT '',
  `keyword` char(255) NOT NULL DEFAULT '',
  `description` char(255) NOT NULL DEFAULT '',
  `open_target` char(255) DEFAULT NULL,
  `page_id` int(11) NOT NULL DEFAULT 0,
  `is_link_int` tinyint(4) DEFAULT NULL,
  `node_id` int(11) NOT NULL DEFAULT 0,
  `mother_id` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `cn_mapprop` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cn_map_id` int(11) NOT NULL DEFAULT 0,
  `ptype` int(11) DEFAULT NULL,
  `pkey` varchar(100) DEFAULT NULL,
  `value` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `cnstatconfig` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `statkeys` text NOT NULL,
  `active` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `cnstatval` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cnstatconfig_id` int(11) NOT NULL DEFAULT 0,
  `timestamp` int(11) NOT NULL DEFAULT 0,
  `field` varchar(255) NOT NULL DEFAULT '',
  `col` int(11) NOT NULL DEFAULT 0,
  `textvalue` text NOT NULL,
  `intvalue` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `cnstatconfig_id` (`cnstatconfig_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `construct` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name_id` int(11) NOT NULL DEFAULT 0,
  `ml_id` int(11) NOT NULL DEFAULT 1,
  `keyword` char(64) NOT NULL DEFAULT '',
  `childable` tinyint(4) NOT NULL DEFAULT 0,
  `intext` tinyint(4) NOT NULL DEFAULT 0,
  `locked` int(11) NOT NULL DEFAULT 0,
  `locked_by` int(11) NOT NULL DEFAULT 0,
  `global` int(11) NOT NULL DEFAULT 0,
  `icon` char(64) NOT NULL DEFAULT '',
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `description_id` int(11) NOT NULL DEFAULT 0,
  `autoenable` int(11) NOT NULL DEFAULT 0,
  `category_id` int(11) DEFAULT NULL,
  `hopedithook` mediumtext DEFAULT NULL,
  `liveeditortagname` varchar(255) DEFAULT NULL,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `construct_category` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name_id` int(11) NOT NULL,
  `sortorder` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `construct_node` (
  `construct_id` int(11) NOT NULL DEFAULT 0,
  `node_id` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `construct_id_2` (`construct_id`,`node_id`),
  KEY `construct_id` (`construct_id`),
  KEY `node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `content` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `node_id` int(11) NOT NULL DEFAULT 0,
  `locked` int(11) NOT NULL DEFAULT 0,
  `locked_by` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contentfile` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` char(255) NOT NULL DEFAULT '',
  `filetype` varchar(255) DEFAULT NULL,
  `folder_id` int(11) NOT NULL DEFAULT 0,
  `filesize` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `description` char(255) NOT NULL DEFAULT '',
  `sizex` int(11) NOT NULL DEFAULT 0,
  `sizey` int(11) NOT NULL DEFAULT 0,
  `md5` char(32) NOT NULL DEFAULT '',
  `dpix` int(11) NOT NULL DEFAULT 0,
  `dpiy` int(11) NOT NULL DEFAULT 0,
  `fpx` float(5,4) NOT NULL DEFAULT 0.5000,
  `fpy` float(5,4) NOT NULL DEFAULT 0.5000,
  `channelset_id` int(11) NOT NULL DEFAULT 0,
  `channel_id` int(11) NOT NULL DEFAULT 0,
  `is_master` tinyint(4) NOT NULL DEFAULT 1,
  `force_online` tinyint(4) NOT NULL DEFAULT 0,
  `mc_exclude` tinyint(1) NOT NULL DEFAULT 0,
  `deleted` int(11) NOT NULL DEFAULT 0,
  `deletedby` int(11) NOT NULL DEFAULT 0,
  `disinherit_default` tinyint(1) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `name` (`name`),
  KEY `folder_id` (`folder_id`),
  KEY `channelset_id` (`channelset_id`,`is_master`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contentfile_disinherit` (
  `contentfile_id` int(11) NOT NULL,
  `channel_id` int(11) NOT NULL,
  PRIMARY KEY (`contentfile_id`,`channel_id`),
  KEY `channel_id` (`channel_id`),
  CONSTRAINT `contentfile_disinherit_ibfk_1` FOREIGN KEY (`contentfile_id`) REFERENCES `contentfile` (`id`) ON DELETE CASCADE,
  CONSTRAINT `contentfile_disinherit_ibfk_2` FOREIGN KEY (`channel_id`) REFERENCES `node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contentfile_online` (
  `contentfile_id` int(11) NOT NULL,
  `node_id` int(11) NOT NULL,
  UNIQUE KEY `node_id` (`node_id`,`contentfile_id`),
  KEY `contentfile_id` (`contentfile_id`),
  KEY `node_id_2` (`node_id`),
  CONSTRAINT `contentfile_online_ibfk_1` FOREIGN KEY (`contentfile_id`) REFERENCES `contentfile` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `contentfile_online_ibfk_2` FOREIGN KEY (`node_id`) REFERENCES `node` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contentfile_processes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `contentfile_id` int(11) DEFAULT NULL,
  `process_key` varchar(100) NOT NULL,
  `data` text DEFAULT NULL,
  `state` enum('NEW','GENERIC_ERROR','INVALID_JSON','INVALID_PROCESS_KEY') NOT NULL DEFAULT 'NEW',
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `contentfile_id` (`contentfile_id`),
  CONSTRAINT `contentfile_processes_ibfk_1` FOREIGN KEY (`contentfile_id`) REFERENCES `contentfile` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contentfiledata` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `contentfile_id` int(11) NOT NULL,
  `binarycontent` longblob DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `contentfile_id` (`contentfile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contentgroup` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` char(255) NOT NULL DEFAULT '',
  `code` char(5) NOT NULL DEFAULT '',
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4;

INSERT INTO `contentgroup` VALUES (1,'Deutsch (German)','de','A547.14625',1312010975),(2,'English','en','A547.14626',1235378905),(3,'język polski (Polish)','pl','A547.14627',1312011149),(4,'čeština (Czech)','cs','A547.14628',1312010960),(5,'Magyar (Hungarian)','hu','A547.14629',1312011089),(6,'Slovenščina (Slovenian)','sl','A547.14630',1312011187),(7,'العربية (Arabic)','ar','A547.14631',1312010729),(8,'Bălgarski esik (Bulgarian)','bg','A547.14632',1312011350),(9,'bosanski jezik (Bosnian)','bs','A547.14633',1312010953),(10,'dansk (Dutch)','da','A547.14634',1312010967),(11,'español (Spanish)','es','A547.14635',1312010982),(12,'eesti keel (Estonian)','et','A547.14636',1312010994),(13,'فارسی (Persian)','fa','A547.14637',1312011002),(14,'suomi (Finnish)','fi','A547.14638',1312011064),(15,'Français (French)','fr','A547.14639',1312011071),(16,'hrvatski (Croatian)','hr','A547.14640',1312011082),(17,'Italiano (Italian)','it','A547.14641',1312011104),(18,'日本語 (Japanese)','ja','A547.14642',1312011117),(19,'한국어 (Korean)','ko','A547.14643',1312011126),(20,'Македонски (Macedonian)','mk','A547.14644',1312011135),(21,'Nederlands (Dutch)','nl','A547.14645',1312011142),(22,'Português (Portugese)','pt','A547.14646',1312011158),(23,'limba româna (Romanian)','ro','A547.14647',1312011165),(24,'русский язык (Russian)','ru','A547.14648',1312011172),(25,'slovenčina (Slovak)','sk','A547.14649',1312011179),(26,'Shqip (Albanian)','sq','A547.14650',1312011195),(27,'српски/srpski (Serbian)','sr','A547.14651',1312011202),(28,'Türkçe (Turkish)','tr','A547.14652',1312011210),(29,'Українська мова (Ukrainian)','uk','A547.14653',1312011234),(30,'Arabic_Egypt','ar_EG','A547.66746',1246971609),(31,'English Canada','en_CA','A547.66747',1246971635),(32,'French_canada','fr_CA','A547.66748',1246971652);

CREATE TABLE `contentrepository` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `crtype` varchar(255) NOT NULL DEFAULT '',
  `dbtype` varchar(255) NOT NULL DEFAULT '',
  `username` varchar(255) NOT NULL DEFAULT '',
  `password` varchar(255) NOT NULL DEFAULT '',
  `url` varchar(255) NOT NULL DEFAULT '',
  `checkdate` int(11) NOT NULL DEFAULT 0,
  `checkstatus` int(11) NOT NULL DEFAULT -1,
  `checkresult` longtext DEFAULT NULL,
  `statusdate` int(11) NOT NULL DEFAULT 0,
  `instant_publishing` tinyint(4) NOT NULL DEFAULT 0,
  `language_information` tinyint(4) NOT NULL DEFAULT 0,
  `permission_information` tinyint(4) NOT NULL DEFAULT 0,
  `permission_property` varchar(255) NOT NULL DEFAULT '',
  `datastatus` tinyint(4) NOT NULL DEFAULT -1,
  `datacheckresult` longtext DEFAULT NULL,
  `basepath` varchar(255) NOT NULL DEFAULT '',
  `diffdelete` tinyint(4) NOT NULL DEFAULT 1,
  `elasticsearch` mediumtext DEFAULT NULL,
  `project_per_node` tinyint(4) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contentrepository_cr_fragment` (
  `contentrepository_id` int(11) NOT NULL DEFAULT 0,
  `cr_fragment_id` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  UNIQUE KEY `uuid` (`uuid`),
  KEY `contentrepository_id` (`contentrepository_id`),
  KEY `cr_fragment_id` (`cr_fragment_id`),
  CONSTRAINT `contentrepository_cr_fragment_ibfk_1` FOREIGN KEY (`contentrepository_id`) REFERENCES `contentrepository` (`id`) ON DELETE CASCADE,
  CONSTRAINT `contentrepository_cr_fragment_ibfk_2` FOREIGN KEY (`cr_fragment_id`) REFERENCES `cr_fragment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contentset` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nada` tinyint(4) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contenttable` (
  `templatetag_id` int(11) NOT NULL DEFAULT 0,
  `contenttag_id` int(11) NOT NULL DEFAULT 0,
  `globaltag_id` int(11) NOT NULL DEFAULT 0,
  `objtag_id` int(11) NOT NULL DEFAULT 0,
  `part_id` int(11) NOT NULL DEFAULT 0,
  `th` tinyint(4) NOT NULL DEFAULT 0,
  `td_f_even` text NOT NULL,
  `td_f_odd` text NOT NULL,
  `th_f` text NOT NULL,
  `td_type` int(11) NOT NULL DEFAULT 0,
  `th_type` int(11) NOT NULL DEFAULT 0,
  `table_f` text NOT NULL,
  KEY `templatetag_id` (`templatetag_id`),
  KEY `contenttag_id` (`contenttag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contenttag` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `content_id` int(11) NOT NULL DEFAULT 0,
  `construct_id` int(11) NOT NULL DEFAULT 0,
  `enabled` tinyint(4) NOT NULL DEFAULT 0,
  `name` char(127) NOT NULL DEFAULT '',
  `unused` tinyint(4) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  `template` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `content_id` (`content_id`),
  KEY `construct_id` (`construct_id`),
  KEY `name` (`name`),
  KEY `content_id_2` (`content_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contenttag_nodeversion` (
  `auto_id` int(11) NOT NULL AUTO_INCREMENT,
  `id` int(11) NOT NULL DEFAULT 0,
  `content_id` int(11) NOT NULL DEFAULT 0,
  `construct_id` int(11) NOT NULL DEFAULT 0,
  `enabled` tinyint(4) NOT NULL DEFAULT 0,
  `name` char(127) NOT NULL DEFAULT '',
  `nodeversiontimestamp` int(11) NOT NULL DEFAULT 0,
  `nodeversion_user` int(11) NOT NULL DEFAULT 0,
  `nodeversionlatest` int(11) NOT NULL DEFAULT 0,
  `nodeversionremoved` int(11) NOT NULL DEFAULT 0,
  `unused` tinyint(4) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL,
  `template` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`auto_id`),
  KEY `id` (`id`),
  KEY `content_id` (`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `cr_fragment` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `cr_fragment_entry` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tagname` varchar(255) NOT NULL DEFAULT '',
  `mapname` varchar(255) NOT NULL DEFAULT '',
  `obj_type` int(11) NOT NULL DEFAULT 0,
  `target_type` int(11) NOT NULL DEFAULT 0,
  `attribute_type` int(11) NOT NULL DEFAULT 0,
  `multivalue` tinyint(4) NOT NULL DEFAULT 0,
  `optimized` tinyint(4) NOT NULL DEFAULT 0,
  `filesystem` tinyint(4) NOT NULL DEFAULT 0,
  `displayfield` tinyint(4) DEFAULT 0,
  `segmentfield` tinyint(4) DEFAULT 0,
  `urlfield` tinyint(4) DEFAULT 0,
  `foreignlink_attribute` varchar(255) NOT NULL DEFAULT '',
  `foreignlink_attribute_rule` varchar(255) NOT NULL DEFAULT '',
  `category` varchar(255) NOT NULL DEFAULT '',
  `elasticsearch` mediumtext DEFAULT NULL,
  `cr_fragment_id` int(11) DEFAULT NULL,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `cr_fragment_id` (`cr_fragment_id`),
  CONSTRAINT `cr_fragment_entry_ibfk_1` FOREIGN KEY (`cr_fragment_id`) REFERENCES `cr_fragment` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `cr_publish_handler` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `contentrepository_id` int(11) NOT NULL,
  `javaclass` varchar(255) NOT NULL DEFAULT '',
  `properties` mediumtext DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `datasource` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `source_type` int(11) NOT NULL DEFAULT 0,
  `name` varchar(255) DEFAULT NULL,
  `param_id` int(11) DEFAULT NULL,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `datasource_nodeversion` (
  `auto_id` int(11) NOT NULL AUTO_INCREMENT,
  `id` int(11) NOT NULL DEFAULT 0,
  `source_type` int(11) NOT NULL DEFAULT 0,
  `name` varchar(255) DEFAULT NULL,
  `param_id` int(11) DEFAULT NULL,
  `nodeversiontimestamp` int(11) NOT NULL DEFAULT 0,
  `nodeversion_user` int(11) NOT NULL DEFAULT 0,
  `nodeversionlatest` int(11) NOT NULL DEFAULT 0,
  `nodeversionremoved` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL,
  PRIMARY KEY (`auto_id`),
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `datasource_value` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `datasource_id` int(11) NOT NULL DEFAULT 0,
  `sorder` int(11) DEFAULT NULL,
  `dskey` varchar(255) DEFAULT NULL,
  `value` text DEFAULT NULL,
  `dsid` int(11) DEFAULT NULL,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `datasource_value_nodeversion` (
  `auto_id` int(11) NOT NULL AUTO_INCREMENT,
  `id` int(11) NOT NULL DEFAULT 0,
  `datasource_id` int(11) NOT NULL DEFAULT 0,
  `sorder` int(11) DEFAULT NULL,
  `dskey` varchar(255) DEFAULT NULL,
  `value` text DEFAULT NULL,
  `dsid` int(11) DEFAULT NULL,
  `nodeversiontimestamp` int(11) NOT NULL DEFAULT 0,
  `nodeversion_user` int(11) NOT NULL DEFAULT 0,
  `nodeversionlatest` int(11) NOT NULL DEFAULT 0,
  `nodeversionremoved` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL,
  PRIMARY KEY (`auto_id`),
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `dependencymap` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mod_type` int(11) NOT NULL DEFAULT 0,
  `mod_id` int(11) NOT NULL DEFAULT 0,
  `mod_event_type` int(11) DEFAULT -1,
  `dep_type` int(11) NOT NULL DEFAULT 0,
  `dep_id` int(11) NOT NULL DEFAULT 0,
  `link_type` int(11) NOT NULL DEFAULT 0,
  `ignore_events` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `mod_type` (`mod_type`,`mod_id`),
  KEY `dep_type` (`dep_type`,`dep_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `dependencymap2` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `mod_obj_type` int(10) unsigned DEFAULT NULL,
  `mod_obj_id` int(10) unsigned DEFAULT NULL,
  `mod_ele_type` int(10) unsigned DEFAULT NULL,
  `mod_ele_id` int(10) unsigned DEFAULT NULL,
  `mod_prop` varchar(255) DEFAULT NULL,
  `dep_obj_type` int(10) unsigned DEFAULT NULL,
  `dep_obj_id` int(10) unsigned DEFAULT NULL,
  `eventmask` int(10) unsigned NOT NULL DEFAULT 0,
  `dep_channel_id` varchar(5000) DEFAULT NULL,
  `dep_prop` varchar(5000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `mod_obj` (`mod_obj_type`,`mod_obj_id`),
  KEY `mod_ele` (`mod_ele_type`,`mod_ele_id`),
  KEY `dep_obj` (`dep_obj_type`,`dep_obj_id`),
  KEY `mod_obj_type` (`mod_obj_type`,`mod_obj_id`,`mod_ele_type`,`mod_ele_id`,`mod_prop`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `dicuser` (
  `output_id` int(11) NOT NULL DEFAULT 0,
  `language_id` int(11) NOT NULL DEFAULT 0,
  `value` text NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `value` (`value`(40),`language_id`),
  KEY `output_id` (`output_id`,`language_id`),
  KEY `output_id_2` (`output_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6608 DEFAULT CHARSET=utf8mb4;

INSERT INTO `dicuser` VALUES (326,1,'Web Adresse',1,'A547.14799',0),(326,2,'Web address',2,'A547.14800',0),(327,1,'Eine Web (http) Adresse.\r\nBeispiel:\r\nhttp://www.gentics.com',3,'A547.14801',0),(327,2,'A web (http) address.\nExample: http://www.gentics.com',4,'A547.14802',0),(301,2,'Text (username, password)',5,'A547.14796',0),(301,1,'Text (Benutzername, Kennwort)',6,'A547.14795',0),(302,1,'Nur Buchstaben oder Zahlen sowie die Zeichen:\r\n _ - . @\r\nMindestens 4 maximal 20 Zeichen.',7,'A547.14797',0),(302,2,'Only characters or numbers and the following:\n _ - . @\nBetween 4 and 20 characters.',8,'A547.14798',0),(285,1,'Text (kurz)',9,'A547.14775',0),(285,2,'Text (short)',10,'A547.14776',0),(286,1,'Beliebiger Text, maximal 255 Zeichen.',11,'A547.14777',0),(286,2,'Arbitrary text with 255 characters maximum.',12,'A547.14778',0),(293,1,'Zahl (ganz)',13,'A547.14791',0),(293,2,'Number (integer)',14,'A547.14792',0),(294,1,'Ganze Zahl, max. 9 Stellen.\r\nBeispiel: -256',15,'A547.14793',0),(294,2,'Integer with maximum of 9 digits.\nExample: -256',16,'A547.14794',0),(288,2,'Arbitrary text with arbitrary length.',17,'A547.14782',0),(287,1,'Text (lang)',18,'A547.14779',0),(288,1,'Beliebiger Text, beliebige Länge.',19,'A547.14781',0),(287,2,'Text (long)',20,'A547.14780',0),(274,2,'Must not be empty.',21,'A547.14774',0),(274,1,'Darf nicht leer sein.',22,'A547.14773',0),(273,2,'Not empty',23,'A547.14772',0),(273,1,'Nicht leer',24,'A547.14771',0),(290,2,'Real number, maximum of 9 digits before comma and 2 digits after comma.\nEnglish notation (\"dot\" as comma)\nExample: 128.56',25,'A547.14786',0),(290,1,'Reelle Zahl, max 9 Vor- und 2 Nachkommastellen.\r\nEnglische Notation: Trennzeichen \"Punkt\".\r\nBeispiel: 128.56',26,'A547.14785',0),(289,2,'Number (real)',27,'A547.14784',0),(289,1,'Zahl (reell)',28,'A547.14783',0),(271,2,'E-mail address',29,'A547.14768',0),(271,1,'E-Mail Adresse',30,'A547.14767',0),(272,2,'E-mail address\nExample: Q&A@cnn.com',31,'A547.14770',0),(272,1,'E-Mail Adresse\r\nBeispiel: Q&A@cnn.com',32,'A547.14769',0),(291,2,'Text (simple)',33,'A547.14788',0),(292,1,'Text mit max. 50 Länge und folgenden Sonderzeichen:\r\nUmlaute, ß, Leerzeichen, Bindestrich, Punkt.',34,'A547.14789',0),(292,2,'Text with maximum of 50 characters containing the following special characters:\nUmlauts, ÃŸ, space, hyphen, dot.',35,'A547.14790',0),(291,1,'Text (einfach)',36,'A547.14787',0),(265,2,'Number (natural)',37,'A547.14764',0),(265,1,'Zahl (natürlich)',38,'A547.14763',0),(266,2,'Natural number with 9 digits maximum.\nExample: 65536',39,'A547.14766',0),(266,1,'Natürliche Zahl maximal 9 Stellen.\r\nBeispiel: 65536',40,'A547.14765',0),(434,2,'Folder name',41,'A547.14804',0),(434,1,'Ordnername',42,'A547.14803',0),(435,2,'Text with maximum of 255 characters containing the following special characters:\nUmlauts, ÃŸ, space, hyphen, dot.',43,'A547.14806',0),(435,1,'Text mit max. 255 Länge und folgenden Sonderzeichen:\r\nUmlaute, ß, Leerzeichen, Bindestrich, Punkt.',44,'A547.14805',0),(593,1,'Dateiname',45,'A547.14813',0),(593,2,'Filename',46,'A547.14814',0),(594,1,'Dateiname ohne Umlaute und Sonderzeichen ausgenommen Zahlen, Punkt, Bindestrich und Unterstrich.\r\nLeerzeichen sind nicht erlaubt.',47,'A547.14815',0),(594,2,'Filename without umlauts or special characters with exception of digits, dot, hyphen and underscore.\nSpaces are not allowed.',48,'A547.14816',0),(787,2,'Date (fix)',49,'A547.14836',0),(788,2,'Date in the format dd.mm.yyyy or dd.mm.yy with delimiters .,_/- and space.',50,'A547.14838',0),(788,1,'Datum im Format dd.mm.yyyy oder dd.mm.yy mit den Trennzeichen .,_/- und Leerzeichen.',51,'A547.14837',0),(787,1,'Datum (fix)',52,'A547.14835',0),(790,2,'Time, in the format hh:mm',53,'A547.14842',0),(790,1,'Uhrzeit, im Format hh:mm',54,'A547.14841',0),(789,2,'Time (fix)',55,'A547.14840',0),(789,1,'Uhrzeit (fix)',56,'A547.14839',0),(792,2,'Time, in the format hh:mm or hh',57,'A547.14846',0),(791,1,'Uhrzeit (alternativ)',58,'A547.14843',0),(791,2,'Time (alternative)',59,'A547.14844',0),(792,1,'Uhrzeit im Format hh:mm oder hh',60,'A547.14845',0),(795,2,'Natural number smaller 60.\nExample: 42',61,'A547.14852',0),(795,1,'Natürliche Zahl kleiner 60.\r\nBeispiel: 42',62,'A547.14851',0),(794,2,'Number (natural, small)',63,'A547.14850',0),(794,1,'Zahl (natürlich, klein)',64,'A547.14849',0),(801,2,'Date in the format DD.MM.YYYY or DD.MM.YY.',65,'A547.14857',0),(801,1,'Datum im Format TT.MM.JJJJ oder TT.MM.JJ.',66,'A547.14856',0),(800,2,'Date (alternative)',67,'A547.14854',0),(800,1,'Datum (alternativ)',68,'A547.14853',0),(800,3,'Datum (alternativ)',69,'A547.14855',0),(801,3,'Datum im Format TT.MM.JJJJ oder TT.MM.JJ.',70,'A547.14858',0),(1021,1,'Verzeichnispfad (Unix) mit Trennzeichen \"/\".',71,'A547.14869',0),(1020,2,'Unix path',72,'A547.14868',0),(1020,1,'Verzeichnispfad',73,'A547.14867',0),(1021,2,'Unix path with delimiter \"/\".',74,'A547.14870',0),(1022,1,'Hostname',75,'A547.14871',0),(1023,1,'Domäne oder IP Adresse.\r\nzB: www.gentics.com',76,'A547.14873',0),(1022,2,'Hostname',77,'A547.14872',0),(1023,2,'Domain name or IP address.\nExample: www.gentics.com',78,'A547.14874',0),(1133,1,'Text (eindeutig)',79,'A547.14895',0),(1133,2,'Text (unique)',80,'A547.14896',0),(1134,1,'Nur Kleinbuchstaben (ohne Sonderzeichen) und Zahlen. Mindestens 3 Zeichen.',81,'A547.14898',0),(1134,2,'Only lowercase characters (no special characters) and digits. Minimum of 3 characters.',82,'A547.14899',0),(1134,3,'Nur Kleinbuchstaben (ohne Sonderzeichen) und Zahlen. Mindestens 3 Zeichen.',83,'A547.14900',0),(1133,3,'Text (eindeutig)',84,'A547.14897',0),(1356,1,'Postleitzahl',85,'A547.14916',0),(1356,2,'Postal code',86,'A547.14917',0),(1356,3,'Telefonnummer/Faxnummer',87,'A547.14918',0),(1357,1,'',88,'A547.14919',0),(1357,2,'',89,'A547.14920',0),(1357,3,'',90,'A547.14921',0),(1358,1,'Telefon/Faxnummer',91,'A547.14922',0),(1358,2,'Phone/faxnumber',92,'A547.14923',0),(1358,3,'Telefon/Faxnummer',93,'A547.14924',0),(1359,1,'Telefon/Faxnummer:\r\n4-25 Ziffern bzw. Sonderzeichen',94,'A547.14925',0),(1359,2,'Phone/faxnumber:\n4-25 digits and special characters',95,'A547.14926',0),(1359,3,'',96,'A547.14927',0),(1360,1,'Preis (SN)',97,'A547.14928',0),(1360,2,'Price (SN)',98,'A547.14929',0),(1360,3,'Preis (SN)',99,'A547.14930',0),(1361,1,'Preis: Max 12 Vorkommastellen und 2 Nachkommastellen.\r\n',100,'A547.14931',0),(1361,2,'Price: maximum of 12 digits before and 2 digits after comma.',101,'A547.14932',0),(1361,3,'',102,'A547.14933',0),(1394,1,'Fileshare',103,'A547.14934',0),(1394,2,'Fileshare',104,'A547.14935',0),(1394,3,'Fileshare',105,'A547.14936',0),(1395,1,'gültiger Fileshare Pfad: zB: \\\\servername\\dir\\file.txt',106,'A547.14937',0),(1395,2,'Fileshare path. Example: \\\\servername\\dir\\file.txt',107,'A547.14938',0),(1395,3,'',108,'A547.14939',0),(1561,1,'Node Tag',109,'A547.15060',0),(1561,2,'Node Tag',110,'A547.15061',0),(1561,3,'Node Tag',111,'A547.15062',0),(1562,1,'Nur Buchstaben, Bindestrich und Unterstrich. 3 bis 255 Zeichen.',112,'A547.15063',0),(1562,2,'Only characters, hyphen and underscore. Between 3 and 255 characters.',113,'A547.15064',0),(1562,3,'Nur Buchstaben, Bindestrich und Unterstrich. 3 bis 255 Zeichen.',114,'A547.15065',0);

CREATE TABLE `dirtanalysis` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sid` varchar(26) DEFAULT NULL,
  `finished` tinyint(4) NOT NULL DEFAULT 0,
  `timestamp` int(11) NOT NULL,
  `analysis` longtext DEFAULT NULL,
  `jstree_analysis` longtext DEFAULT NULL,
  `dirted` int(11) NOT NULL DEFAULT 0,
  `obj_type` int(11) NOT NULL DEFAULT 0,
  `obj_id` int(11) NOT NULL DEFAULT 0,
  `action` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `dirtqueue` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `timestamp` int(10) unsigned NOT NULL DEFAULT 0,
  `obj_type` int(10) unsigned DEFAULT NULL,
  `obj_id` int(10) unsigned DEFAULT NULL,
  `events` int(10) unsigned DEFAULT NULL,
  `property` mediumtext DEFAULT NULL,
  `simulation` tinyint(4) DEFAULT 0,
  `sid` varchar(26) DEFAULT NULL,
  `failed` tinyint(4) NOT NULL DEFAULT 0,
  `failreason` mediumtext DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ds` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `templatetag_id` int(11) NOT NULL DEFAULT 0,
  `contenttag_id` int(11) NOT NULL DEFAULT 0,
  `globaltag_id` int(11) NOT NULL DEFAULT 0,
  `o_type` int(11) NOT NULL DEFAULT 0,
  `is_folder` tinyint(4) NOT NULL DEFAULT 0,
  `orderkind` tinyint(4) NOT NULL DEFAULT 0,
  `orderway` tinyint(4) NOT NULL DEFAULT 0,
  `max_obj` int(11) NOT NULL DEFAULT 0,
  `recursion` tinyint(4) NOT NULL DEFAULT 0,
  `objtag_id` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `templatetag_id` (`templatetag_id`),
  KEY `objtag_id` (`objtag_id`),
  KEY `is_folder` (`is_folder`),
  KEY `contenttag_id` (`contenttag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ds_nodeversion` (
  `auto_id` int(11) NOT NULL AUTO_INCREMENT,
  `id` int(11) NOT NULL DEFAULT 0,
  `templatetag_id` int(11) NOT NULL DEFAULT 0,
  `contenttag_id` int(11) NOT NULL DEFAULT 0,
  `globaltag_id` int(11) NOT NULL DEFAULT 0,
  `o_type` int(11) NOT NULL DEFAULT 0,
  `is_folder` tinyint(4) NOT NULL DEFAULT 0,
  `orderkind` tinyint(4) NOT NULL DEFAULT 0,
  `orderway` tinyint(4) NOT NULL DEFAULT 0,
  `max_obj` int(11) NOT NULL DEFAULT 0,
  `recursion` tinyint(4) NOT NULL DEFAULT 0,
  `nodeversiontimestamp` int(11) NOT NULL DEFAULT 0,
  `nodeversion_user` int(11) NOT NULL DEFAULT 0,
  `nodeversionlatest` int(11) NOT NULL DEFAULT 0,
  `nodeversionremoved` int(11) NOT NULL DEFAULT 0,
  `objtag_id` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL,
  PRIMARY KEY (`auto_id`),
  KEY `id` (`id`),
  KEY `contenttag_id` (`contenttag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ds_obj` (
  `ds_id` int(11) NOT NULL DEFAULT 0,
  `templatetag_id` int(11) NOT NULL DEFAULT 0,
  `contenttag_id` int(11) NOT NULL DEFAULT 0,
  `globaltag_id` int(11) NOT NULL DEFAULT 0,
  `o_id` int(11) NOT NULL DEFAULT 0,
  `node_id` int(11) NOT NULL DEFAULT 0,
  `obj_order` smallint(6) NOT NULL DEFAULT 0,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `adate` int(11) NOT NULL DEFAULT 0,
  `auser` int(11) NOT NULL DEFAULT 0,
  `objtag_id` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `o_id` (`o_id`),
  KEY `contenttag_id` (`contenttag_id`),
  KEY `templatetag_id` (`templatetag_id`),
  KEY `objtag_id` (`objtag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ds_obj_nodeversion` (
  `auto_id` int(11) NOT NULL AUTO_INCREMENT,
  `id` int(11) NOT NULL DEFAULT 0,
  `ds_id` int(11) NOT NULL DEFAULT 0,
  `templatetag_id` int(11) NOT NULL DEFAULT 0,
  `contenttag_id` int(11) NOT NULL DEFAULT 0,
  `globaltag_id` int(11) NOT NULL DEFAULT 0,
  `o_id` int(11) NOT NULL DEFAULT 0,
  `node_id` int(11) NOT NULL DEFAULT 0,
  `obj_order` smallint(6) NOT NULL DEFAULT 0,
  `nodeversiontimestamp` int(11) NOT NULL DEFAULT 0,
  `nodeversion_user` int(11) NOT NULL DEFAULT 0,
  `nodeversionlatest` int(11) NOT NULL DEFAULT 0,
  `nodeversionremoved` int(11) NOT NULL DEFAULT 0,
  `adate` int(11) NOT NULL DEFAULT 0,
  `auser` int(11) NOT NULL DEFAULT 0,
  `objtag_id` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL,
  PRIMARY KEY (`auto_id`),
  KEY `id` (`id`),
  KEY `contenttag_id` (`contenttag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `eventprop` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `triggerevent_id` int(11) NOT NULL DEFAULT 0,
  `keyword` varchar(255) NOT NULL DEFAULT '',
  `value` varchar(255) NOT NULL DEFAULT '',
  `info` text NOT NULL,
  `mapkey` varchar(255) NOT NULL DEFAULT '',
  `workflowlink_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `eventpropeditable` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `eventprop_id` int(11) NOT NULL DEFAULT 0,
  `description` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `faqcontent` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` text DEFAULT NULL,
  `description` text DEFAULT NULL,
  `maintext` text DEFAULT NULL,
  `keywords` text DEFAULT NULL,
  `url` text DEFAULT NULL,
  `folder_id` int(11) DEFAULT NULL,
  `foldername` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `faqinfo` (
  `faq_id` int(11) DEFAULT NULL,
  `visited` int(11) DEFAULT NULL,
  `time` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `faqsearch` (
  `search` varchar(255) DEFAULT NULL,
  `counter` int(11) DEFAULT NULL,
  `timestamp` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `faqvote` (
  `faq_id` int(11) DEFAULT NULL,
  `vote` int(11) DEFAULT NULL,
  `time` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `folder` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mother` int(11) NOT NULL DEFAULT 0,
  `name` char(255) NOT NULL DEFAULT '',
  `type_id` smallint(6) NOT NULL DEFAULT 0,
  `pub_dir` char(255) NOT NULL DEFAULT '',
  `node_id` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `description` char(255) NOT NULL DEFAULT '',
  `startpage_id` int(11) NOT NULL DEFAULT 0,
  `master_id` int(11) NOT NULL DEFAULT 0,
  `channelset_id` int(11) NOT NULL DEFAULT 0,
  `channel_id` int(11) NOT NULL DEFAULT 0,
  `is_master` tinyint(4) NOT NULL DEFAULT 1,
  `mc_exclude` tinyint(1) NOT NULL DEFAULT 0,
  `deleted` int(11) NOT NULL DEFAULT 0,
  `deletedby` int(11) NOT NULL DEFAULT 0,
  `disinherit_default` tinyint(1) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `mother` (`mother`),
  KEY `type_id` (`type_id`),
  KEY `node_id` (`node_id`),
  KEY `startpage_id` (`startpage_id`),
  KEY `channelset_id` (`channelset_id`,`is_master`),
  KEY `master_id` (`master_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `folder_disinherit` (
  `folder_id` int(11) NOT NULL,
  `channel_id` int(11) NOT NULL,
  PRIMARY KEY (`folder_id`,`channel_id`),
  KEY `channel_id` (`channel_id`),
  CONSTRAINT `folder_disinherit_ibfk_1` FOREIGN KEY (`folder_id`) REFERENCES `folder` (`id`) ON DELETE CASCADE,
  CONSTRAINT `folder_disinherit_ibfk_2` FOREIGN KEY (`channel_id`) REFERENCES `node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `folder_processes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `folder_id` int(11) DEFAULT NULL,
  `process_key` varchar(100) NOT NULL,
  `data` text DEFAULT NULL,
  `state` enum('NEW','GENERIC_ERROR','INVALID_JSON','INVALID_PROCESS_KEY') NOT NULL DEFAULT 'NEW',
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `folder_id` (`folder_id`),
  CONSTRAINT `folder_processes_ibfk_1` FOREIGN KEY (`folder_id`) REFERENCES `folder` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `globalprefix` (
  `id` int(11) NOT NULL DEFAULT 1,
  `globalprefix` varchar(4) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `globalprefix` VALUES (1,'82C9');

CREATE TABLE `globaltag` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `construct_id` int(11) NOT NULL DEFAULT 0,
  `name` char(255) NOT NULL DEFAULT '',
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `description` char(255) NOT NULL DEFAULT '',
  `enabled` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `globaltag_node` (
  `globaltag_id` int(11) NOT NULL DEFAULT 0,
  `node_id` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `halt` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `imagestoreimage` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `contentfile_id` int(11) DEFAULT NULL,
  `transform` varchar(255) NOT NULL,
  `edate` int(11) DEFAULT NULL,
  `hash` char(40) DEFAULT NULL,
  `hash_orig` char(40) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `taskdesc` (`contentfile_id`,`transform`),
  CONSTRAINT `imagestoreimage_ibfk_1` FOREIGN KEY (`contentfile_id`) REFERENCES `contentfile` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `imagestoretarget` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `imagestoreimage_id` int(11) NOT NULL,
  `node_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `targetdesc` (`imagestoreimage_id`,`node_id`),
  KEY `node_id` (`node_id`),
  CONSTRAINT `imagestoretarget_ibfk_1` FOREIGN KEY (`imagestoreimage_id`) REFERENCES `imagestoreimage` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `imagestoretarget_ibfk_2` FOREIGN KEY (`node_id`) REFERENCES `node` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `imgresizecache` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `filename` varchar(255) NOT NULL DEFAULT '',
  `normprops` text NOT NULL,
  `keyprop` text NOT NULL,
  `cachefile` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `filename` (`filename`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `import_page` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `import_id` int(11) NOT NULL DEFAULT 0,
  `page_id` int(11) NOT NULL DEFAULT 0,
  `url` varchar(255) NOT NULL DEFAULT '',
  `type` varchar(5) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `import_part` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `import_page_id` int(11) NOT NULL DEFAULT 0,
  `tag_id` int(11) NOT NULL DEFAULT 0,
  `part_id` int(11) NOT NULL DEFAULT 0,
  `type_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `invokerqueue` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(20) NOT NULL,
  `idparam` varchar(200) NOT NULL,
  `additionalparams` varchar(255) NOT NULL,
  `date` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `type` (`type`,`idparam`),
  KEY `date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `job` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `task_id` int(11) NOT NULL DEFAULT 0,
  `schedule_type` varchar(20) DEFAULT NULL,
  `schedule_data` text DEFAULT NULL,
  `status` tinyint(4) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) DEFAULT NULL,
  `edate` int(11) DEFAULT NULL,
  `parallel` tinyint(1) NOT NULL DEFAULT 0,
  `failedemails` varchar(255) NOT NULL DEFAULT '',
  `last_valid_jobrun_id` int(11) DEFAULT NULL,
  `jobruncount` int(11) NOT NULL DEFAULT 0,
  `jobrunaverage` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `jobdependency` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `jobid` int(11) DEFAULT NULL,
  `depid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;








CREATE TABLE `jobrun` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `job_id` int(11) DEFAULT NULL,
  `starttime` int(11) DEFAULT NULL,
  `endtime` int(11) DEFAULT NULL,
  `returnvalue` int(11) DEFAULT NULL,
  `valid` tinyint(4) DEFAULT NULL,
  `output` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `job_id` (`job_id`,`valid`),
  KEY `job_id_2` (`job_id`,`valid`,`endtime`),
  KEY `job_id_3` (`job_id`,`valid`,`starttime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `language` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` char(255) DEFAULT NULL,
  `active` int(11) NOT NULL DEFAULT 1,
  `short` char(2) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4;




INSERT INTO `language` VALUES (1,'Deutsch',1,'de'),(2,'English',1,'en'),(3,'Meta',0,'');





CREATE TABLE `liveeditorsavelog` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tagname` varchar(255) DEFAULT NULL,
  `page_id` int(11) NOT NULL DEFAULT 0,
  `part_id` int(11) NOT NULL DEFAULT 0,
  `value_posted` text DEFAULT NULL,
  `value_parsed` text DEFAULT NULL,
  `value_ieparsed` text DEFAULT NULL,
  `value_pretidy` text DEFAULT NULL,
  `value_tidy` text DEFAULT NULL,
  `value_posttidy` text DEFAULT NULL,
  `value_tosave` text DEFAULT NULL,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `timestamp` int(11) NOT NULL DEFAULT 0,
  `oldlength` int(11) NOT NULL DEFAULT 0,
  `newlength` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `logcmd` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `cmd_desc_id` int(11) NOT NULL DEFAULT 0,
  `o_type` int(11) NOT NULL DEFAULT 0,
  `o_id` int(11) NOT NULL DEFAULT 0,
  `o_id2` int(11) NOT NULL DEFAULT 0,
  `timestamp` int(11) NOT NULL DEFAULT 0,
  `info` varchar(255) NOT NULL DEFAULT '',
  `insert_timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `o_type` (`o_type`),
  KEY `user_id_2` (`user_id`,`o_type`),
  KEY `o_id` (`o_id`),
  KEY `o_id_2` (`o_id`,`o_type`,`timestamp`,`cmd_desc_id`),
  KEY `o_id_3` (`o_id`,`o_type`,`timestamp`),
  KEY `timestamp` (`timestamp`,`cmd_desc_id`,`o_type`),
  KEY `cmd_desc_id` (`cmd_desc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `logerror` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sid` varchar(15) NOT NULL DEFAULT '',
  `user_id` int(11) NOT NULL DEFAULT 0,
  `halt_id` int(11) NOT NULL DEFAULT 0,
  `request` text NOT NULL,
  `errordo` int(11) NOT NULL DEFAULT 0,
  `timestamp` int(11) NOT NULL DEFAULT 0,
  `detail` text NOT NULL,
  `stacktrace` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `logevent` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `logevent_id` int(11) NOT NULL DEFAULT 0,
  `timestamp_start` int(11) NOT NULL DEFAULT 0,
  `timestamp_end` int(11) NOT NULL DEFAULT 0,
  `timestamp_request` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `do` int(11) NOT NULL DEFAULT 0,
  `magic` int(11) NOT NULL DEFAULT 0,
  `module` varchar(16) NOT NULL DEFAULT '',
  `obj_type` int(11) NOT NULL DEFAULT 0,
  `obj_command` varchar(32) NOT NULL DEFAULT '',
  `status` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `loginfo` (
  `logevent_id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(32) NOT NULL DEFAULT '',
  `value_int` int(11) NOT NULL DEFAULT 0,
  `value_text` varchar(255) NOT NULL DEFAULT '',
  `obj_type` int(11) NOT NULL DEFAULT 0,
  `obj_id` int(11) NOT NULL DEFAULT 0,
  KEY `logevent_id` (`logevent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `logrequest` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `request` text NOT NULL,
  `timestamp` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `migrationjob` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `job_id` int(11) NOT NULL,
  `job_type` tinyint(4) DEFAULT NULL,
  `job_status` tinyint(4) NOT NULL,
  `start_timestamp` int(11) NOT NULL,
  `job_config` mediumtext NOT NULL,
  `log_name` char(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `migrationjob_item` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `job_id` int(11) NOT NULL,
  `obj_id` int(11) NOT NULL,
  `obj_type` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `missingreference` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `source_tablename` varchar(50) NOT NULL,
  `source_id` int(10) unsigned NOT NULL,
  `reference_name` varchar(255) NOT NULL,
  `target_uuid` varchar(41) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `source` (`source_tablename`,`source_id`),
  KEY `reference` (`source_tablename`,`source_id`,`reference_name`),
  KEY `target_uuid` (`target_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `ml` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `ext` varchar(8) NOT NULL DEFAULT '',
  `contenttype` varchar(63) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4;




INSERT INTO `ml` VALUES (1,'HTML','html','text/html'),(2,'PHP','php','text/html'),(4,'CGI-Script','cgi','text/html'),(5,'Perl-Script','pl','text/html'),(6,'ASP','asp','text/html'),(7,'JSP','jsp','text/html'),(8,'WML','wml','text/xml'),(9,'CSS','css','text/css'),(10,'JavaScript','js','text/js'),(11,'XML','xml','text/xml'),(12,'XSL','xsl','text/xml'),(13,'Text','txt','text/plain'),(14,'XHTML','xhtml','text/html'),(15,'ASP.NET','aspx','text/html'),(16,'HTM','htm','text/html'),(17,'RSS','rss','application/rss+xml'),(18,'INC','inc','text/plain'),(19,'JSON','json','application/json');





CREATE TABLE `msg` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `from_user_id` int(11) NOT NULL DEFAULT 0,
  `to_user_id` int(11) NOT NULL DEFAULT 0,
  `msg` text NOT NULL,
  `oldmsg` int(11) NOT NULL DEFAULT 0,
  `timestamp` int(11) NOT NULL DEFAULT 0,
  `instanttime` int(255) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `from_user_id` (`from_user_id`),
  KEY `to_user_id` (`to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `node` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `folder_id` int(11) NOT NULL DEFAULT 0,
  `pub_dir` varchar(255) NOT NULL DEFAULT '',
  `pub_dir_bin` varchar(255) NOT NULL DEFAULT '',
  `pub_dir_segment` int(11) DEFAULT 0,
  `host` varchar(255) NOT NULL DEFAULT '',
  `https` tinyint(4) NOT NULL DEFAULT 0,
  `ftphost` varchar(255) NOT NULL DEFAULT '',
  `ftplogin` varchar(255) NOT NULL DEFAULT '',
  `ftppassword` varchar(255) NOT NULL DEFAULT '',
  `ftpsync` tinyint(4) NOT NULL DEFAULT 0,
  `ftpwwwroot` varchar(255) NOT NULL DEFAULT '',
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `utf8` int(11) NOT NULL DEFAULT 0,
  `publish_fs` int(11) NOT NULL DEFAULT 0,
  `publish_fs_pages` tinyint(1) DEFAULT 1,
  `publish_fs_files` tinyint(1) DEFAULT 1,
  `publish_contentmap` int(11) NOT NULL DEFAULT 0,
  `publish_contentmap_pages` tinyint(1) DEFAULT 1,
  `publish_contentmap_files` tinyint(1) DEFAULT 1,
  `publish_contentmap_folders` tinyint(1) DEFAULT 1,
  `contentmap_handle` varchar(255) NOT NULL DEFAULT '',
  `disable_publish` tinyint(1) NOT NULL DEFAULT 0,
  `contentrepository_id` int(11) DEFAULT NULL,
  `editorversion` tinyint(4) DEFAULT 0,
  `default_file_folder_id` int(11) NOT NULL DEFAULT 0,
  `default_image_folder_id` int(11) NOT NULL DEFAULT 0,
  `urlrenderway_pages` tinyint(2) DEFAULT 0,
  `urlrenderway_files` tinyint(2) DEFAULT 0,
  `mesh_preview_url` varchar(255) NOT NULL DEFAULT '',
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `folder_id` (`folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `node_contentgroup` (
  `node_id` int(11) NOT NULL DEFAULT 0,
  `contentgroup_id` int(11) NOT NULL DEFAULT 0,
  `sortorder` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `node_feature` (
  `node_id` int(11) NOT NULL,
  `feature` varchar(255) NOT NULL,
  UNIQUE KEY `node_id` (`node_id`,`feature`),
  KEY `node_id_2` (`node_id`),
  CONSTRAINT `node_feature_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `node` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `node_package` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `node_id` int(11) NOT NULL,
  `package` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `node_id` (`node_id`,`package`),
  CONSTRAINT `node_package_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `node_pdate` (
  `node_id` int(11) NOT NULL,
  `pdate` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`node_id`),
  CONSTRAINT `node_pdate_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `node` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `nodecache` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parser` varchar(127) NOT NULL DEFAULT '',
  `type` varchar(127) NOT NULL DEFAULT '',
  `keyname` varchar(255) NOT NULL DEFAULT '',
  `properties` varchar(255) NOT NULL DEFAULT '',
  `tagkey` text NOT NULL,
  `input` mediumtext NOT NULL,
  `output` mediumtext NOT NULL,
  `lastupdate` int(11) NOT NULL DEFAULT 0,
  `reload` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `nodecache_event` (
  `nodecache_id` int(11) NOT NULL DEFAULT 0,
  `o_id` int(11) NOT NULL DEFAULT 0,
  `o_type` int(11) NOT NULL DEFAULT 0,
  `eventtype` int(11) NOT NULL DEFAULT 0,
  `ignoreevent` int(11) NOT NULL DEFAULT 0,
  KEY `nodecache_id` (`nodecache_id`,`o_id`,`o_type`,`eventtype`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `nodecache_object` (
  `nodecache_id` int(11) NOT NULL DEFAULT 0,
  `eventtype` int(11) NOT NULL DEFAULT 0,
  `o_id` int(11) NOT NULL DEFAULT 0,
  `o_type` int(11) NOT NULL DEFAULT 0,
  `linktype` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `nodesetup` (
  `name` char(255) NOT NULL DEFAULT '',
  `intvalue` int(11) NOT NULL DEFAULT 0,
  `textvalue` char(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;




INSERT INTO `nodesetup` VALUES ('job:syncdefaultpackage',10000,''),('maintenancemode',0,''),('triggerversion',14,'');





CREATE TABLE `nodeversion` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `timestamp` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `o_type` int(11) NOT NULL DEFAULT 0,
  `o_id` int(11) NOT NULL DEFAULT 0,
  `nodeversion` varchar(10) DEFAULT NULL,
  `published` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `o_id` (`o_id`,`o_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `objprop` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name_id` int(11) DEFAULT NULL,
  `description_id` int(11) DEFAULT NULL,
  `o_type` int(11) NOT NULL DEFAULT 0,
  `keyword` char(64) NOT NULL DEFAULT '',
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `objtag_id` int(11) NOT NULL,
  `category_id` int(11) NOT NULL,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `objtag_id` (`objtag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `objprop_category` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name_id` int(11) NOT NULL,
  `sortorder` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `objprop_node` (
  `objprop_id` int(11) NOT NULL,
  `node_id` int(11) NOT NULL,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  PRIMARY KEY (`objprop_id`,`node_id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `objprop_id` (`objprop_id`),
  KEY `node_id` (`node_id`),
  CONSTRAINT `objprop_node_ibfk_1` FOREIGN KEY (`objprop_id`) REFERENCES `objprop` (`id`),
  CONSTRAINT `objprop_node_ibfk_2` FOREIGN KEY (`node_id`) REFERENCES `node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `objtag` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `obj_id` int(11) NOT NULL DEFAULT 0,
  `obj_type` int(11) NOT NULL DEFAULT 0,
  `construct_id` int(11) NOT NULL DEFAULT 0,
  `enabled` tinyint(4) NOT NULL DEFAULT 0,
  `name` char(255) NOT NULL DEFAULT '',
  `intag` int(11) NOT NULL DEFAULT 0,
  `inheritable` int(11) NOT NULL DEFAULT 0,
  `required` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `obj_id` (`obj_id`),
  KEY `obj_type` (`obj_type`),
  KEY `name` (`name`),
  KEY `intag` (`intag`),
  KEY `name_2` (`name`,`obj_id`,`obj_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `objtranslatecache` (
  `o_id` int(11) NOT NULL DEFAULT 0,
  `o_type` int(11) NOT NULL DEFAULT 0,
  `cachekey` varchar(255) NOT NULL DEFAULT '',
  `value_tags` mediumtext NOT NULL,
  KEY `cachekey` (`cachekey`),
  KEY `o_id` (`o_id`,`o_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `outputuser` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `info` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=3705 DEFAULT CHARSET=utf8mb4;




INSERT INTO `outputuser` VALUES (265,9,'A547.17749',0),(266,9,'A547.17750',0),(271,9,'A547.17751',0),(272,9,'A547.17752',0),(273,9,'A547.17753',0),(274,9,'A547.17754',0),(285,9,'A547.17755',0),(286,9,'A547.17756',0),(287,9,'A547.17757',0),(288,9,'A547.17758',0),(289,9,'A547.17759',0),(290,9,'A547.17760',0),(291,9,'A547.17761',0),(292,9,'A547.17762',0),(293,9,'A547.17763',0),(294,9,'A547.17764',0),(301,9,'A547.17765',0),(302,9,'A547.17766',0),(326,9,'A547.17767',0),(327,9,'A547.17768',0),(434,9,'A547.17769',0),(435,9,'A547.17770',0),(593,9,'A547.17773',0),(594,9,'A547.17774',0),(787,9,'A547.17781',0),(788,9,'A547.17782',0),(789,9,'A547.17783',0),(790,9,'A547.17784',0),(791,9,'A547.17785',0),(792,9,'A547.17786',0),(794,9,'A547.17787',0),(795,9,'A547.17788',0),(800,9,'A547.17789',0),(801,9,'A547.17790',0),(1020,9,'A547.17794',0),(1021,9,'A547.17795',0),(1022,9,'A547.17796',0),(1023,9,'A547.17797',0),(1133,9,'A547.17804',0),(1134,9,'A547.17805',0),(1356,9,'A547.17810',0),(1357,9,'A547.17811',0),(1358,9,'A547.17812',0),(1359,9,'A547.17813',0),(1360,9,'A547.17814',0),(1361,9,'A547.17815',0),(1394,9,'A547.17816',0),(1395,9,'A547.17817',0),(1561,9,'A547.17832',0),(1562,9,'A547.17833',0);





CREATE TABLE `page` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` char(255) NOT NULL DEFAULT '',
  `nice_url` varchar(255) DEFAULT NULL,
  `description` char(255) NOT NULL DEFAULT '',
  `filename` char(64) NOT NULL DEFAULT '',
  `priority` tinyint(4) NOT NULL DEFAULT 1,
  `status` tinyint(4) NOT NULL DEFAULT 0,
  `online` tinyint(4) NOT NULL DEFAULT 0,
  `time_start` int(11) NOT NULL DEFAULT 0,
  `time_end` int(11) NOT NULL DEFAULT 0,
  `time_mon` tinyint(4) NOT NULL DEFAULT 1,
  `time_tue` tinyint(4) NOT NULL DEFAULT 1,
  `time_wed` tinyint(4) NOT NULL DEFAULT 1,
  `time_thu` tinyint(4) NOT NULL DEFAULT 1,
  `time_fri` tinyint(4) NOT NULL DEFAULT 1,
  `time_sat` tinyint(4) NOT NULL DEFAULT 1,
  `time_sun` tinyint(4) NOT NULL DEFAULT 1,
  `content_id` int(11) DEFAULT 0,
  `template_id` int(11) NOT NULL DEFAULT 0,
  `folder_id` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `pdate` int(11) NOT NULL DEFAULT 0,
  `publisher` int(11) NOT NULL DEFAULT 0,
  `time_pub` int(11) NOT NULL DEFAULT 0,
  `contentgroup_id` int(11) NOT NULL DEFAULT 0,
  `contentset_id` int(11) NOT NULL DEFAULT 0,
  `delay_publish` int(11) NOT NULL DEFAULT 0,
  `ddate` int(11) NOT NULL DEFAULT 0,
  `channelset_id` int(11) NOT NULL DEFAULT 0,
  `channel_id` int(11) NOT NULL DEFAULT 0,
  `sync_page_id` int(11) NOT NULL DEFAULT 0,
  `sync_timestamp` int(11) NOT NULL DEFAULT 0,
  `is_master` tinyint(4) NOT NULL DEFAULT 1,
  `mc_exclude` tinyint(1) NOT NULL DEFAULT 0,
  `deleted` int(11) NOT NULL DEFAULT 0,
  `deletedby` int(11) NOT NULL DEFAULT 0,
  `disinherit_default` tinyint(1) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `content_id` (`content_id`),
  KEY `folder_id` (`folder_id`),
  KEY `template_id` (`template_id`),
  KEY `name` (`name`),
  KEY `contentset_id` (`contentset_id`),
  KEY `contentgroup_id` (`contentgroup_id`),
  KEY `channelset_id` (`channelset_id`,`is_master`),
  KEY `nice_url` (`nice_url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `page_disinherit` (
  `page_id` int(11) NOT NULL,
  `channel_id` int(11) NOT NULL,
  PRIMARY KEY (`page_id`,`channel_id`),
  KEY `channel_id` (`channel_id`),
  CONSTRAINT `page_disinherit_ibfk_1` FOREIGN KEY (`page_id`) REFERENCES `page` (`id`) ON DELETE CASCADE,
  CONSTRAINT `page_disinherit_ibfk_2` FOREIGN KEY (`channel_id`) REFERENCES `node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `page_nodeversion` (
  `auto_id` int(11) NOT NULL AUTO_INCREMENT,
  `id` int(11) NOT NULL DEFAULT 0,
  `name` char(255) NOT NULL DEFAULT '',
  `nice_url` varchar(255) DEFAULT NULL,
  `description` char(255) NOT NULL DEFAULT '',
  `filename` char(64) NOT NULL DEFAULT '',
  `priority` tinyint(4) NOT NULL DEFAULT 1,
  `content_id` int(11) DEFAULT 0,
  `template_id` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `publisher` int(11) NOT NULL DEFAULT 0,
  `contentgroup_id` int(11) NOT NULL DEFAULT 0,
  `contentset_id` int(11) NOT NULL DEFAULT 0,
  `nodeversiontimestamp` int(11) NOT NULL DEFAULT 0,
  `nodeversion_user` int(11) NOT NULL DEFAULT 0,
  `nodeversionlatest` int(11) NOT NULL DEFAULT 0,
  `nodeversionremoved` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL,
  PRIMARY KEY (`auto_id`),
  KEY `id` (`id`),
  KEY `content_id` (`content_id`),
  KEY `template_id` (`template_id`),
  KEY `name` (`name`),
  KEY `contentset_id` (`contentset_id`),
  KEY `contentgroup_id` (`contentgroup_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `page_processes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `page_id` int(11) DEFAULT NULL,
  `process_key` varchar(100) NOT NULL,
  `data` text DEFAULT NULL,
  `state` enum('NEW','GENERIC_ERROR','INVALID_JSON','INVALID_PROCESS_KEY') NOT NULL DEFAULT 'NEW',
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `page_id` (`page_id`),
  CONSTRAINT `page_processes_ibfk_1` FOREIGN KEY (`page_id`) REFERENCES `page` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `page_task` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `page_id` int(11) NOT NULL,
  `task_id` varchar(255) NOT NULL,
  `complete` varchar(255) NOT NULL DEFAULT '',
  `handle` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `page_index` (`page_id`),
  CONSTRAINT `page_task_ibfk_1` FOREIGN KEY (`page_id`) REFERENCES `page` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `param` (
  `paramid` int(11) DEFAULT NULL,
  `name` varchar(30) DEFAULT NULL,
  `value` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `part` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `construct_id` int(11) NOT NULL DEFAULT 0,
  `type_id` int(11) NOT NULL DEFAULT 0,
  `name_id` int(11) NOT NULL DEFAULT 0,
  `required` tinyint(4) NOT NULL DEFAULT 0,
  `editable` tinyint(4) NOT NULL DEFAULT 0,
  `partoption_id` int(11) NOT NULL DEFAULT 0,
  `partorder` smallint(6) NOT NULL DEFAULT 1,
  `keyword` varchar(64) NOT NULL DEFAULT '',
  `hidden` tinyint(4) NOT NULL DEFAULT 0,
  `ml_id` int(11) DEFAULT 0,
  `info_int` int(11) NOT NULL DEFAULT 0,
  `info_text` text DEFAULT NULL,
  `policy` varchar(255) DEFAULT '',
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `construct_id` (`construct_id`),
  KEY `partorder` (`partorder`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `perm` (
  `usergroup_id` int(11) NOT NULL DEFAULT 0,
  `o_type` int(11) NOT NULL DEFAULT 0,
  `o_id` int(11) NOT NULL DEFAULT 0,
  `perm` char(32) NOT NULL DEFAULT '',
  KEY `usergroup_id` (`usergroup_id`),
  KEY `o_id` (`o_id`),
  KEY `perm` (`perm`),
  KEY `o_type` (`o_type`,`usergroup_id`),
  KEY `usergroup_id_2` (`usergroup_id`,`o_type`,`o_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;




INSERT INTO `perm` VALUES (1,11,25,'11000000000000000000000000000000'),(1,17,34,'11000000000000000000000000000000'),(1,10013,35,'11000000000000000000000000000000'),(1,1,7,'11000000000000000000000000000000'),(1,3,8,'11000000111000000000000000000000'),(1,4,14,'11000000111111000000000000000000'),(1,8,21,'11000000000000000000000000000000'),(1,10010,11,'11000000000000000000000000000000'),(1,12,26,'11000000000000000000000000000000'),(1,10003,9,'11100000000000000000000000000000'),(1,10000,1,'11000000100000000000000000000000'),(2,11,25,'11000000000000000000000000000000'),(2,17,34,'11100000000000000000000000000000'),(2,10013,35,'11000000000000000000000000000000'),(2,1,7,'11000000000000000000000000000000'),(2,3,8,'11000000111000000000000000000000'),(2,4,14,'11000000111111000000000000000000'),(2,8,21,'11000000000000000000000000000000'),(2,10010,11,'11000000100000000000000000000000'),(2,12,26,'11000000000000000000000000000000'),(2,10003,9,'11100000000000000000000000000000'),(2,16,32,'11000000100000000000000000000000'),(2,10000,1,'11000000100000000000000000000000'),(2,70000,45,'11000000000000000000000000000000'),(2,30001,7,'11000001110000001111110011100000'),(2,30001,8,'11000001110000001111110011100000'),(1,70002,50,'11000000000000000000000000000000'),(2,70002,50,'11000000000000000000000000000000'),(2,36,63,'11100000111111000000000000000000'),(2,10024,65,'11000000000000000000000000000000'),(2,10023,66,'11000000000000000000000000000000'),(2,24,58,'11000000000000000000000000000000'),(1,70000,45,'11000000000000000000000000000000'),(2,14,10002,'11100000000000000000000000000000'),(2,14,10006,'11100000000000000000000000000000'),(2,14,10007,'11100000000000000000000000000000'),(2,14,10011,'11100000000000000000000000000000'),(2,14,10008,'11100000000000000000000000000000'),(2,10032,69,'11100000000000000000000000000000'),(2,10200,70,'11000000000000000000000000000000'),(2,10202,74,'11000000000000000000000000000000'),(2,108,75,'11000000000000000000000000000000'),(2,109,76,'11000000001000000000000000000000'),(2,10211,77,'11000000000000000000000000000000'),(2,130,80,'11111111000000000000000000000000'),(1,14,10002,'11000000000000000000000000000000'),(1,14,10006,'11000000000000000000000000000000'),(1,14,10007,'11000000000000000000000000000000'),(1,14,10011,'11000000000000000000000000000000'),(1,14,10008,'11000000000000000000000000000000'),(2,1042,84,'11000000000000000000000000000000'),(2,141,85,'11100000000000000000000000000000'),(1,10211,77,'11000000000000000000000000000000'),(2,10207,86,'11000000100000000000000000000000'),(1,109,76,'11000000001000000000000000000000'),(2,510,88,'11000000000000000000000000000000'),(2,110,89,'11000000000000000000000000000000'),(2,12000,90,'11000000000000000000000000000000'),(2,10300,91,'11000000100000000000000000000000'),(2,90000,92,'11000000000000000000000000000000'),(2,90100,93,'11000000000000000000000000000000'),(2,10400,94,'11000000000000000000000000000000');





CREATE TABLE `pmdiary` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `folder_id` int(11) NOT NULL DEFAULT 0,
  `body` text NOT NULL,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `csid` varchar(64) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `pmdiary_group` (
  `pmdiary_id` int(11) NOT NULL DEFAULT 0,
  `usergroup_id` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `pmdiary_notify` (
  `pmdiary_id` int(11) NOT NULL DEFAULT 0,
  `systemuser_id` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `pmfile` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `o_id` int(11) NOT NULL DEFAULT 0,
  `o_type` int(11) NOT NULL DEFAULT 0,
  `description` text NOT NULL,
  `filename` varchar(255) NOT NULL DEFAULT '',
  `filetype` varchar(128) NOT NULL DEFAULT '',
  `filesize` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `pmfolder` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mother` int(11) NOT NULL DEFAULT 0,
  `type_id` int(11) NOT NULL DEFAULT 0,
  `name` char(100) NOT NULL DEFAULT '',
  `description` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `item_order` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `mother` (`mother`),
  KEY `type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `pmtime` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `project_id` int(11) NOT NULL DEFAULT 0,
  `starttime` int(11) NOT NULL DEFAULT 0,
  `endtime` int(11) NOT NULL DEFAULT 0,
  `name` text NOT NULL,
  `description` text NOT NULL,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`),
  KEY `user_id` (`user_id`,`starttime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `pmtodo` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `folder_id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` text NOT NULL,
  `starttime` int(11) NOT NULL DEFAULT 0,
  `endtime` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `priority` int(11) NOT NULL DEFAULT 0,
  `category` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `folder_id` (`folder_id`),
  KEY `creator` (`creator`),
  KEY `editor` (`editor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `pmtodo_comment` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `todo_id` int(11) NOT NULL DEFAULT 0,
  `comment` text NOT NULL,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `pmtodo_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `todo_id` int(11) NOT NULL DEFAULT 0,
  `filename` varchar(100) NOT NULL DEFAULT '',
  `filesize` int(11) NOT NULL DEFAULT 0,
  `filetype` varchar(100) NOT NULL DEFAULT '',
  `friendlyname` varchar(100) NOT NULL DEFAULT '',
  `name` varchar(255) NOT NULL DEFAULT '',
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `pmtodo_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `todo_id` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `action` int(11) NOT NULL DEFAULT 0,
  `action_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `pmtodo_user` (
  `todo_id` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `state` int(11) NOT NULL DEFAULT 0,
  `viewed` int(11) NOT NULL DEFAULT 0,
  `last_send` int(11) NOT NULL DEFAULT 0,
  KEY `todo_id` (`todo_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `ptree` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mother_id` int(11) NOT NULL DEFAULT 0,
  `mother_type` int(11) NOT NULL DEFAULT 0,
  `link` varchar(255) NOT NULL DEFAULT '',
  `name` varchar(255) NOT NULL DEFAULT '',
  `allowlink` varchar(255) NOT NULL DEFAULT '',
  `folderopen` varchar(255) NOT NULL DEFAULT '',
  `folderclosed` varchar(255) NOT NULL DEFAULT '',
  `prepend` varchar(255) NOT NULL DEFAULT '',
  `append` varchar(255) NOT NULL DEFAULT '',
  `sqlstmt` varchar(255) NOT NULL DEFAULT '',
  `fkt` varchar(255) NOT NULL DEFAULT '',
  `bind_id` int(11) NOT NULL DEFAULT 0,
  `bind_type` int(11) NOT NULL DEFAULT 0,
  `linktarget` varchar(255) NOT NULL DEFAULT '',
  `linkext` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `publish` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `page_id` int(11) NOT NULL DEFAULT 0,
  `node_id` int(11) NOT NULL DEFAULT 0,
  `folder_id` int(11) NOT NULL DEFAULT 0,
  `path` varchar(1024) NOT NULL,
  `filename` varchar(255) NOT NULL DEFAULT '',
  `nice_url` varchar(255) DEFAULT NULL,
  `source` mediumtext DEFAULT NULL,
  `pdate` int(11) NOT NULL DEFAULT 0,
  `ddate` int(11) NOT NULL DEFAULT 0,
  `active` tinyint(4) NOT NULL DEFAULT 0,
  `updateimagestore` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `page_id` (`page_id`),
  KEY `active` (`active`),
  KEY `folder_id` (`folder_id`),
  KEY `node_id` (`node_id`,`active`),
  KEY `updateimagestore_idx` (`updateimagestore`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `publish_imagestoretarget` (
  `publish_id` int(11) NOT NULL,
  `imagestoretarget_id` int(11) NOT NULL,
  PRIMARY KEY (`publish_id`,`imagestoretarget_id`),
  KEY `imagestoretarget_id` (`imagestoretarget_id`),
  CONSTRAINT `publish_imagestoretarget_ibfk_1` FOREIGN KEY (`publish_id`) REFERENCES `publish` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `publish_imagestoretarget_ibfk_2` FOREIGN KEY (`imagestoretarget_id`) REFERENCES `imagestoretarget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `publishqueue` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `obj_type` int(11) NOT NULL DEFAULT 0,
  `obj_id` int(11) NOT NULL DEFAULT 0,
  `action` varchar(10) NOT NULL DEFAULT '',
  `channel_id` int(11) NOT NULL DEFAULT 0,
  `publish_flag` tinyint(4) NOT NULL DEFAULT 0,
  `delay` tinyint(4) NOT NULL DEFAULT 0,
  `timestamp` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `obj_type` (`obj_type`,`obj_id`,`action`,`channel_id`),
  KEY `channel_id` (`channel_id`),
  CONSTRAINT `publishqueue_ibfk_1` FOREIGN KEY (`channel_id`) REFERENCES `node` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=307 DEFAULT CHARSET=utf8mb4;









CREATE TABLE `publishqueue_attribute` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `publishqueue_id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `publishqueue_id` (`publishqueue_id`),
  CONSTRAINT `publishqueue_id_fk` FOREIGN KEY (`publishqueue_id`) REFERENCES `publishqueue` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4;









CREATE TABLE `publishworkflow` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `page_id` int(11) NOT NULL,
  `currentstep_id` int(11) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `page_id` (`page_id`),
  KEY `currentstep_id` (`currentstep_id`),
  CONSTRAINT `currentstep_id` FOREIGN KEY (`currentstep_id`) REFERENCES `publishworkflow_step` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `page_id` FOREIGN KEY (`page_id`) REFERENCES `page` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `publishworkflow_step` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `publishworkflow_id` int(11) NOT NULL,
  `sortorder` smallint(6) NOT NULL DEFAULT 1,
  `modified` tinyint(4) NOT NULL DEFAULT 0,
  `message` text NOT NULL,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `publishworkflow_id` (`publishworkflow_id`),
  KEY `creator` (`creator`),
  CONSTRAINT `publishworkflow_id` FOREIGN KEY (`publishworkflow_id`) REFERENCES `publishworkflow` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `publishworkflowstep_group` (
  `publishworkflowstep_id` int(11) NOT NULL,
  `group_id` int(11) NOT NULL,
  KEY `group_id` (`group_id`),
  KEY `publishworkflowstep_id` (`publishworkflowstep_id`),
  CONSTRAINT `group_id` FOREIGN KEY (`group_id`) REFERENCES `usergroup` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `publishworkflowstep_id` FOREIGN KEY (`publishworkflowstep_id`) REFERENCES `publishworkflow_step` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `reaction` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `keyword` varchar(255) NOT NULL DEFAULT '',
  `triggerevent_id` int(11) NOT NULL DEFAULT 0,
  `workflow_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `reactionprop` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `reaction_id` int(11) NOT NULL DEFAULT 0,
  `keyword` varchar(255) NOT NULL DEFAULT '',
  `value` text NOT NULL,
  `mapkey` varchar(255) NOT NULL DEFAULT '',
  `workflowlink_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `reactionpropeditable` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `reactionprop_id` int(11) NOT NULL DEFAULT 0,
  `description` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `regex` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name_id` int(11) NOT NULL DEFAULT 0,
  `desc_id` int(11) NOT NULL DEFAULT 0,
  `regex` text NOT NULL,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `system` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `name_id` (`name_id`),
  KEY `desc_id` (`desc_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1032 DEFAULT CHARSET=utf8mb4;




INSERT INTO `regex` VALUES (1001,265,266,'^[1-9][0-9]{0,8}$',1,979578599,1,980158482,0),(1004,271,272,'^([-_.&0-9a-zA-Z])+@[0-9a-z]([-.]?[0-9a-z])*.[a-z]{2,}$',1,979578599,1,983197608,0),(1005,273,274,'.+',1,979578599,1,981209550,0),(1008,285,286,'^.{1,255}$',1,979578599,1,980158464,0),(1009,287,288,'',1,979578599,1,980158467,0),(1010,289,290,'^[-+]{0,1}[0-9]{1,9}.{0,1}[0-9]{0,2}$',1,979578599,1,981669547,0),(1011,291,292,'^[ßäöüÄÖÜa-zA-Z .-]{1,50}$',1,979578599,1,982010014,0),(1012,293,294,'^[+-]{0,1}[0-9]{0,9}$',1,979578599,1,980158479,0),(1013,301,302,'^[a-zA-Z0-9\\._@\\-]{4,20}$',1,979578599,1,980158458,0),(1015,326,327,'^(?:http(?:s)?:)?//.+$',1,979826135,1,980158475,0),(1016,394,395,'^[a-zA-Z0-9\\._-]{1,64}$',1,980104488,1,980117531,0),(1017,434,435,'^[0-9ßäöüÄÖÜa-zA-Z \\.-]{1,255}$',1,980357188,1,980357611,0),(1018,593,594,'^[a-zA-Z0-9._-]{1,64}$',1,980768717,1,982857379,0),(1019,787,788,'^([0-9]{1,2})[.,_/ -]([0-9]{1,2})[.,_/ -]([0-9]{2}|[0-9]{4})$',1,982866575,1,982876572,0),(1020,789,790,'^([0-9]{1,2}):([0-9]{1,2})$',1,982866746,1,982870043,0),(1021,791,792,'^([0-1]?[0-9]|2[0-3])(:([0-5]?[0-9])|())$',1,982869997,1,982936595,0),(1022,794,795,'^[1-5]{0,1}[0-9]$',1,982876549,1,982876670,0),(1023,800,801,'^([0-2]?[0-9]|3[0-1])([.:_ /-]([0]?[0-9]|1[0-2])([.:_ /-]([0-9]{1,2}|[0-9]{4})|[.:_ /-]|())|[.:_ /-]|())$',1,982939213,1,989328912,0),(1024,1020,1021,'^/{0,1}([a-zA-Z0-9\\._-]{1,64}/{0,1}){0,127}$',1,984314665,1,985767431,0),(1025,1022,1023,'^[0-9a-z]([-.]?[0-9a-z])*$',1,984316002,1,984316083,0),(1026,1133,1134,'^[a-z0-9]{3,64}$',1,986907484,1,1000303606,0),(1027,1356,1357,'[A-Z]{0,2}[\\-]{0,1}[0-9]{4,6}',1,988391108,1,988391254,0),(1028,1358,1359,'^[0-9\\-\\/ \\+\\(\\)]{4,25}$',1,988391332,1,989258229,0),(1029,1360,1361,'^[0-9]{1,12}.{0,1}[0-9]{0,2}$ ',1,988467258,1,988467401,0),(1030,1394,1395,'^\\\\\\\\.*$',1,989423166,1,989829310,0),(1031,1561,1562,'^[a-zA-Z0-9\\_\\-]{3,255}$',1,999070680,1,999070728,0);





CREATE TABLE `role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name_id` int(11) DEFAULT NULL,
  `description_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `role_usergroup` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `role_id` int(11) NOT NULL,
  `usergroup_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `role_usergroup_assignment` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `role_usergroup_id` int(11) NOT NULL,
  `obj_id` int(11) DEFAULT NULL,
  `obj_type` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `role_usergroup_id` (`role_usergroup_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `roleperm` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `role_id` int(11) NOT NULL,
  `perm` char(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `roleperm_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `roleperm_obj` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `roleperm_id` int(11) NOT NULL,
  `obj_type` int(11) NOT NULL,
  `obj_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `roleperm_id` (`roleperm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `schedule2` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` text NOT NULL,
  `starttime2` int(11) NOT NULL DEFAULT 0,
  `endtime2` int(11) NOT NULL DEFAULT 0,
  `fullday` int(11) NOT NULL DEFAULT 0,
  `dorepeat` tinyint(4) NOT NULL DEFAULT 0,
  `repeatuntil` int(11) NOT NULL DEFAULT 0,
  `private` tinyint(4) NOT NULL DEFAULT 0,
  `state` int(11) NOT NULL DEFAULT 0,
  `remind` tinyint(4) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `location` varchar(255) NOT NULL DEFAULT '-',
  PRIMARY KEY (`id`),
  KEY `starttime2` (`starttime2`,`endtime2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `systemsession` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `secret` varchar(15) NOT NULL DEFAULT '',
  `user_id` int(11) NOT NULL DEFAULT 0,
  `ip` varchar(45) DEFAULT NULL,
  `agent` text DEFAULT NULL,
  `cookie` int(11) NOT NULL DEFAULT 0,
  `since` int(11) NOT NULL DEFAULT 0,
  `language` tinyint(4) NOT NULL DEFAULT 0,
  `val` mediumtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `sid` (`secret`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `systemuser` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `firstname` char(255) NOT NULL DEFAULT '',
  `lastname` char(255) NOT NULL DEFAULT '',
  `login` char(255) NOT NULL DEFAULT '',
  `password` char(64) DEFAULT NULL,
  `email` char(255) NOT NULL DEFAULT '',
  `bonus` int(11) NOT NULL DEFAULT 0,
  `active` tinyint(4) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `description` char(255) NOT NULL DEFAULT '',
  `isldapuser` int(11) NOT NULL DEFAULT 0,
  `inboxtoemail` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4;




INSERT INTO `systemuser` VALUES (1,'.Node','Gentics','system','$2a$10$luCaVcPS9gVndfg5zNFUseXU.N9s4JL3RQndBdjN434EFCEEpcji.','nowhere@gentics.com',13505,1,0,0,1,1253255021,'',0,NULL),(2,'Support',' Gentics','gentics','$2a$10$4AnFVPifC53bcMRilmnnhOApg/I0jBWqeTPNQXKcrrWd4JncTB3dG','nowhere@gentics.com',6696,1,1,1019499196,1,1253255039,'',0,NULL),(3,'Node','Admin','node','','nowhere@gentics.com',15548,1,2,1032503389,1,1563784282,'System Administrator',0,0);





CREATE TABLE `systemuser_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `systemuser_id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `json` mediumtext DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_name` (`systemuser_id`,`name`),
  CONSTRAINT `systemuser_id` FOREIGN KEY (`systemuser_id`) REFERENCES `systemuser` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `tagmap` (
  `tagname` varchar(255) NOT NULL DEFAULT '',
  `mapname` varchar(255) NOT NULL DEFAULT '',
  `searchname` varchar(255) NOT NULL DEFAULT '',
  `object` int(11) NOT NULL DEFAULT 0,
  `objtype` int(11) NOT NULL DEFAULT 0,
  `attributetype` int(11) NOT NULL DEFAULT 0,
  `multivalue` int(1) NOT NULL DEFAULT 0,
  `static` int(11) NOT NULL DEFAULT 0,
  `optimized` int(11) NOT NULL DEFAULT 0,
  `filesystem` int(11) NOT NULL DEFAULT 0,
  `foreignlinkattribute` varchar(255) NOT NULL DEFAULT '',
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `foreignlinkattributerule` varchar(255) NOT NULL DEFAULT '',
  `contentrepository_id` int(11) DEFAULT NULL,
  `category` varchar(255) NOT NULL DEFAULT '',
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  `displayfield` int(11) DEFAULT 0,
  `segmentfield` int(11) DEFAULT 0,
  `urlfield` int(11) DEFAULT 0,
  `elasticsearch` mediumtext DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `task` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tasktemplate_id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(255) DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) DEFAULT NULL,
  `edate` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `taskparam` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `task_id` int(11) NOT NULL DEFAULT 0,
  `templateparam_id` int(11) NOT NULL DEFAULT 0,
  `value` mediumtext DEFAULT NULL,
  `name` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `tasktemplate` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` text DEFAULT NULL,
  `command` text DEFAULT NULL,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) DEFAULT NULL,
  `edate` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `tasktemplateparam` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tasktemplate_id` int(11) NOT NULL DEFAULT 0,
  `paramtype` int(11) NOT NULL DEFAULT 0,
  `name` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `template` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `folder_id` int(11) NOT NULL DEFAULT 0,
  `templategroup_id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(255) NOT NULL DEFAULT '',
  `locked` int(11) NOT NULL DEFAULT 0,
  `locked_by` int(11) NOT NULL DEFAULT 0,
  `ml_id` int(11) NOT NULL DEFAULT 0,
  `ml` mediumtext NOT NULL,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `description` varchar(255) NOT NULL DEFAULT '',
  `channelset_id` int(11) NOT NULL DEFAULT 0,
  `channel_id` int(11) NOT NULL DEFAULT 0,
  `is_master` tinyint(4) NOT NULL DEFAULT 1,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `folder_id` (`folder_id`),
  KEY `channelset_id` (`channelset_id`,`is_master`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `template_folder` (
  `template_id` int(11) NOT NULL DEFAULT 0,
  `folder_id` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  UNIQUE KEY `uuid` (`uuid`),
  KEY `template_id` (`template_id`),
  KEY `folder_id` (`folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `template_node` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `template_id` int(11) NOT NULL,
  `node_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `template_id` (`template_id`,`node_id`),
  KEY `node_id` (`node_id`),
  CONSTRAINT `template_node_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `node` (`id`) ON DELETE CASCADE,
  CONSTRAINT `template_node_ibfk_2` FOREIGN KEY (`template_id`) REFERENCES `template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `template_processes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `template_id` int(11) DEFAULT NULL,
  `process_key` varchar(100) NOT NULL,
  `data` text DEFAULT NULL,
  `state` enum('NEW','GENERIC_ERROR','INVALID_JSON','INVALID_PROCESS_KEY') NOT NULL DEFAULT 'NEW',
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `template_id` (`template_id`),
  CONSTRAINT `template_processes_ibfk_1` FOREIGN KEY (`template_id`) REFERENCES `template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `templategroup` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nada` tinyint(4) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `templatetag` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `templategroup_id` int(11) NOT NULL DEFAULT 0,
  `template_id` int(11) NOT NULL DEFAULT 0,
  `construct_id` int(11) NOT NULL DEFAULT 1,
  `pub` tinyint(4) NOT NULL DEFAULT 0,
  `enabled` tinyint(4) NOT NULL DEFAULT 0,
  `name` char(255) NOT NULL DEFAULT '',
  `mandatory` tinyint(4) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `template_id` (`template_id`),
  KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `tetris` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `score` int(11) NOT NULL DEFAULT 0,
  `timestamp` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `tree` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `itemorder` int(11) NOT NULL DEFAULT 0,
  `mother` int(11) NOT NULL DEFAULT 0,
  `name` char(255) NOT NULL DEFAULT '',
  `type_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `mother` (`mother`),
  KEY `type_id` (`type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8mb4;




INSERT INTO `tree` VALUES (1,6,0,'Content.Node',10000),(7,4,0,'Administration',1),(8,1,7,'Benutzer',3),(9,3,11,'Tagtypen',10003),(11,4,7,'Content.Admin',10010),(12,3,16,'Regex',2),(14,2,7,'Gruppen',4),(15,4,16,'Tree Menu',5),(16,101,7,'Dev.Tools',7),(17,2,16,'Globale Tags',10005),(20,0,13,'Todo',30001),(21,10,7,'Log',8),(25,0,0,'Persönliche Einstellungen',11),(26,1,11,'Objekt Eigenschaften',12),(27,5,16,'Trashmodul',15),(31,0,16,'Queue',10012),(32,8,7,'Error',16),(34,1,0,'Inbox',17),(35,3,0,'Warteschlange',10013),(37,0,16,'Sanity',18),(40,0,16,'Dev.Scripts',21),(43,0,42,'Status',60006),(45,7,0,'Portal.Node',70000),(50,6,7,'Portal.Admin',70002),(58,13,7,'Workflow',24),(60,0,16,'DBAdmin',23),(63,11,7,'Scheduler',36),(65,6,11,'Datasources',10024),(66,6,11,'Page Languages',10023),(69,7,11,'Wartung',10032),(70,8,11,'Contentmap Browser',10200),(74,9,11,'Tagmap Editor',10202),(75,2,11,'Objekt Eigenschaften Kategorien',108),(76,3,7,'Rollen',109),(77,1,9,'Tagtyp Kategorien',10211),(80,7,7,'Bundle Management',130),(83,3,11,'Objekt Eigenschaften Wartung',140),(84,4,7,'Systemwartung',1042),(85,4,7,'AutoUpdate',141),(86,100,11,'ContentRepositories',10207),(87,99,16,'Java Admin',10204),(88,3,7,'Berechtigungen anzeigen',510),(89,2,0,'Task Management',110),(90,101,11,'Packages',12000),(91,102,11,'ContentRepository Fragments',10300),(92,102,7,'Tools',90000);





CREATE TABLE `triggerevent` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `keyword` varchar(255) NOT NULL DEFAULT '',
  `reaction_id` int(11) NOT NULL DEFAULT 0,
  `active` int(11) NOT NULL DEFAULT 0,
  `workflow_id` int(11) NOT NULL DEFAULT 0,
  `templateevent_id` int(11) NOT NULL DEFAULT 0,
  `workflowlink_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `type` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` char(255) NOT NULL DEFAULT '',
  `description` char(255) NOT NULL DEFAULT '',
  `auto` tinyint(4) NOT NULL DEFAULT 0,
  `javaclass` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4;




INSERT INTO `type` VALUES (1,'Text','',0,'com.gentics.contentnode.object.parttype.NormalTextPartType'),(2,'Text/HTML','',0,'com.gentics.contentnode.object.parttype.HTMLTextPartType'),(3,'HTML','',0,'com.gentics.contentnode.object.parttype.HTMLPartType'),(4,'URL (page)','',0,'com.gentics.contentnode.object.parttype.PageURLPartType'),(5,'Tag (global)','',0,''),(6,'URL (image)','',0,'com.gentics.contentnode.object.parttype.ImageURLPartType'),(8,'URL (file)','',0,'com.gentics.contentnode.object.parttype.FileURLPartType'),(9,'Text (short)','',0,'com.gentics.contentnode.object.parttype.ShortTextPartType'),(10,'Text/HTML (long)','',0,'com.gentics.contentnode.object.parttype.LongHTMLTextPartType'),(11,'Tag (page)','',0,'com.gentics.contentnode.object.parttype.PageTagPartType'),(13,'Overview','',0,'com.gentics.contentnode.object.parttype.OverviewPartType'),(15,'List','',0,'com.gentics.contentnode.object.parttype.ChangeableListPartType'),(16,'List (unordered)','',0,'com.gentics.contentnode.object.parttype.UnorderedListPartType'),(17,'List (ordered)','',0,'com.gentics.contentnode.object.parttype.OrderedListPartType'),(18,'Select (image-height)','',0,'com.gentics.contentnode.object.parttype.ImageHeightPartType'),(19,'Select (image-width)','',0,'com.gentics.contentnode.object.parttype.ImageWidthPartType'),(20,'Tag (template)','',0,'com.gentics.contentnode.object.parttype.TemplateTagPartType'),(21,'HTML (long)','',0,'com.gentics.contentnode.object.parttype.LongHTMLPartType'),(22,'File (localpath)','',0,'com.gentics.contentnode.object.parttype.FilePartType'),(23,'Table ext','',0,'com.gentics.contentnode.object.parttype.TablePartType'),(24,'Select (class)','',0,'com.gentics.contentnode.object.parttype.SelectClassPartType'),(25,'URL (folder)','',0,'com.gentics.contentnode.object.parttype.FolderURLPartType'),(26,'Java Editor','',0,'com.gentics.contentnode.object.parttype.JavaEditorPartType'),(27,'DHTML Editor','',0,'com.gentics.contentnode.object.parttype.DHTMLPartType'),(29,'Select (single)','',0,'com.gentics.contentnode.object.parttype.SingleSelectPartType'),(30,'Select (multiple)','',0,'com.gentics.contentnode.object.parttype.MultiSelectPartType'),(31,'Checkbox','',0,'com.gentics.contentnode.object.parttype.CheckboxPartType'),(32,'Datasource','',0,'com.gentics.contentnode.object.parttype.DatasourcePartType'),(33,'Velocity','',0,'com.gentics.contentnode.object.parttype.VelocityPartType'),(34,'Breadcrumb','',0,'com.gentics.contentnode.object.parttype.BreadcrumbPartType'),(35,'Navigation','',0,'com.gentics.contentnode.object.parttype.NavigationPartType'),(36,'HTML (custom form)','',0,'com.gentics.contentnode.object.parttype.HTMLPartType'),(37,'Text (custom form)','',0,'com.gentics.contentnode.object.parttype.NormalTextPartType'),(38,'File (Upload)','',0,'com.gentics.contentnode.object.parttype.FileURLPartType'),(39,'Folder (Upload)','',0,'com.gentics.contentnode.object.parttype.FolderURLPartType'),(40,'Node','Select a single node',0,'com.gentics.contentnode.object.parttype.NodePartType');





CREATE TABLE `user_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `usergroup_id` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `usergroup_id` (`usergroup_id`),
  KEY `usergroup_id_2` (`usergroup_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4;




INSERT INTO `user_group` VALUES (1,1,1,0,0),(2,3,2,1193148296,1),(13,2,2,0,0);





CREATE TABLE `user_group_node` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_group_id` int(11) NOT NULL DEFAULT 0,
  `node_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `user_group_id` (`user_group_id`),
  KEY `node_id` (`node_id`),
  CONSTRAINT `user_group_node_ibfk_1` FOREIGN KEY (`user_group_id`) REFERENCES `user_group` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `user_group_node_ibfk_2` FOREIGN KEY (`node_id`) REFERENCES `node` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `user_schedule2` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date_id` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `group_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `date_id` (`date_id`),
  KEY `user_id` (`user_id`),
  KEY `group_id` (`group_id`),
  KEY `date_id_2` (`date_id`,`group_id`),
  KEY `date_id_3` (`date_id`,`user_id`,`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `usergroup` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` char(255) NOT NULL DEFAULT '',
  `mother` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `description` char(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `mother` (`mother`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;




INSERT INTO `usergroup` VALUES (1,'system',0,0,0,0,0,''),(2,'Node Super Admin',1,1,1019499174,3,1159876978,'The one and only System Super Administrator.');





CREATE TABLE `value` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `part_id` int(11) NOT NULL DEFAULT 0,
  `info` int(11) NOT NULL DEFAULT 0,
  `static` tinyint(4) NOT NULL DEFAULT 0,
  `templatetag_id` int(11) NOT NULL DEFAULT 0,
  `contenttag_id` int(11) NOT NULL DEFAULT 0,
  `globaltag_id` int(11) NOT NULL DEFAULT 0,
  `objtag_id` int(11) NOT NULL DEFAULT 0,
  `value_text` mediumtext NOT NULL,
  `value_ref` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL DEFAULT '',
  `udate` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `part_id` (`part_id`),
  KEY `info` (`info`),
  KEY `templatetag_id` (`templatetag_id`),
  KEY `contenttag_id` (`contenttag_id`),
  KEY `globaltag_id` (`globaltag_id`),
  KEY `objtag_id` (`objtag_id`),
  KEY `value_ref` (`value_ref`),
  KEY `static` (`static`),
  KEY `part_id_2` (`part_id`,`templatetag_id`,`contenttag_id`,`objtag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `value_nodeversion` (
  `auto_id` int(11) NOT NULL AUTO_INCREMENT,
  `id` int(11) NOT NULL DEFAULT 0,
  `part_id` int(11) NOT NULL DEFAULT 0,
  `info` int(11) NOT NULL DEFAULT 0,
  `static` tinyint(4) NOT NULL DEFAULT 0,
  `templatetag_id` int(11) NOT NULL DEFAULT 0,
  `contenttag_id` int(11) NOT NULL DEFAULT 0,
  `globaltag_id` int(11) NOT NULL DEFAULT 0,
  `objtag_id` int(11) NOT NULL DEFAULT 0,
  `value_text` mediumtext NOT NULL,
  `value_ref` int(11) NOT NULL DEFAULT 0,
  `nodeversiontimestamp` int(11) NOT NULL DEFAULT 0,
  `nodeversion_user` int(11) NOT NULL DEFAULT 0,
  `nodeversionlatest` int(11) NOT NULL DEFAULT 0,
  `nodeversionremoved` int(11) NOT NULL DEFAULT 0,
  `uuid` varchar(41) NOT NULL,
  PRIMARY KEY (`auto_id`),
  KEY `id` (`id`),
  KEY `contenttag_id` (`contenttag_id`),
  KEY `part_id` (`part_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `workflow` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` text NOT NULL,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  `o_type` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;









CREATE TABLE `workflowlink` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `workflow_id` int(11) NOT NULL DEFAULT 0,
  `o_type` int(11) NOT NULL DEFAULT 0,
  `o_id` int(11) NOT NULL DEFAULT 0,
  `creator` int(11) NOT NULL DEFAULT 0,
  `cdate` int(11) NOT NULL DEFAULT 0,
  `editor` int(11) NOT NULL DEFAULT 0,
  `edate` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
















