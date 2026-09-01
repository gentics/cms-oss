--
-- Account used by the Gentics CMS test suites on the local test database container.
-- See build/testdb/README.md.
--
-- The gcn-testdb-manager hands this account out to the tests and runs
--     GRANT ALL PRIVILEGES ON *.* to node@'%' IDENTIFIED BY ''
-- for every database it prepares. Since MariaDB 10.4 that statement does not create the
-- account implicitly any more, so it is created here. The empty password is required:
-- the manager always reports an empty password back to the tests
-- (com.gentics.testutils.testdbmanager.ManagerResponse#toProperties).
--
CREATE USER IF NOT EXISTS 'node'@'%';
GRANT ALL PRIVILEGES ON *.* TO 'node'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;
