insert into contentobject (name, type, id_counter, exclude_versioning) values ('optimizedtest', 101, 1, 1);

insert into contentattributetype (name, attributetype, optimized, quickname, objecttype) values ('mystring', 1, 1, 'quick_mystring', 101);
insert into contentattributetype (name, attributetype, optimized, quickname, objecttype) values ('myint', 3, 1, 'quick_myint', 101);
insert into contentattributetype (name, attributetype, optimized, quickname, objecttype) values ('mylongtext', 5, 1, 'quick_mylongtext', 101);
insert into contentattributetype (name, attributetype, optimized, quickname, objecttype) values ('mybinary', 6, 1, 'quick_mybinary', 101);
insert into contentattributetype (name, attributetype, optimized, quickname, objecttype) values ('mylongint', 8, 1, 'quick_mylongint', 101);
insert into contentattributetype (name, attributetype, optimized, quickname, objecttype) values ('mydouble', 9, 1, 'quick_mydouble', 101);
insert into contentattributetype (name, attributetype, optimized, quickname, objecttype) values ('mydate', 10, 1, 'quick_mydate', 101);
insert into contentattributetype (name, attributetype, optimized, quickname, objecttype, linkedobjecttype) values ('mylink', 2, 1, 'quick_mylink', 101, 101);


alter table contentmap add quick_mystring varchar(255);
alter table contentmap add quick_myint int(11);
alter table contentmap add quick_mylongtext mediumtext;
alter table contentmap add quick_mybinary longblob;
alter table contentmap add quick_mylongint bigint(20);
alter table contentmap add quick_mydouble double;
alter table contentmap add quick_mydate datetime;
alter table contentmap add quick_mylink varchar(255);


alter table contentmap add index (quick_mystring);
alter table contentmap add index (quick_myint);
alter table contentmap add index (quick_mylongtext(255));
alter table contentmap add index (quick_mybinary(255));
alter table contentmap add index (quick_mylongint);
alter table contentmap add index (quick_mydouble);
alter table contentmap add index (quick_mydate);
alter table contentmap add index (quick_mylink);