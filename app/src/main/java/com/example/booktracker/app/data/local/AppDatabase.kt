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
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun sessionDao(): SessionDao
    abstract fun marginNoteDao(): MarginNoteDao

    companion object {
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
                        "page INTEGER NOT NULL, " +
                        "markdownContent TEXT NOT NULL, " +
                        "isVoiceDictated INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_margin_notes_bookId ON margin_notes(bookId)"
                )
            }
        }

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("INSERT INTO `books_fts` (`books_fts`) VALUES ('rebuild')")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migrate books
                db.execSQL("CREATE TABLE IF NOT EXISTS `books_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `isbn` TEXT, `coverUrl` TEXT NOT NULL, `totalPages` INTEGER NOT NULL, `currentPage` INTEGER NOT NULL, `language` TEXT NOT NULL, `creators` TEXT NOT NULL, `publication` TEXT, `format` TEXT NOT NULL, `progressUnit` TEXT NOT NULL, `description` TEXT NOT NULL, `seriesInfo` TEXT, `classification` TEXT NOT NULL, `status` TEXT NOT NULL, `rating` TEXT NOT NULL, `dnfData` TEXT, `purchaseLog` TEXT NOT NULL, `loanRecord` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO books_new (id, title, isbn, coverUrl, totalPages, currentPage, language, creators, publication, format, progressUnit, description, seriesInfo, classification, status, rating, dnfData, purchaseLog, loanRecord, dateAdded, lastUpdated, isFavorite) SELECT id, title, isbn, coverUrl, totalPages, currentPage, 'en', '[]', null, format, 'Page', description, null, '{\"collections\":[],\"tags\":[]}', status, '{}', null, '[]', '[]', dateAdded, lastUpdated, isFavorite FROM books")
                db.execSQL("DROP TABLE books")
                db.execSQL("ALTER TABLE books_new RENAME TO books")

                // Migrate sessions
                db.execSQL("CREATE TABLE IF NOT EXISTS `sessions_new` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, `startPage` INTEGER NOT NULL, `endPage` INTEGER NOT NULL, `pagesRead` INTEGER NOT NULL, `environmentTag` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO sessions_new (id, bookId, startTime, endTime, durationSeconds, startPage, endPage, pagesRead, environmentTag) SELECT id, bookId, startTime, endTime, 0, startPage, endPage, pagesRead, '' FROM sessions")
                db.execSQL("DROP TABLE sessions")
                db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_bookId ON sessions(bookId)")

                // Migrate margin_notes
                db.execSQL("CREATE TABLE IF NOT EXISTS `margin_notes_new` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `pageNumber` INTEGER NOT NULL, `content` TEXT NOT NULL, `type` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO margin_notes_new (id, bookId, pageNumber, content, type, isFavorite, timestamp) SELECT id, bookId, page, markdownContent, 'BOOK_CONTENT', 0, timestamp FROM margin_notes")
                db.execSQL("DROP TABLE margin_notes")
                db.execSQL("ALTER TABLE margin_notes_new RENAME TO margin_notes")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_margin_notes_bookId ON margin_notes(bookId)")

                // Migrate FTS tables
                db.execSQL("DROP TABLE IF EXISTS books_fts")
                db.execSQL("DROP TABLE IF EXISTS notes_fts")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `books_fts` USING FTS4(`title` TEXT NOT NULL, `creators` TEXT NOT NULL, `description` TEXT NOT NULL, `classification` TEXT NOT NULL, content=`books`)")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(`content` TEXT NOT NULL, content=`margin_notes`)")
                db.execSQL("INSERT INTO `books_fts`(`books_fts`) VALUES('rebuild')")
                db.execSQL("INSERT INTO `notes_fts`(`notes_fts`) VALUES('rebuild')")
            }
        }

        /**
         * Data-only repair: finalizeSession never wrote durationSeconds, so every
         * timer-recorded session persisted a 0 and was skipped by consumers that
         * filter on durationSeconds > 0. Backfill from the recorded wall-clock
         * span, leaving instant progress deltas (startTime == endTime) at 0.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE sessions SET durationSeconds = (endTime - startTime) / 1000 " +
                        "WHERE durationSeconds = 0 AND endTime > startTime"
                )
            }
        }

        /** Tracks re-reads so the detail header can say which pass this is. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN readCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("INSERT INTO `books_fts`(`books_fts`) VALUES('rebuild')")
            }
        }

        /** Adds plannedDate and review fields for Phase 12 Planner and Reviews. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN plannedDate INTEGER")
                db.execSQL("ALTER TABLE books ADD COLUMN review TEXT")
                db.execSQL("INSERT INTO `books_fts`(`books_fts`) VALUES('rebuild')")
            }
        }

        /**
         * Stores the finish date explicitly. Until now stats read it from lastUpdated,
         * which any later edit (favourite, loan, collection) silently moved. Existing
         * finished/abandoned books keep their best-known date: the last update.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN finishedAt INTEGER")
                db.execSQL(
                    "UPDATE books SET finishedAt = lastUpdated " +
                        "WHERE status IN ('FINISHED', 'DNF', 'ABANDONED') AND lastUpdated > 0"
                )
            }
        }

        /**
         * Adds ownership (owned, gift, wishlist). Books that already log a purchase
         * were bought, so they start as Owned; everything else stays unset.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN ownership TEXT")
                db.execSQL("UPDATE books SET ownership = 'OWNED' WHERE purchaseLog NOT IN ('', '[]')")
            }
        }

        /** Adds photo attachments to notes; existing notes simply have none. */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE margin_notes ADD COLUMN images TEXT")
            }
        }

        /**
         * Records each session's time zone so a streak survives travel. Existing
         * sessions stay null and keep using the phone's zone, as before.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN zoneId TEXT")
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12
        )
    }
}
