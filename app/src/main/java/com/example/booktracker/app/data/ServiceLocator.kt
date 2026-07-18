package com.example.booktracker.app.data

import android.content.Context
import androidx.room.Room
import com.example.booktracker.app.data.local.AppDatabase

/**
 * Minimal manual DI. Both the UI (via ViewModel) and WearSyncService — which
 * runs without the Activity — resolve the repository through here, so they
 * share one database instance.
 */
object ServiceLocator {
    @Volatile
    private var database: AppDatabase? = null

    fun repository(context: Context): BookRepository =
        db(context).let { RoomBookRepository(it.bookDao(), it.sessionDao(), it.marginNoteDao()) }

    fun settings(context: Context): SettingsRepository =
        SettingsRepository(context.applicationContext)

    private fun db(context: Context): AppDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "booktracker.db"
            )
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .build().also { database = it }
        }
}
