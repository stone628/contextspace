# Table schema policy

Every table follows the same pattern — a JSONB document per row storing all non-indexed fields.

| Column | Purpose |
|---|---|
| `id` | `BIGINT PK`, auto-increment |
| `<index_column>` | One or more indexed lookup columns (`VARCHAR`, unique if appropriate) |
| `content` | `JSONB` — all remaining fields bundled in a `{TableName}Content` data class |
| `created_at` | `DATETIME`, default `now()` |

Three classes are tied together for each table:

| Class | File | Role |
|---|---|---|
| `{TableName}Content` | `tables/{TableName}.kt` | `@Serializable` — exact shape of the JSONB document |
| `{TableName}` (table) | `tables/{TableName}.kt` | Exposed table with `id`, index columns, `jsonb<...>("content", ...)`, `createdAt` |
| `{TableName}Row` | `tables/{TableName}.kt` | In-memory row — **must mirror every field in `{TableName}Content`** as top-level fields, plus `createdAt: java.time.LocalDateTime` |

**When a field is added, removed, or renamed in `{TableName}Content`, the same change must be applied to `{TableName}Row`.** The DAO's `toRow` and `toContent` mapping functions are the bridge — `toRow` destructures the content class into the row's expanded fields, `toContent` folds the row back into the content class. Keep the two in sync.

Steps to add a new table (e.g. `widgets`):

1. Create `tables/Widgets.kt` — `object Widgets : Table("widgets")` with `id`, index columns, `jsonb<WidgetsContent>("content", Json.Default)`, and `createdAt`. Add `WidgetsRow` with the same fields as `WidgetsContent` expanded at the top level. Add `WidgetsContent` — a `@Serializable` data class with all non-indexed fields (use `Long?` for timestamps, not `LocalDateTime`).
2. In the `init` block, call `registerTable(this)` — auto-registers for schema creation.
3. Create `dao/WidgetDao.kt` — read/write `it[Widgets.content]` as the whole document, map via `toRow`/`toContent`.

Registration is automatic: `CollectPreloadClassesTask` scans for files importing `registerTable`, generates `PreloadClasses.kt` with `preloadClasses()` that forces object init. `Application.common()` iterates `registered().tables()` and calls `SchemaUtils.create` in batch. No manual wiring in `Application.kt`.
