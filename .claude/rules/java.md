---
paths:
  - "**/*.java"
  - "cms-core/src/main/resources/contentnode_*.properties"
---

# Java code conventions

- Indentation with **tabs**.
- Javadoc on public classes, fields and methods.
- Database access runs in transactions: `try (Trx trx = new Trx()) { … }` or the helpers
  `Trx.supply(…)`, `Trx.operate(…)`, `Trx.consume(…)`, `Trx.execute(…)`; channel scope via
  `ChannelTrx`, wastebin handling via `WastebinFilter`. Reuse these instead of manual transaction
  handling.
- User-facing backend messages are i18n keys in
  `cms-core/src/main/resources/contentnode_de_DE.properties` and `contentnode_en_EN.properties` – add
  every new key to both, translated to German and English, respectively.
