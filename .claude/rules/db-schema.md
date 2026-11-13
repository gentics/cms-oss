---
paths:
  - "cms-core/src/main/resources/changelog/**"
  - "cms-core/src/main/java/com/gentics/contentnode/changelog/**"
---

# Database schema changes

- Schema changes and data migrations are appended to
  `cms-core/src/main/resources/changelog/{content,node,system}.CHANGELOG`, one line per statement,
  format: `<YYYY-MM-DD> CH-<number> <author initials> SQL <statement>;` (optional `## comment`),
  fields separated by whitespace (tabs in `content.CHANGELOG`, spaces in `system.CHANGELOG`).
  Example: `2026-07-08  CH-1748 NOP SQL CREATE TABLE api_token (…);` They are applied by
  `com.gentics.contentnode.changelog.ChangeLogHandler`.
- `CH-` numbers are shared between `content.CHANGELOG` and `system.CHANGELOG`; `node.CHANGELOG`
  contains no `CH-` entries. The highest number in use:
  ```
  grep -ohE 'CH-[0-9]+' cms-core/src/main/resources/changelog/{content,system}.CHANGELOG | sort -t- -k2 -n | tail -1
  ```
  The next free number is obtained from a 3rd party service and has to be requested from the user.
