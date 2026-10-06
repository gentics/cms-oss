# Build

- Full build: `mvn clean package` at the root, with `-Dskip.integration.tests`.
- Avoid skipping the CMS UI parts of the build where possible.
- For development, build without running tests and run the distinct JUnit tests of the task
  separately (see `testing.md`):
  ```
  mvn -Dskip.integration.tests -DskipTests clean package
  ```
