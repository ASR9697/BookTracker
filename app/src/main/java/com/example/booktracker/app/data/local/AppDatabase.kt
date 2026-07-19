package com.example.booktracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookEntity::class,
        SessionEntity::class,
        MarginNoteEntity::class,
        BookFtsEntity::class,
        NoteFtsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun sessionDao(): SessionDao
    abstract fun marginNoteDao(): MarginNoteDao

    companion object {
        // v2: book description/genres/publishedDate + margin notes table.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE books ADD COLUMN genres TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE books ADD COLUMN publishedDate TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS margin_notes (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "bookId TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "pageOrUnit INTEGER NOT NULL, " +
                        "markdownContent TEXT NOT NULL, " +
                        "isVoiceDictated INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_margin_notes_bookId ON margin_notes(bookId)"
                )
            }
        }

        // v3: external-content FTS tables for universal search. The DDL must match
        // Room's generated createAllTables exactly or schema validation aborts on
        // open; 'rebuild' backfills the index from rows that predate the tables.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `books_fts` USING FTS4(" +
                        "`title` TEXT NOT NULL, `authors` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, `genres` TEXT NOT NULL, " +
                        "content=`books`)"
                )
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(" +
                        "`markdownContent` TEXT NOT NULL, content=`margin_notes`)"
                )
                db.execSQL("INSERT INTO books_fts(books_fts) VALUES('rebuild')")
                db.execSQL("INSERT INTO `notes_fts` (`notes_fts`) VALUES ('rebuild')")
            }
        }

        // v4: Added isFavorite boolean to books
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                // Trigger a rebuild of books_fts just in case, though the schema of books_fts didn't change
                db.execSQL("INSERT INTO `books_fts` (`books_fts`) VALUES ('rebuild')")
            }
        }
    }
}
