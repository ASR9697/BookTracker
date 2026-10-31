package com.example.booktracker.app.data.local

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.booktracker.shared.models.Ownership
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A reader's library survives every database upgrade. Opening through Room
 * runs the migrations and then checks the tables against the entities, so a
 * migration that leaves a column, index or default different from what the
 * entities declare fails here instead of on the reader's phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test.db"

    @After
    fun delete() {
        context.deleteDatabase(name)
    }

    private fun openWithMigrations(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

    /** Writes a database at [version] from raw SQL, the way an older app left it. */
    private fun createAt(version: Int, statements: List<String>) {
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            statements.forEach(db::execSQL)
            db.version = version
        }
    }

    private inline fun <R> AppDatabase.use(block: (AppDatabase) -> R): R =
        try { block(this) } finally { close() }

    private fun currentVersion(): Int =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build().use { it.openHelper.readableDatabase.version }

    @Test
    fun `the migrations step one version at a time up to the current one`() {
        val steps = AppDatabase.ALL_MIGRATIONS.map { it.startVersion to it.endVersion }
        assertEquals((1 until currentVersion()).map { it to it + 1 }, steps)
    }

    @Test
    fun `a version 5 library upgrades with its books, sessions and notes intact`() = runBlocking {
        createAt(5, VERSION_5_TABLES + VERSION_5_LIBRARY)
        openWithMigrations().use { db ->
            val books = db.bookDao().getAll().associate { it.id to it.toModel() }

            val finished = books.getValue("finished")
            assertEquals("Dune", finished.title)
            assertEquals(1_000L, finished.finishedAt) // 8 -> 9: the old finish date was lastUpdated
            assertEquals(Ownership.OWNED, finished.ownership) // 9 -> 10: it logs a purchase
            assertEquals(0, finished.readCount)
            assertNull(finished.wish)

            val toRead = books.getValue("to-read")
            assertNull(toRead.finishedAt)
            assertEquals(Ownership.NONE, toRead.ownership)

            val session = db.sessionDao().getAll().single().toModel()
            assertEquals(600, session.durationSeconds) // 5 -> 6: backfilled from the clock span
            assertEquals("", session.zoneId)
            assertEquals(0, session.focus)

            val note = db.marginNoteDao().getAll().single().toModel()
            assertEquals("Fear is the mind-killer.", note.content)
            assertTrue(note.imageUris.isEmpty())

            // Search covers the old rows, and keeps up with books saved after the upgrade.
            assertEquals(listOf("finished"), db.bookDao().search("Dune").map { it.id })
            assertEquals(listOf("note"), db.marginNoteDao().search("mind").map { it.id })
            db.bookDao().upsert(books.getValue("to-read").copy(id = "new", title = "Hyperion").toEntity())
            assertEquals(listOf("new"), db.bookDao().search("Hyperion").map { it.id })
        }
    }

    /**
     * Every schema committed in app/schemas upgrades to the current one. The
     * current version's file has to be there too, so the next migration can be
     * tested from exactly these tables.
     */
    @Test
    fun `every exported schema upgrades to the current one`() {
        val current = currentVersion()
        val schemas = SCHEMA_DIR.listFiles { file -> file.extension == "json" }.orEmpty()
            .associateBy { it.nameWithoutExtension.toInt() }
        assertTrue("app/schemas has no $current.json: build once and commit it", current in schemas)

        schemas.filterKeys { it < current }.toSortedMap().forEach { (version, file) ->
            createAt(version, createStatements(file))
            openWithMigrations().use { db -> assertEquals(current, db.openHelper.readableDatabase.version) }
            context.deleteDatabase(name)
        }
    }

    private companion object {
        val SCHEMA_DIR = File("schemas/${AppDatabase::class.java.name}")

        /** The tables, indices, search triggers and identity row Room creates for an exported schema. */
        fun createStatements(file: File): List<String> {
            val database = JsonParser.parseString(file.readText()).asJsonObject.getAsJsonObject("database")
            fun JsonObject.strings(key: String) = getAsJsonArray(key)?.map { it.asString }.orEmpty()
            val tables = database.getAsJsonArray("entities").map { it.asJsonObject }.flatMap { entity ->
                val table = entity.get("tableName").asString
                val sql = listOf(entity.get("createSql").asString) +
                    entity.getAsJsonArray("indices")?.map { it.asJsonObject.get("createSql").asString }.orEmpty() +
                    entity.strings("contentSyncTriggers")
                sql.map { it.replace("\${TABLE_NAME}", table) }
            }
            return tables + database.strings("setupQueries")
        }

        /** The tables as MIGRATION_4_5 left them; versions 5 and later only add to these. */
        val VERSION_5_TABLES = listOf(
            "CREATE TABLE `books` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `isbn` TEXT, `coverUrl` TEXT NOT NULL, `totalPages` INTEGER NOT NULL, `currentPage` INTEGER NOT NULL, `language` TEXT NOT NULL, `creators` TEXT NOT NULL, `publication` TEXT, `format` TEXT NOT NULL, `progressUnit` TEXT NOT NULL, `description` TEXT NOT NULL, `seriesInfo` TEXT, `classification` TEXT NOT NULL, `status` TEXT NOT NULL, `rating` TEXT NOT NULL, `dnfData` TEXT, `purchaseLog` TEXT NOT NULL, `loanRecord` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE `sessions` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, `startPage` INTEGER NOT NULL, `endPage` INTEGER NOT NULL, `pagesRead` INTEGER NOT NULL, `environmentTag` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS index_sessions_bookId ON sessions(bookId)",
            "CREATE TABLE `margin_notes` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `pageNumber` INTEGER NOT NULL, `content` TEXT NOT NULL, `type` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS index_margin_notes_bookId ON margin_notes(bookId)",
            "CREATE VIRTUAL TABLE `books_fts` USING FTS4(`title` TEXT NOT NULL, `creators` TEXT NOT NULL, `description` TEXT NOT NULL, `classification` TEXT NOT NULL, content=`books`)",
            "CREATE VIRTUAL TABLE `notes_fts` USING FTS4(`content` TEXT NOT NULL, content=`margin_notes`)"
        )

        val VERSION_5_LIBRARY = listOf(
            "INSERT INTO books VALUES ('finished', 'Dune', NULL, '', 412, 412, 'en', '[{\"role\":\"Author\",\"name\":\"Frank Herbert\"}]', NULL, 'Paperback', 'Page', 'Arrakis.', NULL, '{\"collections\":[],\"tags\":[]}', 'FINISHED', '{}', NULL, '[{\"date\":1,\"vendor\":\"Shop\",\"price\":9.5,\"currency\":\"EUR\",\"memo\":\"\"}]', '[]', 500, 1000, 0)",
            "INSERT INTO books VALUES ('to-read', 'Solaris', NULL, '', 204, 0, 'en', '[]', NULL, 'Paperback', 'Page', '', NULL, '{\"collections\":[],\"tags\":[]}', 'TO_READ', '{}', NULL, '[]', '[]', 600, 700, 0)",
            "INSERT INTO sessions VALUES ('session', 'finished', 100000, 700000, 0, 1, 20, 19, '')",
            "INSERT INTO margin_notes VALUES ('note', 'finished', 8, 'Fear is the mind-killer.', 'BOOK_CONTENT', 0, 800)",
            "INSERT INTO books_fts(books_fts) VALUES('rebuild')",
            "INSERT INTO notes_fts(notes_fts) VALUES('rebuild')"
        )
    }
}
