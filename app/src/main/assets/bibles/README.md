# Bundled Bible files

Everything in this folder is packaged inside the APK and imported into Room on
first launch. No network access is involved at any point.

## What is here now

The three `.json` files are **small samples** — enough verses to prove the whole
pipeline works on a real phone (single verse, range queue, next-verse advance),
but not a usable Bible. Replace them before you preach from this.

| File | Code | Status |
| --- | --- | --- |
| `kjv.json` | `KJV` | sample — ~27 verses |
| `tagalog_adb.json` | `ADB` | sample — verify the text |
| `cebuano_bugna.json` | `BUGNA` | sample — verify the text |

> The Tagalog and Cebuano sample verses were typed from memory, not copied from
> a verified source. Check them against a printed or trusted digital edition
> before trusting them in a service. The same applies to the translated book
> names in `data/model/BibleBook.kt`.

## File format

```json
{
  "code": "KJV",
  "name": "King James Version",
  "verses": [
    { "b": 1, "c": 1, "v": 1, "t": "In the beginning God created..." }
  ]
}
```

| Key | Meaning | Also accepted |
| --- | --- | --- |
| `b` | book, 1–66 **or** a book name | `book`, `book_number`, `book_name` |
| `c` | chapter | `chapter` |
| `v` | verse | `verse` |
| `t` | verse text | `text`, `verse_text`, `content` |

The importer also accepts:

- the **root being a bare array** of verse objects (no wrapper object);
- the array under `verses`, `data` or `rows`;
- numbers written as JSON strings (`"c": "1"`);
- book names in English, Tagalog, Cebuano or OSIS form (`Genesis`, `Juan`,
  `1Cor`) — resolved through `BibleBooks.resolve()`.

Rows that cannot be resolved are skipped and counted, not fatal. The count is
written to logcat under the `BibleSeedManager` tag after each import.

## Swapping in the full text

1. Get a public-domain JSON edition. KJV, Ang Dating Biblia 1905 and the Cebuano
   Bugna are all public domain; several community repositories publish them as
   flat JSON in exactly this shape.
2. If the field names differ, either rename them to `b/c/v/t` or add the names
   you have to the key sets at the bottom of `BibleJsonImporter.kt`.
3. **Gzip it** — a full Bible is roughly 4–5 MB of JSON and compresses to under
   1.5 MB:
   ```bash
   gzip -9 -k kjv.json          # produces kjv.json.gz
   ```
4. Point `manifest.json` at the new file and set a realistic `expectedVerses`:
   ```json
   { "code": "KJV", "file": "kjv.json.gz", "expectedVerses": 31102, ... }
   ```
   The importer detects `.gz` from the filename — nothing else changes.
5. Uninstall the app (or clear its data) before reinstalling, so Room recreates
   the database and re-imports. The seeder also recovers on its own if a version
   row is incomplete.

## Adding a fourth bundled translation

Drop the file here, add one entry to `manifest.json`. No code change.

## Alternative: ship a pre-populated database

If first-launch import time ever becomes a problem on older phones, the faster
route is to build the SQLite file once on a desktop and ship that instead, using
`Room.databaseBuilder(...).createFromAsset("bible_prompter.db")`. Import time
then drops to zero because there is nothing to parse. The trade-off is that the
schema in the prebuilt file has to match the Room schema exactly, which makes
later changes more awkward — which is why the JSON route is the default here.
